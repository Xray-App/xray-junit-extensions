package app.getxray.xray.junit.xray_json;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class XrayJsonReporter implements TestExecutionListener, InvocationInterceptor {

	private static final String DEFAULT_REPORTS_DIR = "./target";
	private static final Logger logger = LoggerFactory.getLogger(XrayJsonReporter.class);
	private final XrayJsonReporterConfig config = new XrayJsonReporterConfig();

	private Path propertiesFile;
	private Path reportsDir;
	private final PrintWriter out;
	private final Clock clock;

	private String reportFilename = null;
	boolean addTimestampToReportFilename = false;
	private XrayReportData reportData;

	// default constructor
	// service discovered automatically at runtime by JUnit
	public XrayJsonReporter() {
		this(FileSystems.getDefault().getPath(DEFAULT_REPORTS_DIR), new PrintWriter(System.out, true),
				Clock.systemDefaultZone());
	}


	public XrayJsonReporter(Path reportsDir, PrintWriter out, Clock clock) {
		this(reportsDir, null, out, clock);
	}

	// main construtor; used directly by tests
	public XrayJsonReporter(Path reportsDir, Path propertiesFile, PrintWriter out, Clock clock) {
		this.reportsDir = reportsDir;
		this.propertiesFile = propertiesFile;
		this.out = out;
		this.clock = clock;

		try {
			
			InputStream stream = null;
			if (propertiesFile == null) {
				stream = getClass().getClassLoader().getResourceAsStream("xray-junit-extensions.properties");
			} else {
				// For tests only
				stream = Files.newInputStream(propertiesFile);
			}

			// if properties exist, or are enforced from the test, then process them
			if (stream != null) {
				Properties properties = new Properties();
				properties.load(stream);
				String customReportFilename = properties.getProperty("report_filename");
				if (customReportFilename != null) {
					this.reportFilename = customReportFilename;
				}
				String customReportsDirectory = properties.getProperty("report_directory");
				if (customReportsDirectory != null) {
					this.reportsDir = FileSystems.getDefault().getPath(customReportsDirectory);
				}
				this.addTimestampToReportFilename = "true".equals(properties.getProperty("add_timestamp_to_report_filename"));


				Optional<String> userOptional = Optional.ofNullable(properties.getProperty("user"));
				userOptional.ifPresent(this.config::setUser);

				Optional<String> summaryOptional = Optional.ofNullable(properties.getProperty("summary"));
				summaryOptional.ifPresent(this.config::setSummary);

				Optional<String> descriptionOptional = Optional.ofNullable(properties.getProperty("description"));
				descriptionOptional.ifPresent(this.config::setDescription);

				Optional<String> projectKeyOptional = Optional.ofNullable(properties.getProperty("project_key"));
				projectKeyOptional.ifPresent(this.config::setProjectKey);

				Optional<String> versionOptional = Optional.ofNullable(properties.getProperty("version"));
				versionOptional.ifPresent(this.config::setVersion);

				Optional<String> revisionOptional = Optional.ofNullable(properties.getProperty("revision"));
				revisionOptional.ifPresent(this.config::setRevision);

				Optional<String> testExecutionKeyOptional = Optional.ofNullable(properties.getProperty("testexecution_key"));
				testExecutionKeyOptional.ifPresent(this.config::setTestExecutionKey);

				Optional<String> testPlanKeyOptional = Optional.ofNullable(properties.getProperty("testplan_key"));
				testPlanKeyOptional.ifPresent(this.config::setTestPlanKey);

				Optional<String> testEnvironmentsOptional = Optional.ofNullable(properties.getProperty("test_environments"));
				testEnvironmentsOptional.ifPresent(this.config::setTestEnvironments);

                this.config.setXrayCloud(!"false".equals(properties.getProperty("xray_cloud")));  // true, if not specified
                this.config.setUseManualTestsForDatadrivenTests(!"false".equals(properties.getProperty("use_manual_tests_for_datadriven_tests"))); // true, if not specified
                this.config.setUseManualTestsForRegularTests("true".equals(properties.getProperty("use_manual_tests_for_regular_tests"))); // false, if not specified


			} else {
				if (reportsDir == null) {
					this.reportsDir = FileSystems.getDefault().getPath(DEFAULT_REPORTS_DIR);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// used whenever registering listener programatically
	public XrayJsonReporter(Path reportsDir, PrintWriter out) {
		this(reportsDir, out, Clock.systemDefaultZone());
	}


    @Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		log("TestPlan Execution Started: %s", testPlan);
		this.reportData = new XrayReportData(testPlan, clock);
		this.reportData.setExecutionsStartedAt(System.currentTimeMillis());
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
		log("TestPlan Execution Finished: %s", testPlan);
		this.reportData.setExecutionsFinishedAt(System.currentTimeMillis());
		//this.reportData = null;
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		log("Dynamic Test Registered: %s - %s - %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), testIdentifier.getLegacyReportingName(), testIdentifier.getParentId());
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		log("Execution Started: %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId());
		this.reportData.markStarted(testIdentifier);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		log("Execution Skipped: %s - %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), reason);
		this.reportData.markSkipped(testIdentifier, reason);
		writeReportInCaseOfRoot(testIdentifier);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		logWithThrowable("Execution Finished: %s - %s - %s", testExecutionResult.getThrowable().orElse(null),
			testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), testExecutionResult);
		this.reportData.markFinished(testIdentifier, testExecutionResult);
		writeReportInCaseOfRoot(testIdentifier);
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		this.reportData.addReportEntry(testIdentifier, entry);
	}

	private void writeReportInCaseOfRoot(TestIdentifier testIdentifier) {
		if (isRoot(testIdentifier)) {
			String rootName = UniqueId.parse(testIdentifier.getUniqueId()).getSegments().get(0).getValue();
			writeReportSafely(testIdentifier, rootName);
		}
	}

	private void writeReportSafely(TestIdentifier testIdentifier, String rootName) {
		Path reportFile;
		String fileName;
		if ((this.reportFilename != null) && (!"".equals(this.reportFilename))) {
			fileName = reportFilename;
		} else {
			fileName = "TEST-" + rootName;
		}
		if (this.addTimestampToReportFilename) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS");
			fileName += "-" + LocalDateTime.now(this.clock).format(formatter);
		}
		fileName += ".json";
		reportFile = this.reportsDir.resolve(fileName);

		try (Writer fileWriter = Files.newBufferedWriter(reportFile)) {
			new XrayJsonReportWriter(this.reportData, this.config).writeXrayJsonReport(testIdentifier, fileWriter);
		} catch (IOException e) {
			printException("Could not write Xray JSON report: " + reportFile, e);
			logger.error(e, () -> "Could not write Xray JSON report: " + reportFile);
		}
	}

	private boolean isRoot(TestIdentifier testIdentifier) {
		return !testIdentifier.getParentId().isPresent();
	}

	private void printException(String message, Exception exception) {
		out.println(message);
		exception.printStackTrace(out);
	}

	private void log(String message, Object... args) {
		logWithThrowable(message, null, args);
	}

	private void logWithThrowable(String message, Throwable t, Object... args) {
		System.out.println(String.format(message, args));
		// this.logger.accept(t, () -> String.format(message, args));
	}

    @Override
    public void interceptTestTemplateMethod(final Invocation<Void> invocation,
                                            final ReflectiveInvocationContext<Method> invocationContext,
                                            final ExtensionContext extensionContext) throws Throwable {
		long beforeTest = System.currentTimeMillis();
		try {
			invocation.proceed();
		} finally {
			long afterTest = System.currentTimeMillis();
			long duration = afterTest - beforeTest;
				
			String testClassName = invocationContext.getTargetClass().getSimpleName();
			String testMethodName = invocationContext.getExecutable().getName();
			System.out.println(String.format("%s.%s: %dms", testClassName, testMethodName, duration));
		}
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
 
        long beforeTest = System.currentTimeMillis();
        try {
            invocation.proceed();
        } finally {
            long afterTest = System.currentTimeMillis();
            long duration = afterTest - beforeTest;
             
            String testClassName = invocationContext.getTargetClass().getSimpleName();
            String testMethodName = invocationContext.getExecutable().getName();
            System.out.println(String.format("%s.%s: %dms", testClassName, testMethodName, duration));
        }
    }


}
