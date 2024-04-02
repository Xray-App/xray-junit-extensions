package app.getxray.xray.junit.xray_json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;

import app.getxray.xray.junit.xray_json.XrayJsonReportWriter.AggregatedTestResult.Type;
import junit.framework.TestResult;
import app.getxray.xray.junit.common.Utils;
import app.getxray.xray.junit.customjunitxml.XrayTestReporter;
import app.getxray.xray.junit.customjunitxml.annotations.Defect;
import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static app.getxray.xray.junit.xray_json.XrayJsonReportWriter.AggregatedTestResult.Type.ERROR;
import static app.getxray.xray.junit.xray_json.XrayJsonReportWriter.AggregatedTestResult.Type.FAILURE;
import static app.getxray.xray.junit.xray_json.XrayJsonReportWriter.AggregatedTestResult.Type.SKIPPED;
import static app.getxray.xray.junit.xray_json.XrayJsonReportWriter.AggregatedTestResult.Type.SUCCESS;
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
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;


import static app.getxray.xray.junit.common.Utils.isStringEmpty;

public class XrayJsonReportWriter {

	// Using zero-width assertions in the split pattern simplifies the splitting
	// process: All split parts
	// (including the first and last one) can be used directly, without having to
	// re-add separator characters.
	private static final Pattern CDATA_SPLIT_PATTERN = Pattern.compile("(?<=]])(?=>)");
	private final XrayReportData reportData;
	private static final Logger logger = LoggerFactory.getLogger(XrayJsonReportWriter.class);
	private final XrayJsonReporterConfig config;
    
	XrayJsonReportWriter(XrayReportData reportData, XrayJsonReporterConfig config) {
		this.reportData = reportData;
		this.config = config;
	}

	void writeXrayJsonReport(TestIdentifier rootDescriptor, Writer out) {
		TestPlan testPlan = this.reportData.getTestPlan();
		Map<TestIdentifier, AggregatedTestResult> tests = testPlan.getDescendants(rootDescriptor) //
				.stream() //
				.filter(testIdentifier -> shouldInclude(testPlan, testIdentifier)) //
				.collect(toMap(identity(), this::toAggregatedResult)); //
		writeXrayJsonReport(rootDescriptor, tests, out);
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

	private void writeXrayJsonReport(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> testResults,
			Writer out)  {

		JSONObject report = new JSONObject();
		JSONObject info = new JSONObject();

        if (!Utils.isStringEmpty(config.getUser())) {
            info.put("user", config.getUser());  
        }
        if (!Utils.isStringEmpty(config.getSummary())) {
            info.put("summary", config.getSummary());  
        }
        if (!Utils.isStringEmpty(config.getDescription())) {
            info.put("description", config.getDescription());  
        }
        if (!Utils.isStringEmpty(config.getProjectKey())) {
            info.put("project", config.getProjectKey());  
        }
        if (!Utils.isStringEmpty(config.getVersion())) {
            info.put("version", config.getVersion());  
        }
        if (!Utils.isStringEmpty(config.getRevision())) {
            info.put("revision", config.getRevision());  
        }
        if (!Utils.isStringEmpty(config.getTestExecutionKey())) {
            report.put("testExecutionKey", config.getTestExecutionKey());  
        }
        if (!Utils.isStringEmpty(config.getTestPlanKey())) {
            info.put("testPlanKey", config.getTestPlanKey());  
        }
        if (!Utils.isStringEmpty(config.getTestEnvironments())) {
            ArrayList<String> envs = new ArrayList<String>(Arrays.asList(config.getTestEnvironments().split(",")));
            info.put("testEnvironments", envs) ; 
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String startDate = dateFormatter.format(this.reportData.getExecutionsStartedAt());
        String finishDate = dateFormatter.format(this.reportData.getExecutionsFinishedAt());
        info.put("startDate", startDate);
        info.put("finishDate", finishDate);
		report.put("info", info);
		JSONArray tests = new JSONArray();

		for (Entry<TestIdentifier, AggregatedTestResult> entry : testResults.entrySet()) {
			addTestResults(tests, entry);
		}

		report.put("tests", tests);

		try (PrintWriter reportWriter = new PrintWriter(new BufferedWriter(out))) {
			reportWriter.println(report.toString());
		} catch (Exception e) {
			logger.error(e, () -> "Problem saving report " + e.getMessage());
		}
	}


	public void addTestResults(JSONArray tests, Entry<TestIdentifier, AggregatedTestResult> entry) {
		TestIdentifier testIdentifier = entry.getKey();
		AggregatedTestResult aggregatedTestResult = entry.getValue();

		System.out.println("Adding test " + testIdentifier.getUniqueId() + " to the amazing report");
		System.out.println("ParentId " + testIdentifier.getParentId() + " to the report");
		System.out.println("Type: " + testIdentifier.getParentIdObject().get().getSegments().get(2).getType());
		System.out.println("Method name: " + ((MethodSource)testIdentifier.getSource().get()).getMethodName());

		logger.debug(() -> " " + testIdentifier.getUniqueId() + " to the report");
		
		JSONObject test = new JSONObject();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");


        String xrayTestKey = null;
        String xrayTestSummary = null;
        String xrayTestDescription = null;
        String testDescription = null;
        String testSummary;
        // TestExecutionResult firstResult = aggregatedTestResult.executionResults.get(0);
        Method method = getTestMethod(testIdentifier.getSource().get()).get();


        // if no testKey was given, use autoprovision mechanism
        boolean autoprovision =  !method.isAnnotationPresent(XrayTest.class) ||  (method.isAnnotationPresent(XrayTest.class)) && (isStringEmpty(method.getAnnotation(XrayTest.class).key()));
      
        String testKey = "";
        if (method.isAnnotationPresent(XrayTest.class)) {
            testKey = method.getAnnotation(XrayTest.class).key();
            if (!isStringEmpty(testKey))
                test.put("testKey", testKey);
        }

		if (method.isAnnotationPresent(Defect.class)) {
			String[] defectKeys = method.getAnnotation(Defect.class).value();
			if (defectKeys.length > 0) {
				test.put("defects", defectKeys);
			}
		}

        if (autoprovision) {
            JSONObject testInfo = new JSONObject();
            testInfo.put("projectKey", config.getProjectKey());

			// get fully qualified name of the method
			String fullyQualifiedName = method.getDeclaringClass().getName() + "." + method.getName();
			// testInfo.put("description", fullyQualifiedName);


            if (method.isAnnotationPresent(Requirement.class)) {
                String[] requirementKeys = method.getAnnotation(Requirement.class).value();
                if (requirementKeys.length > 0) {
                    testInfo.put("requirementKeys", requirementKeys);
                }
            }
            if (method.isAnnotationPresent(XrayTest.class)) {
                if (!isStringEmpty(testKey)) {
                    xrayTestKey = testKey;
                    test.put("testKey", testKey);
                }
            
				List<String> tags = testIdentifier.getTags().stream().map(TestTag::getName).map(String::trim).collect(Collectors.toList());
				if (!tags.isEmpty()) {
					testInfo.put("labels", tags);
				}
 
                xrayTestSummary = method.getAnnotation(XrayTest.class).summary();
                xrayTestDescription = method.getAnnotation(XrayTest.class).description();
            }
            if  (method.isAnnotationPresent(XrayTest.class)) {
                testDescription = method.getAnnotation(XrayTest.class).description();
            }

            // summary should only be added if no testKey was given
            if (isStringEmpty(xrayTestKey)) {
                // override default Test issue summary using the "summary" attribute from the XrayTest annotation
                // or else, with teh "description" of @XrayTest, or from @Test; else use test name (method name or overriden)
                if (!isStringEmpty(xrayTestSummary)) {
                    testSummary = xrayTestSummary;
                } else if (!isStringEmpty(xrayTestDescription)) {
                    testSummary = xrayTestDescription;
                } else if (!isStringEmpty(testDescription)) {
                    testSummary = testDescription;
                } else {
                    // testSummary = firstResult.getName();
					// testSummary = method.getName();
					// testSummary = testIdentifier.getDisplayName();
					testSummary = getName(testIdentifier);
                }
                testInfo.put("summary", testSummary);
            }

			testInfo.put("type", "Generic");
			testInfo.put("definition", fullyQualifiedName);
            test.put("testInfo", testInfo);
        }
        // end autoprovision logic

		// assume non-datadriven for the time being	
		test.put("status", getTestStatus(aggregatedTestResult.type));
		test.put("start", dateFormatter.format(Date.from(this.reportData.getStartInstant(testIdentifier))));
		test.put("finish", dateFormatter.format(Date.from(this.reportData.getEndInstant(testIdentifier))));

		StringBuilder testrunComment = new StringBuilder();
		List<String> testrunComments = new ArrayList<>();
		collectReportEntriesFor(testIdentifier, "xray:comment", testrunComments);
		testrunComments.forEach((comment) -> testrunComment.append(format("{0}\n", comment.trim())));


		if (aggregatedTestResult.type == SKIPPED || aggregatedTestResult.type == ERROR || aggregatedTestResult.type == FAILURE) {
			testrunComment.append(format("{0}\n", generateComment(testIdentifier, aggregatedTestResult)));
		}
		test.put("comment", testrunComment.toString().trim());
		
		// process attachments
		List<ReportEntry> entries = this.reportData.getReportEntries(testIdentifier);
		if (!entries.isEmpty()) {
			JSONArray evidences = new JSONArray();

			for (ReportEntry reportEntry : entries) {
				List<String> files = reportEntry.getKeyValuePairs().entrySet().stream()
												.filter(mapItem -> mapItem.getKey()
																		  .equals(XrayTestReporter.TESTRUN_EVIDENCE))
												.map(Entry::getValue).collect(Collectors.toList());

				Base64.Encoder enc = Base64.getEncoder();
				for (String file : files) {
					try {
						byte[] fileContent = Files.readAllBytes(Paths.get(file));
						byte[] encoded = enc.encode(fileContent);
						String encodedStr = new String(encoded, "UTF-8");
						JSONObject evidence = new JSONObject();
						evidence.put("filename", new File(file).getName());
						evidence.put("data", encodedStr);
						evidence.put("contentType", getContentTypeFor(Paths.get(file).toFile()));
						evidences.put(evidence);
					} catch (Exception e) {
						logger.error(e, () -> "error encoding evidence " + file);
					}
				}
			}

			test.put("evidence", evidences);
		}

		// process custom fields
		Map<String, String> testrunCustomFields = getTestRunCustomFields(entries);
		if (!testrunCustomFields.isEmpty()) {
			JSONArray customFields = new JSONArray();
			for (Map.Entry<String, String> customField : testrunCustomFields.entrySet()) {
				JSONObject customFieldJson = new JSONObject();
				customFieldJson.put("name", customField.getKey());
				customFieldJson.put("value", customField.getValue());
				customFields.put(customFieldJson);
			}
			test.put("customFields", customFields);
		}

		tests.put(test);
	}
    
    private String getContentTypeFor(File file) {
        String filename = file.getName();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        switch (extension) {
            case "png":
                return "image/png";
            case "jpeg":
            case "jpg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "txt":
            case "log":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
            }
    }

	private String getTestStatus(Type status){
		boolean xrayCloud = config.isXrayCloud();
		switch (status) {
			case SUCCESS:
				return xrayCloud ? "PASSED": "PASS";
			case ERROR:
			case FAILURE:
				return xrayCloud ? "FAILED": "FAIL";
			default:
				return xrayCloud ? "TO DO": "TODO";
			}    
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

	private Map<String, String> getTestRunCustomFields(List<ReportEntry> entries) {
		HashMap<String, String> testRunCustomFields = new HashMap<>();

		if (!entries.isEmpty()) {
			for (ReportEntry reportEntry : entries) {
				Map<String, String> entryTestRunCustomFields = reportEntry.getKeyValuePairs().entrySet()
																	 .stream()
																	 .filter(mapItem -> mapItem.getKey().startsWith(XrayTestReporter.TESTRUN_CUSTOMFIELD_PREFIX))
																	 .collect(Collectors.toMap(map -> (map.getKey()).substring(25), Entry::getValue));
				testRunCustomFields.putAll(entryTestRunCustomFields);
			}
		}
		return testRunCustomFields;
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


		List<ReportEntry> entries = this.reportData.getReportEntries(testIdentifier);
		Map<String, String> testrunCustomFields = getTestRunCustomFields(entries);

		if (!testrunCustomFields.isEmpty()) {
			writer.writeStartElement("property");
			writeAttributeSafely(writer, "name", "testrun_customfields");
			newLine(writer);

			for (Map.Entry<String, String> customField : testrunCustomFields.entrySet()) {
				addItem(writer, customField.getKey(), customField.getValue());
			}

			writer.writeEndElement(); // property testrun_customfields
			newLine(writer);
		}


		if (!entries.isEmpty()) {
			writer.writeStartElement("property");
			writeAttributeSafely(writer, "name", "testrun_evidence");
			newLine(writer);

			for (ReportEntry reportEntry : entries) {
				List<String> files = reportEntry.getKeyValuePairs().entrySet().stream()
												.filter(mapItem -> mapItem.getKey()
																		  .equals(XrayTestReporter.TESTRUN_EVIDENCE))
												.map(Entry::getValue).collect(Collectors.toList());

				Base64.Encoder enc = Base64.getEncoder();
				for (String file : files) {
					try {
						byte[] fileContent = Files.readAllBytes(Paths.get(file));
						byte[] encoded = enc.encode(fileContent);
						String encodedStr = new String(encoded, "UTF-8");
						addItem(writer, new File(file).getName(), encodedStr);
					} catch (Exception e) {
						logger.error(e, () -> "error encoding evidence " + file);
					}
				}
			}

			writer.writeEndElement(); // property testrun_evidence
			newLine(writer);
		}

		// quick hack: add a dummy property, to overcome a temporary Xray Cloud parsing issue for empty <properties> element
		// ideally, the <properties> element should not be added if there are no properties for the testcase
		addProperty(writer, "_dummy_", "");

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

	private void addItem(XMLStreamWriter writer, String name, String content) throws XMLStreamException {
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
			return legacyName;
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
			for (ReportEntry reportEntry : entries) {
				List<String> tempComments = reportEntry.getKeyValuePairs()
													   .entrySet()
													   .stream()
													   .filter(mapItem -> mapItem.getKey().equals(entryName))
													   .map(Entry::getValue)
													   .collect(Collectors.toList());
				elements.addAll(tempComments);
			}
		}
	}

	private void removeXrayKeys(Map<String, String> keyValuePairs) {
		keyValuePairs.entrySet().removeIf(entry -> entry.getKey().startsWith(XrayTestReporter.XRAY_PREFIX));
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
		if (text.codePoints().allMatch(XrayJsonReportWriter::isAllowedXmlCharacter)) {
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


	private String generateComment(TestIdentifier testIdentifier, AggregatedTestResult testResult) {
		StringBuilder comment = new StringBuilder();
		if (testResult.type == SKIPPED) {
			return this.reportData.getSkipReason(testIdentifier);
		} else {
			Map<Type, List<Optional<Throwable>>> throwablesByType = testResult.getThrowablesByType();
			for (Type type : EnumSet.of(FAILURE, ERROR)) {
				for (Optional<Throwable> throwable : throwablesByType.getOrDefault(type, emptyList())) {

					// add all throwable as comments to the "comment" field on the test object
					Throwable t = throwable.orElse(null);
					if (t != null) {
						comment.append(t.getMessage() + "\n");
					}
					comment.append(readStackTrace(t));


				}
			}
			return comment.toString();
		}
	}
}
