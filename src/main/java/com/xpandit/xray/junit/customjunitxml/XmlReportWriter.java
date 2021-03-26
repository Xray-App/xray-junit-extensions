/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.xpandit.xray.junit.customjunitxml;

import com.xpandit.xray.junit.customjunitxml.XmlReportWriter.AggregatedTestResult.Type;
import com.xpandit.xray.junit.customjunitxml.annotations.Requirement;
import com.xpandit.xray.junit.customjunitxml.annotations.XrayTest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import static java.text.MessageFormat.format;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.Comparator.naturalOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.AnnotationSupport;
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import org.junit.platform.commons.util.ReflectionUtils;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;
import static com.xpandit.xray.junit.customjunitxml.XmlReportWriter.AggregatedTestResult.Type.ERROR;
import static com.xpandit.xray.junit.customjunitxml.XmlReportWriter.AggregatedTestResult.Type.FAILURE;
import static com.xpandit.xray.junit.customjunitxml.XmlReportWriter.AggregatedTestResult.Type.SKIPPED;
import static com.xpandit.xray.junit.customjunitxml.XmlReportWriter.AggregatedTestResult.Type.SUCCESS;

import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;
import org.junit.platform.engine.TestTag;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * {@code XmlReportWriter} writes an XML report whose format is compatible with
 * the de facto standard for JUnit 4 based test reports that was made popular by
 * the Ant build system.
 *
 * @since 1.4
 */
class XmlReportWriter {

	// Using zero-width assertions in the split pattern simplifies the splitting
	// process: All split parts
	// (including the first and last one) can be used directly, without having to
	// re-add separator characters.
	private static final Pattern CDATA_SPLIT_PATTERN = Pattern.compile("(?<=]])(?=>)");

	private final XmlReportData reportData;
	private static final Logger logger = LoggerFactory.getLogger(EnhancedLegacyXmlReportGeneratingListener.class);

	XmlReportWriter(XmlReportData reportData) {
		this.reportData = reportData;
	}

	void writeXmlReport(TestIdentifier rootDescriptor, Writer out) throws XMLStreamException {
		TestPlan testPlan = this.reportData.getTestPlan();
		Map<TestIdentifier, AggregatedTestResult> tests = testPlan.getDescendants(rootDescriptor) //
				.stream() //
				.filter(testIdentifier -> shouldInclude(testPlan, testIdentifier)) //
				.collect(toMap(identity(), this::toAggregatedResult)); //
		writeXmlReport(rootDescriptor, tests, out);
	}

	private AggregatedTestResult toAggregatedResult(TestIdentifier testIdentifier) {
		if (this.reportData.wasSkipped(testIdentifier)) {
			return AggregatedTestResult.skipped();
		}
		return AggregatedTestResult.nonSkipped(this.reportData.getResults(testIdentifier));
	}

	private boolean shouldInclude(TestPlan testPlan, TestIdentifier testIdentifier) {
		return testIdentifier.isTest() || testPlan.getChildren(testIdentifier).isEmpty();
	}

	private void writeXmlReport(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> tests,
			Writer out) throws XMLStreamException {

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(out);
		xmlWriter.writeStartDocument("UTF-8", "1.0");
		newLine(xmlWriter);
		writeTestsuite(testIdentifier, tests, xmlWriter);
		xmlWriter.writeEndDocument();
		xmlWriter.flush();
		xmlWriter.close();
	}

	private void writeTestsuite(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> tests,
			XMLStreamWriter writer) throws XMLStreamException {

		// NumberFormat is not thread-safe. Thus, we instantiate it here and pass it to
		// writeTestcase instead of using a constant
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

		writer.writeStartElement("testsuite");

		writeSuiteAttributes(testIdentifier, tests.values(), numberFormat, writer);

		newLine(writer);
		writeSystemProperties(writer);

		for (Entry<TestIdentifier, AggregatedTestResult> entry : tests.entrySet()) {
			writeTestcase(entry.getKey(), entry.getValue(), numberFormat, writer);
		}

		writeOutputElement("system-out", formatNonStandardAttributesAsString(testIdentifier), writer);

		writer.writeEndElement();
		newLine(writer);
	}

	private void writeSuiteAttributes(TestIdentifier testIdentifier, Collection<AggregatedTestResult> testResults,
			NumberFormat numberFormat, XMLStreamWriter writer) throws XMLStreamException {

		writeAttributeSafely(writer, "name", testIdentifier.getDisplayName());
		writeTestCounts(testResults, writer);
		writeAttributeSafely(writer, "time", getTime(testIdentifier, numberFormat));
		writeAttributeSafely(writer, "hostname", getHostname().orElse("<unknown host>"));
		writeAttributeSafely(writer, "timestamp", ISO_LOCAL_DATE_TIME.format(getCurrentDateTime()));
	}

	private void writeTestCounts(Collection<AggregatedTestResult> testResults, XMLStreamWriter writer)
			throws XMLStreamException {
		Map<Type, Long> counts = testResults.stream().map(it -> it.type).collect(groupingBy(identity(), counting()));
		long total = counts.values().stream().mapToLong(Long::longValue).sum();
		writeAttributeSafely(writer, "tests", String.valueOf(total));
		writeAttributeSafely(writer, "skipped", counts.getOrDefault(SKIPPED, 0L).toString());
		writeAttributeSafely(writer, "failures", counts.getOrDefault(FAILURE, 0L).toString());
		writeAttributeSafely(writer, "errors", counts.getOrDefault(ERROR, 0L).toString());
	}

	private void writeSystemProperties(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("properties");
		newLine(writer);
		Properties systemProperties = System.getProperties();
		for (String propertyName : new TreeSet<>(systemProperties.stringPropertyNames())) {
			writer.writeEmptyElement("property");
			writeAttributeSafely(writer, "name", propertyName);
			writeAttributeSafely(writer, "value", systemProperties.getProperty(propertyName));
			newLine(writer);
		}
		writer.writeEndElement();
		newLine(writer);
	}

	private Optional<Class<?>> getTestClass(final TestSource source) {
		if (source instanceof MethodSource) {
			return getTestClass(((MethodSource) source).getClassName());
		}
		if (source instanceof ClassSource) {
			return Optional.of(((ClassSource) source).getJavaClass());
		}
		return Optional.empty();
	}

	private Optional<Class<?>> getTestClass(final String className) {
		try {
			return Optional.of(Class.forName(className));
		} catch (ClassNotFoundException e) {
			logger.error(e, () -> "Could not get test class from test source " + className);
		}
		return Optional.empty();
	}

	private Optional<Method> getTestMethod(final TestSource source) {
		if (source instanceof MethodSource) {
			return getTestMethod((MethodSource) source);
		}
		return Optional.empty();
	}

	private Optional<Method> getTestMethod(final MethodSource source) {
		try {
			final Class<?> aClass = Class.forName(source.getClassName());
			return Stream.of(aClass.getDeclaredMethods()).filter(method -> MethodSource.from(method).equals(source))
					.findAny();
		} catch (ClassNotFoundException e) {
			logger.error(e, () -> "Could not get test method from method source " + source);
		}
		return Optional.empty();
	}

	private HashMap<String, String> getTestRunCustomFields(List<ReportEntry> entries) {
		HashMap<String, String> hash = new HashMap<>();

		if (!entries.isEmpty()) {
			for (int i = 0; i < entries.size(); i++) {
				ReportEntry reportEntry = entries.get(i);
				Map<String, String> testrunCustomFields = reportEntry.getKeyValuePairs().entrySet().stream().filter(
						mapItem -> ((String) mapItem.getKey()).startsWith(XrayTestReporter.TESTRUN_CUSTOMFIELD_PREFIX))
						.collect(Collectors.toMap(map -> ((String) map.getKey()).substring(5), map -> map.getValue()));
				hash.putAll(testrunCustomFields);
			}
		}
		return hash;
	}

	private void writeTestcase(TestIdentifier testIdentifier, AggregatedTestResult testResult,
			NumberFormat numberFormat, XMLStreamWriter writer) throws XMLStreamException {

		writer.writeStartElement("testcase");
		writeAttributeSafely(writer, "name", getName(testIdentifier));
		writeAttributeSafely(writer, "classname", getClassName(testIdentifier));
		writeAttributeSafely(writer, "time", getTime(testIdentifier, numberFormat));
		DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
		writeAttributeSafely(writer, "started-at", getStartedAt(testIdentifier, dateFormatter));
		writeAttributeSafely(writer, "finished-at", getFinishedAt(testIdentifier, dateFormatter));
		newLine(writer);

		writeSkippedOrErrorOrFailureElement(testIdentifier, testResult, writer);

		List<String> systemOutElements = new ArrayList<>();
		List<String> systemErrElements = new ArrayList<>();
		systemOutElements.add(formatNonStandardAttributesAsString(testIdentifier));
		collectReportEntries(testIdentifier, systemOutElements, systemErrElements);
		writeOutputElements("system-out", systemOutElements, writer);
		writeOutputElements("system-err", systemErrElements, writer);

		StringBuilder testrunComment = new StringBuilder();
		List<String> testrunComments = new ArrayList<>();
		collectReportEntriesFor(testIdentifier, "xray:comment", testrunComments);
		testrunComments.forEach((comment) -> testrunComment.append(format("{0}\n", comment.trim())));

		writer.writeStartElement("properties");
		newLine(writer);

		if (testrunComment.length() > 0) {
			addPropertyWithInnerContent(writer, "testrun_comment", testrunComment.toString().trim());
			newLine(writer);
		}

		final Optional<TestSource> testSource = testIdentifier.getSource();
		final Optional<Method> testMethod = testSource.flatMap(this::getTestMethod);
		// final Optional<Class<?>> testClass = testSource.flatMap(this::getTestClass);

		Optional<Requirement> requirement = AnnotationSupport.findAnnotation(testMethod, Requirement.class);
		if (requirement.isPresent()) {
			String[] requirements = requirement.get().value();
			addProperty(writer, "requirements", String.join(",", requirements));
		}

		Optional<XrayTest> xrayTest = AnnotationSupport.findAnnotation(testMethod, XrayTest.class);
		String test_key = null;
		String test_id = null;
		String test_summary = null;
		String test_description = null;
		if (xrayTest.isPresent()) {
			test_key = xrayTest.get().key();
			if ((test_key != null) && (!test_key.isEmpty())) {
				addProperty(writer, "test_key", test_key);
			}

			test_id = xrayTest.get().id();
			if ((test_id != null) && (!test_id.isEmpty())) {
				addProperty(writer, "test_id", test_id);
			}

			test_summary = xrayTest.get().summary();
			test_description = xrayTest.get().description();
			if ((test_description != null) && (!test_description.isEmpty())) {
				addPropertyWithInnerContent(writer, "test_description", test_description);
			}
		}

		Optional<ParameterizedTest> paramterizedTest = AnnotationSupport.findAnnotation(testMethod, ParameterizedTest.class);
		Optional<TestFactory> dynamicTest = AnnotationSupport.findAnnotation(testMethod, TestFactory.class);
		Optional<DisplayName> displayName = AnnotationSupport.findAnnotation(testMethod, DisplayName.class);
		if ( ((test_summary == null) || (test_summary.isEmpty())) && (displayName.isPresent()) ) {
			//test_summary = testIdentifier.getDisplayName();
			test_summary = displayName.get().value();
		}
		if ( ((test_summary == null) || (test_summary.isEmpty())) && (dynamicTest.isPresent()) ) {
			test_summary = testIdentifier.getDisplayName();
		}
		if ((test_summary != null) && (!test_summary.isEmpty())) {
			addProperty(writer, "test_summary", test_summary);
		}

		List<String> tags = testIdentifier.getTags().stream().map(TestTag::getName).map(String::trim)
				.collect(Collectors.toList());
		if (!tags.isEmpty()) {
			addProperty(writer, "tags", String.join(",", tags));
		}

		// TODO: get arguments
		// Object[] args = argumentsFrom(testIdentifier);
		// System.out.println("xargs: " + args);
		// System.out.println("xargs.len: " + args.length);

		List<ReportEntry> entries = this.reportData.getReportEntries(testIdentifier);
		HashMap<String, String> testrunCustomFields = getTestRunCustomFields(entries);
		for (Map.Entry customField : testrunCustomFields.entrySet()) {
			addProperty(writer, (String) customField.getKey(), (String) customField.getValue());
		}

		if (!entries.isEmpty()) {
			writer.writeStartElement("property");
			writeAttributeSafely(writer, "name", "testrun_evidence");
			newLine(writer);

			for (int i = 0; i < entries.size(); i++) {
				ReportEntry reportEntry = entries.get(i);

				List<String> files = reportEntry.getKeyValuePairs().entrySet().stream()
						.filter(mapItem -> ((String) mapItem.getKey()).equals(XrayTestReporter.TESTRUN_EVIDENCE))
						.map(item -> item.getValue()).collect(Collectors.toList());

				Base64.Encoder enc = Base64.getEncoder();
				for (String file : files) {
					try {
						byte[] fileContent = Files.readAllBytes(Paths.get(file));
						byte[] encoded = enc.encode(fileContent);
						String encodedStr = new String(encoded, "UTF-8");
						addEvidence(writer, new File(file).getName(), encodedStr);
					} catch (Exception e) {
						logger.error(e, () -> "error encoding evidence " + file);
					}
				}
			}

			writer.writeEndElement(); // property testrun_evidence
			newLine(writer);
		}

		writer.writeEndElement(); // properties
		newLine(writer);

		writer.writeEndElement(); // testcase
		newLine(writer);
	}

	private void addProperty(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
		writer.writeEmptyElement("property");
		writeAttributeSafely(writer, "name", name);
		writeAttributeSafely(writer, "value", value);
		newLine(writer);
	}

	private void addPropertyWithInnerContent(XMLStreamWriter writer, String name, String value)
			throws XMLStreamException {
		writer.writeStartElement("property");
		writeAttributeSafely(writer, "name", name);
		writeCDataSafely(writer, value);
		writer.writeEndElement();
		newLine(writer);
	}

	private void addEvidence(XMLStreamWriter writer, String name, String content) throws XMLStreamException {
		writer.writeStartElement("item");
		writeAttributeSafely(writer, "name", name);
		writer.writeCharacters(content);
		writer.writeEndElement();
		newLine(writer);
	}

	private String getName(TestIdentifier testIdentifier) {
		String legacyName = testIdentifier.getLegacyReportingName();
		int pos = legacyName.indexOf('(');
		if (pos > 0) {
			return legacyName.substring(0, pos);
		} else {
			return legacyName.substring(0, -1);
			// return legacyName;
		}
	}

	private String getClassName(TestIdentifier testIdentifier) {
		return LegacyReportingUtils.getClassName(this.reportData.getTestPlan(), testIdentifier);
	}

	private void writeSkippedOrErrorOrFailureElement(TestIdentifier testIdentifier, AggregatedTestResult testResult,
			XMLStreamWriter writer) throws XMLStreamException {

		if (testResult.type == SKIPPED) {
			writeSkippedElement(this.reportData.getSkipReason(testIdentifier), writer);
		} else {
			Map<Type, List<Optional<Throwable>>> throwablesByType = testResult.getThrowablesByType();
			for (Type type : EnumSet.of(FAILURE, ERROR)) {
				for (Optional<Throwable> throwable : throwablesByType.getOrDefault(type, emptyList())) {
					writeErrorOrFailureElement(type, throwable.orElse(null), writer);
				}
			}
		}
	}

	private void writeSkippedElement(String reason, XMLStreamWriter writer) throws XMLStreamException {
		if (isNotBlank(reason)) {
			writer.writeStartElement("skipped");
			writeCDataSafely(writer, reason);
			writer.writeEndElement();
		} else {
			writer.writeEmptyElement("skipped");
		}
		newLine(writer);
	}

	private void writeErrorOrFailureElement(Type type, Throwable throwable, XMLStreamWriter writer)
			throws XMLStreamException {

		String elementName = type == FAILURE ? "failure" : "error";
		if (throwable != null) {
			writer.writeStartElement(elementName);
			writeFailureAttributesAndContent(throwable, writer);
			writer.writeEndElement();
		} else {
			writer.writeEmptyElement(elementName);
		}
		newLine(writer);
	}

	private void writeFailureAttributesAndContent(Throwable throwable, XMLStreamWriter writer)
			throws XMLStreamException {

		if (throwable.getMessage() != null) {
			writeAttributeSafely(writer, "message", throwable.getMessage());
		}
		writeAttributeSafely(writer, "type", throwable.getClass().getName());
		writeCDataSafely(writer, readStackTrace(throwable));
	}

	private void collectReportEntries(TestIdentifier testIdentifier, List<String> systemOutElements,
			List<String> systemErrElements) {
		List<ReportEntry> entries = this.reportData.getReportEntries(testIdentifier);
		if (!entries.isEmpty()) {
			List<String> systemOutElementsForCapturedOutput = new ArrayList<>();
			StringBuilder formattedReportEntries = new StringBuilder();
			for (int i = 0; i < entries.size(); i++) {
				ReportEntry reportEntry = entries.get(i);
				Map<String, String> keyValuePairs = new LinkedHashMap<>(reportEntry.getKeyValuePairs());
				removeIfPresentAndAddAsSeparateElement(keyValuePairs, STDOUT_REPORT_ENTRY_KEY,
						systemOutElementsForCapturedOutput);
				removeIfPresentAndAddAsSeparateElement(keyValuePairs, STDERR_REPORT_ENTRY_KEY, systemErrElements);
				removeXrayKeys(keyValuePairs);
				if (!keyValuePairs.isEmpty()) {
					buildReportEntryDescription(reportEntry.getTimestamp(), keyValuePairs, i + 1,
							formattedReportEntries);
				}
			}
			systemOutElements.add(formattedReportEntries.toString().trim());
			systemOutElements.addAll(systemOutElementsForCapturedOutput);
		}
	}

	private void collectReportEntriesFor(TestIdentifier testIdentifier, String entryName, List<String> elements) {
		List<ReportEntry> entries = this.reportData.getReportEntries(testIdentifier);
		if (!entries.isEmpty()) {
			for (int i = 0; i < entries.size(); i++) {
				ReportEntry reportEntry = entries.get(i);
				List<String> tempComments = reportEntry.getKeyValuePairs().entrySet().stream()
						.filter(mapItem -> ((String) mapItem.getKey()).equals(entryName)).map(item -> item.getValue())
						.collect(Collectors.toList());
				elements.addAll(tempComments);
			}
		}
	}

	private void removeXrayKeys(Map<String, String> keyValuePairs) {
		keyValuePairs.entrySet().removeIf(entry -> ((String) entry.getKey()).startsWith(XrayTestReporter.XRAY_PREFIX));
	}

	private void removeIfPresentAndAddAsSeparateElement(Map<String, String> keyValuePairs, String key,
			List<String> elements) {
		String value = keyValuePairs.remove(key);
		if (value != null) {
			elements.add(value);
		}
	}

	private void buildReportEntryDescription(LocalDateTime timestamp, Map<String, String> keyValuePairs,
			int entryNumber, StringBuilder result) {
		result.append(
				format("Report Entry #{0} (timestamp: {1})\n", entryNumber, ISO_LOCAL_DATE_TIME.format(timestamp)));
		keyValuePairs.forEach((key, value) -> result.append(format("\t- {0}: {1}\n", key, value)));
	}

	private String getTime(TestIdentifier testIdentifier, NumberFormat numberFormat) {
		return numberFormat.format(this.reportData.getDurationInSeconds(testIdentifier));
	}

	private String getStartedAt(TestIdentifier testIdentifier, DateTimeFormatter dateFormatter) {
		return dateFormatter.format(this.reportData.getStartInstant(testIdentifier));
	}

	private String getFinishedAt(TestIdentifier testIdentifier, DateTimeFormatter dateFormatter) {
		return dateFormatter.format(this.reportData.getEndInstant(testIdentifier));
	}

	private Optional<String> getHostname() {
		try {
			return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			return Optional.empty();
		}
	}

	private LocalDateTime getCurrentDateTime() {
		return LocalDateTime.now(this.reportData.getClock()).withNano(0);
	}

	private String formatNonStandardAttributesAsString(TestIdentifier testIdentifier) {
		return "unique-id: " + testIdentifier.getUniqueId() //
				+ "\ndisplay-name: " + testIdentifier.getDisplayName();
	}

	private void writeOutputElements(String elementName, List<String> elements, XMLStreamWriter writer)
			throws XMLStreamException {
		for (String content : elements) {
			writeOutputElement(elementName, content, writer);
		}
	}

	private void writeOutputElement(String elementName, String content, XMLStreamWriter writer)
			throws XMLStreamException {
		writer.writeStartElement(elementName);
		writeCDataSafely(writer, "\n" + content + "\n");
		writer.writeEndElement();
		newLine(writer);
	}

	private void writeAttributeSafely(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
		writer.writeAttribute(name, escapeIllegalChars(value));
	}

	private void writeCDataSafely(XMLStreamWriter writer, String data) throws XMLStreamException {
		for (String safeDataPart : CDATA_SPLIT_PATTERN.split(escapeIllegalChars(data))) {
			writer.writeCData(safeDataPart);
		}
	}

	static String escapeIllegalChars(String text) {
		if (text.codePoints().allMatch(XmlReportWriter::isAllowedXmlCharacter)) {
			return text;
		}
		StringBuilder result = new StringBuilder(text.length() * 2);
		text.codePoints().forEach(codePoint -> {
			if (isAllowedXmlCharacter(codePoint)) {
				result.appendCodePoint(codePoint);
			} else { // use a Character Reference (cf. https://www.w3.org/TR/xml/#NT-CharRef)
				result.append("&#").append(codePoint).append(';');
			}
		});
		return result.toString();
	}

	private static boolean isAllowedXmlCharacter(int codePoint) {
		// source: https://www.w3.org/TR/xml/#charsets
		return codePoint == 0x9 //
				|| codePoint == 0xA //
				|| codePoint == 0xD //
				|| (codePoint >= 0x20 && codePoint <= 0xD7FF) //
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD) //
				|| (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
	}

	private void newLine(XMLStreamWriter xmlWriter) throws XMLStreamException {
		xmlWriter.writeCharacters("\n");
	}

	private static boolean isFailure(TestExecutionResult result) {
		Optional<Throwable> throwable = result.getThrowable();
		return throwable.isPresent() && throwable.get() instanceof AssertionError;
	}

	static class AggregatedTestResult {

		private static final AggregatedTestResult SKIPPED_RESULT = new AggregatedTestResult(SKIPPED, emptyList());

		public static AggregatedTestResult skipped() {
			return SKIPPED_RESULT;
		}

		public static AggregatedTestResult nonSkipped(List<TestExecutionResult> executionResults) {
			Type type = executionResults.stream() //
					.map(Type::from) //
					.max(naturalOrder()) //
					.orElse(SUCCESS);
			return new AggregatedTestResult(type, executionResults);
		}

		private final Type type;
		private final List<TestExecutionResult> executionResults;

		private AggregatedTestResult(Type type, List<TestExecutionResult> executionResults) {
			this.type = type;
			this.executionResults = executionResults;
		}

		public Map<Type, List<Optional<Throwable>>> getThrowablesByType() {
			return executionResults.stream() //
					.collect(groupingBy(Type::from, mapping(TestExecutionResult::getThrowable, toList())));
		}

		enum Type {

			SUCCESS, SKIPPED, FAILURE, ERROR;

			private static Type from(TestExecutionResult executionResult) {
				if (executionResult.getStatus() == FAILED) {
					return isFailure(executionResult) ? FAILURE : ERROR;
				}
				return SUCCESS;
			}
		}
	}

}
