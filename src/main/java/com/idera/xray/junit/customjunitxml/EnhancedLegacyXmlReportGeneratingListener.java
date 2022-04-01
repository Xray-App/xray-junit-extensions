/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.idera.xray.junit.customjunitxml;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Properties;
import java.time.format.DateTimeFormatter;

/**
 * {@code EnhancedLegacyXmlReportGeneratingListener} is a
 * {@link TestExecutionListener} that generates a separate XML report for each
 * {@linkplain TestPlan#getRoots() root} in the {@link TestPlan}.
 *
 * <p>
 * Note that the generated XML format is compatible with the <em>legacy</em> de
 * facto standard for JUnit 4 based test reports that was made popular by the
 * Ant build system.
 *
 * @since 1.4
 * @see org.junit.platform.launcher.listeners.LoggingListener
 * @see org.junit.platform.launcher.listeners.SummaryGeneratingListener
 */

public class EnhancedLegacyXmlReportGeneratingListener implements TestExecutionListener {

	private static final String DEFAULT_REPORTS_DIR = "./reports";
	private static final Logger logger = LoggerFactory.getLogger(EnhancedLegacyXmlReportGeneratingListener.class);

	private final Path reportsDir;
	private final PrintWriter out;
	private final Clock clock;

	private String reportFilename = null;
	boolean addTimestampToReportFilename = false;

	private XmlReportData reportData;

	// For tests only
	public EnhancedLegacyXmlReportGeneratingListener(Path reportsDir, PrintWriter out, Clock clock) {
		this.reportsDir = reportsDir;
		this.out = out;
		this.clock = clock;

		try {
			InputStream stream = getClass().getClassLoader().getResourceAsStream("xray-junit-extensions.properties");
			if (stream != null) {
				Properties properties = new Properties();
				properties.load(stream);
				this.reportFilename = properties.getProperty("report_filename");
				this.addTimestampToReportFilename = "true".equals(properties.getProperty("add_timestamp_to_report_filename"));
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	public EnhancedLegacyXmlReportGeneratingListener() {
		this(FileSystems.getDefault().getPath(DEFAULT_REPORTS_DIR), new PrintWriter(System.out, true),
				Clock.systemDefaultZone());
	}

	public EnhancedLegacyXmlReportGeneratingListener(Path reportsDir, PrintWriter out) {
		this(reportsDir, out, Clock.systemDefaultZone());
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.reportData = new XmlReportData(testPlan, clock);
		try {
			Files.createDirectories(this.reportsDir);
		} catch (IOException e) {
			printException("Could not create reports directory: " + this.reportsDir, e);
			logger.error(e, () -> "Could not create reports directory: " + this.reportsDir);
		}
		// note: it's possible to get parent for the testidentifier from the testPlan
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		this.reportData = null;
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		this.reportData.markSkipped(testIdentifier, reason);
		writeXmlReportInCaseOfRoot(testIdentifier);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		this.reportData.markStarted(testIdentifier);
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		this.reportData.addReportEntry(testIdentifier, entry);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		this.reportData.markFinished(testIdentifier, result);
		writeXmlReportInCaseOfRoot(testIdentifier);
	}

	private void writeXmlReportInCaseOfRoot(TestIdentifier testIdentifier) {
		if (isRoot(testIdentifier)) {
			String rootName = UniqueId.parse(testIdentifier.getUniqueId()).getSegments().get(0).getValue();
			writeXmlReportSafely(testIdentifier, rootName);
		}
	}

	private void writeXmlReportSafely(TestIdentifier testIdentifier, String rootName) {
		Path xmlFile;
		String fileName;
		if ((this.reportFilename != null) && (!"".equals(this.reportFilename))) {
			fileName = reportFilename;
		} else {
			fileName = "TEST-" + rootName;
		}
		if (this.addTimestampToReportFilename) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH:mm:ss_S");
			fileName += "-" + LocalDateTime.now().format(formatter);
		}
		fileName += ".xml";
		xmlFile = this.reportsDir.resolve(fileName);

		try (Writer fileWriter = Files.newBufferedWriter(xmlFile)) {
			new XmlReportWriter(this.reportData).writeXmlReport(testIdentifier, fileWriter);
		} catch (XMLStreamException | IOException e) {
			printException("Could not write XML report: " + xmlFile, e);
			logger.error(e, () -> "Could not write XML report: " + xmlFile);
		}
	}

	private boolean isRoot(TestIdentifier testIdentifier) {
		return !testIdentifier.getParentId().isPresent();
	}

	private void printException(String message, Exception exception) {
		out.println(message);
		exception.printStackTrace(out);
	}

}
