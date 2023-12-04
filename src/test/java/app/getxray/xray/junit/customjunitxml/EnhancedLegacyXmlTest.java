/*
/*
 * Copyright 2021-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */


package app.getxray.xray.junit.customjunitxml;

import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.joox.JOOX.$;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.joox.Match;
import org.junit.jupiter.api.Disabled;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import app.getxray.xray.junit.customjunitxml.EnhancedLegacyXmlReportGeneratingListener;

public class EnhancedLegacyXmlTest {

    private static final Class TEST_EXAMPLES_CLASS = XrayEnabledTestExamples.class;
    private static final Runnable FAILING_BLOCK = () -> fail("should fail");
    private static final Runnable SUCCEEDING_TEST = () -> {
    };

	@TempDir
	Path tempDirectory;
    private static final String REPORT_NAME = "TEST-junit-jupiter.xml";


    @Test
    public void shouldSupportCustomReportNames() throws Exception {
        String testMethodName = "legacyTest";

        String customProperties = "report_filename=custom-report-junit\n# report_directory=reports\n# add_timestamp_to_report_filename=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());

        String reportName = "custom-report-junit.xml";
        Match testsuite = readValidXmlFile(tempDirectory.resolve(reportName));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void shouldSupportCustomReportNamesWithTimestamp() throws Exception {
        String testMethodName = "legacyTest";
        String customProperties = "report_filename=custom-report-junit\n# report_directory=reports\nadd_timestamp_to_report_filename=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String fakeTimestamp = "2021-03-24T12:01:02.456";
        LocalDateTime now = LocalDateTime.parse(fakeTimestamp);
        ZoneId zone = ZoneId.of("UTC");
        Clock clock = Clock.fixed(ZonedDateTime.of(now, zone).toInstant(), zone);

        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", clock);

        String reportName = "custom-report-junit-2021_03_24-12_01_02_456.xml";
        Match testsuite = readValidXmlFile(tempDirectory.resolve(reportName));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void shouldSupportCustomReportAbsoluteDirectory() throws Exception {
        String testMethodName = "legacyTest";

        String customReportDir = tempDirectory.resolve("custom_reports").toString();
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        //String customProperties = "#report_filename=custom-report-junit\nreport_directory=" + customReportDir + "\n# add_timestamp_to_report_filename=true\n";
        //Files.write(customPropertiesFile, customProperties.getBytes());

        Properties properties = new Properties();
        OutputStream outputStream = new FileOutputStream(customPropertiesFile.toString());
        properties.setProperty("report_directory", customReportDir);
        properties.store(outputStream, null);

        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());
        
        Match testsuite = readValidXmlFile(tempDirectory.resolve("custom_reports").resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void shouldSupportCustomReportRelativeDirectory() throws Exception {
        String testMethodName = "legacyTest";
        String relativeCustomReportDir = "target/custom_reports";
        String customProperties = "#report_filename=custom-report-junit\nreport_directory=" + relativeCustomReportDir + "\n# add_timestamp_to_report_filename=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());
        
        Match testsuite = readValidXmlFile(FileSystems.getDefault().getPath(".").resolve(relativeCustomReportDir).resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void withReportAnnotatedTestsReportAnnotatedXrayTest() throws Exception {
        String testMethodName = "annotatedXrayTestWithKey";
        String customProperties = "#report_filename=custom-report-junit\n#report_directory=\n# add_timestamp_to_report_filename=true\nreport_only_annotated_tests=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());

        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key").attr("value")).isEqualTo("CALC-100");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void withReportAnnotatedTestsReportAnnotatedRequirement() throws Exception {
        String testMethodName = "annotatedWithOneRequirement";
        String customProperties = "#report_filename=custom-report-junit\n#report_directory=\n# add_timestamp_to_report_filename=true\nreport_only_annotated_tests=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());

        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements").attr("value")).isEqualTo("CALC-123");
    }

    @Test
    public void withReportAnnotatedTestsDontReportNonAnnotated() throws Exception {
        String testMethodName = "legacyTest";
        String customProperties = "#report_filename=custom-report-junit\n#report_directory=\n# add_timestamp_to_report_filename=true\nreport_only_annotated_tests=true\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());

        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase).isNullOrEmpty();
    }

    @Test
    public void withoutReportAnnotatedTestsReportAllTests() throws Exception {
        String testMethodName = "legacyTest";
        String customProperties = "#report_filename=custom-report-junit\n#report_directory=\n# add_timestamp_to_report_filename=true\n"+"report_only_annotated_tests=false\n";
        Path customPropertiesFile = Files.createTempFile("xray-junit-extensions", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());
        executeTestMethodWithCustomProperties(TEST_EXAMPLES_CLASS, testMethodName, customPropertiesFile, "", Clock.systemDefaultZone());

        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void simpleTestShouldNotHaveXrayProperties() throws Exception {
        String testMethodName = "legacyTest";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);

        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags")).isEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements")).isEmpty();
    }

    @Test
    public void testsShouldHaveStartAndFinishTimestamps() throws Exception {
        String testMethodName = "legacyTest";

        String fakeTimestamp = "2021-03-24T12:01:02.456";
        LocalDateTime now = LocalDateTime.parse(fakeTimestamp);
		ZoneId zone = ZoneId.of("UTC");
        Clock clock = Clock.fixed(ZonedDateTime.of(now, zone).toInstant(), zone);

        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName, clock);
        
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        //assertThat(testcase.attr("started-at")).isNotEmpty();
        assertThat(testcase.attr("started-at")).isEqualTo(fakeTimestamp);
        //assertThat(testcase.attr("finished-at")).isNotEmpty();
        assertThat(testcase.attr("finished-at")).isEqualTo(fakeTimestamp);
    }


    @Test
    public void shouldMapXrayTestIdToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedXrayTestWithCustomId";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_id").attr("value")).isEqualTo("myCustomId");
    }

    @Test
    public void shouldMapXrayTestKeyToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedXrayTestWithKey";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_key").attr("value")).isEqualTo("CALC-100");
    }
    

    @Test
    public void shouldMapXrayTestSummaryToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedXrayTestWithSummary";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        //System.out.println(tempDirectory.resolve(REPORT_NAME));
        //Thread.sleep(10000);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("custom summary");
    }


    @Test
    public void shouldMapXrayTestDescriptionToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedXrayTestWithDescription";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        //System.out.println(tempDirectory.resolve(REPORT_NAME));
        //Thread.sleep(10000);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_description").cdata()).isEqualTo("custom description");
    }


    @Test
    public void shouldMapOneRequirementToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedWithOneRequirement";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements").attr("value")).isEqualTo("CALC-123");
    }

    @Test
    public void shouldMapMultipleRequirementsToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedWithMultipleRequirements";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "requirements").attr("value")).isEqualTo("CALC-123,CALC-124");
    }

    @Test
    public void shouldMapOneTagToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedWithOneTag";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags").attr("value")).isEqualTo("tag1");
    }

    @Test
    public void shouldMapMultipleTagsToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedWithMultipleTags";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "tags").attr("value")).isEqualTo("tag1,tag2");
    }

    private void executeTestMethod(Class<?> testClass, String methodName, Clock clock) {
        executeTestMethodWithParams(testClass, methodName, "", clock);      
    }

    private void executeTestMethod(Class<?> testClass, String methodName) {
        executeTestMethodWithParams(testClass, methodName, "");      
    }

    private void executeTestMethodWithCustomProperties(Class<?> testClass, String methodName, Path propertiesPath,  String methodParameterTypes, Clock clock) {
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectMethod(testClass, methodName, ""))
                .build();
        Launcher launcher = LauncherFactory.create();
        EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener(tempDirectory, propertiesPath, new PrintWriter(System.out), clock);
        launcher.execute(discoveryRequest, listener);  
    }

    private void executeTestMethodWithParams(Class<?> testClass, String methodName, String methodParameterTypes) {
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectMethod(testClass, methodName, methodParameterTypes))
                .build();
        Launcher launcher = LauncherFactory.create();
        EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener(tempDirectory, new PrintWriter(System.out));
        //launcher.registerTestExecutionListeners(listener);
        launcher.execute(discoveryRequest, listener);        
    }

    private void executeTestMethodWithParams(Class<?> testClass, String methodName, String methodParameterTypes, Clock clock) {
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectMethod(testClass, methodName, methodParameterTypes))
                .build();
        Launcher launcher = LauncherFactory.create();
        EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener(tempDirectory, new PrintWriter(System.out), clock);
        //launcher.registerTestExecutionListeners(listener);
        launcher.execute(discoveryRequest, listener);        
    }

    @Test
    public void shouldMapDisplayNameToTestSummaryProperty() throws Exception {
        String testMethodName = "annotatedWithDisplayName";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("custom name");
    }

    @Test
    public void shouldMapDisplayNameInParameterizedTestToTestSummaryProperty() throws Exception {
        String testMethodName = "parameterizedTestAnnotatedWithDisplayName";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "int");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("custom name");
    }

    @Test
    public void shouldNotMapMethodNameInParameterizedTestToTestSummaryProperty() throws Exception {
        String testMethodName = "parameterizedTestWithoutDisplayName";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "int");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty(); // isEqualTo("parameterizedTestWithoutDisplayName");
    }
    

    @Test
    public void shouldMapDisplayNameInRepeatedTestToTestSummaryProperty() throws Exception {
        String testMethodName = "repeatedTestAnnotatedWithDisplayName";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("custom name");
    }

    @Test
    public void shouldNotMapMethodNameInRepeatedTestToTestSummaryProperty() throws Exception {
        String testMethodName = "repeatedTestAnnotatedWithoutDisplayName";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary")).isEmpty(); // isEqualTo("parameterizedTestWithoutDisplayName");
    }


    @Test
    public void shouldMapDisplayNameInDynamicTestToTestSummaryProperty() throws Exception {
        String testMethodName = "dynamicTestsFromCollection";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("1st dynamic test");
    }

    @Test
    public void shouldCreateReportEntryForAComment() throws Exception {
        String testMethodName = "testWithTestRunComment";
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectMethod(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter"))
                .build();
        Launcher launcher = LauncherFactory.create();

        EnhancedLegacyXmlReportGeneratingListener listener = mock(EnhancedLegacyXmlReportGeneratingListener.class);
        //EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener(tempDirectory, new PrintWriter(System.out));
        launcher.execute(discoveryRequest, listener);    

        ArgumentCaptor<TestPlan> testPlanArgumentCaptor = ArgumentCaptor.forClass(TestPlan.class);
		InOrder inOrder = inOrder(listener);
		inOrder.verify(listener).testPlanExecutionStarted(testPlanArgumentCaptor.capture());
		TestPlan testPlan = testPlanArgumentCaptor.getValue();
		//TestIdentifier testIdentifier = testPlan.getTestIdentifier("test.getUniqueId().toString()");

        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		//inOrder.verify(listener).reportingEntryPublished(same(testIdentifier), reportEntryArgumentCaptor.capture());
        inOrder.verify(listener, times(1)).reportingEntryPublished(any(TestIdentifier.class), reportEntryArgumentCaptor.capture());
		//Mockito.verify(listener).executionFinished(testIdentifier, successful());
		ReportEntry reportEntry = reportEntryArgumentCaptor.getValue();
		assertThat(reportEntry.getKeyValuePairs()).containsExactly(entry("xray:comment", "hello"));
    }

    @Test
    public void shouldCreateReportEntryForComments() throws Exception {
        String testMethodName = "testWithTestRunComments";
        LauncherDiscoveryRequest discoveryRequest = request()
                .selectors(selectMethod(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter"))
                .build();
        Launcher launcher = LauncherFactory.create();

        EnhancedLegacyXmlReportGeneratingListener listener = mock(EnhancedLegacyXmlReportGeneratingListener.class);
        launcher.execute(discoveryRequest, listener);    

        ArgumentCaptor<TestPlan> testPlanArgumentCaptor = ArgumentCaptor.forClass(TestPlan.class);
		InOrder inOrder = inOrder(listener);
		inOrder.verify(listener).testPlanExecutionStarted(testPlanArgumentCaptor.capture());
		TestPlan testPlan = testPlanArgumentCaptor.getValue();
		//TestIdentifier testIdentifier = testPlan.getTestIdentifier("test.getUniqueId().toString()");

        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		//inOrder.verify(listener).reportingEntryPublished(same(testIdentifier), reportEntryArgumentCaptor.capture());
        inOrder.verify(listener, times(2)).reportingEntryPublished(any(TestIdentifier.class), reportEntryArgumentCaptor.capture());
		//Mockito.verify(listener).executionFinished(testIdentifier, successful());
		//ReportEntry reportEntry = reportEntryArgumentCaptor.getValue();
		//assertThat(reportEntry.getKeyValuePairs()).containsExactly(entry("xray:comment", "hellso"));//, entry("xray:comment", "world"));


        //List<Map<String,String>> entries = reportEntryArgumentCaptor.getAllValues().stream().flatMap(reportEntry -> reportEntry.getKeyValuePairs()).collect(Collectors.toList());
        //assertThat(entries).containsExactly(entry("xray:comment", "hello"), entry("xray:comment", "world"));

        assertThat(reportEntryArgumentCaptor.getAllValues().get(0).getKeyValuePairs()).containsExactly(entry("xray:comment", "hello"));
        assertThat(reportEntryArgumentCaptor.getAllValues().get(1).getKeyValuePairs()).containsExactly(entry("xray:comment", "world"));
    }



    @Test
    public void shouldStoreTestRunCommentsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunComments";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter");
        //System.out.println(tempDirectory.resolve(REPORT_NAME));
        //Thread.sleep(10000);
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_comment").cdata()).isEqualTo("hello\nworld");
    }

    @Test
    public void shouldStoreTestRunCustomFieldsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithStringBasedTestRunCustomFields";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);

/*
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").get(0).getAttribute("name")).isEqualTo("cf2");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").get(0).getTextContent()).isEqualTo("field2_value");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").get(1).getAttribute("name")).isEqualTo("cf1");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").get(1).getTextContent()).isEqualTo("field1_value");
*/
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1").cdata()).isEqualTo("field1_value");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf2")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf2").cdata()).isEqualTo("field2_value");
    }

    @Test
    public void shouldCreateReportEntryForCustomFields() throws Exception {
        String testMethodName = "testWithStringBasedTestRunCustomFields";
        LauncherDiscoveryRequest discoveryRequest = request()
                .selectors(selectMethod(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter"))
                .build();
        Launcher launcher = LauncherFactory.create();

        EnhancedLegacyXmlReportGeneratingListener listener = mock(EnhancedLegacyXmlReportGeneratingListener.class);
        launcher.execute(discoveryRequest, listener);    

		InOrder inOrder = inOrder(listener);
        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        inOrder.verify(listener, times(2)).reportingEntryPublished(any(TestIdentifier.class), reportEntryArgumentCaptor.capture());
        assertThat(reportEntryArgumentCaptor.getAllValues().get(0).getKeyValuePairs()).containsExactly(entry("xray:testrun_customfield:cf1", "field1_value"));
        assertThat(reportEntryArgumentCaptor.getAllValues().get(1).getKeyValuePairs()).containsExactly(entry("xray:testrun_customfield:cf2", "field2_value"));
    }

    @Test
    public void shouldCreateReportEntryForArrayBasedCustomFields() throws Exception {
        String testMethodName = "testWithArrayBasedTestRunCustomField";
        LauncherDiscoveryRequest discoveryRequest = request()
                .selectors(selectMethod(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter"))
                .build();
        Launcher launcher = LauncherFactory.create();

        EnhancedLegacyXmlReportGeneratingListener listener = mock(EnhancedLegacyXmlReportGeneratingListener.class);
        launcher.execute(discoveryRequest, listener);    

		InOrder inOrder = inOrder(listener);
        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        inOrder.verify(listener, times(1)).reportingEntryPublished(any(TestIdentifier.class), reportEntryArgumentCaptor.capture());
        assertThat(reportEntryArgumentCaptor.getAllValues().get(0).getKeyValuePairs()).containsExactly(entry("xray:testrun_customfield:cf1", "Porto;Lisbon"));
    }

    @Test
    public void shouldStoreArrayBasedTestRunCustomFieldsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithArrayBasedTestRunCustomField";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1").cdata()).isEqualTo("Porto;Lisbon");
    }

    @Test
    public void shouldStoreArrayBasedTestRunCustomFieldAndSpecialCharsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithArrayBasedTestRunCustomFieldAndSpecialChars";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1").cdata()).isEqualTo("P\\;orto\\;;Lis\\\\bon\\\\");
    }

    @Test
    public void shouldStoreTestRunEvidenceToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunEvidence";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").children("item").attr("name")).isEqualTo("xray.png");

        String file = "src/test/java/app/getxray/xray/junit/customjunitxml/xray.png";
        Base64.Encoder enc = Base64.getEncoder();
        byte[] fileContent = Files.readAllBytes(Paths.get(file));
        byte[] encoded = enc.encode(fileContent);
        String contentInBase64 = new String(encoded, "UTF-8");


        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").child("item").text()).isEqualTo(contentInBase64);
    }

 
    @Test
    public void shouldCreateReportEntryForEvidence() throws Exception {
        String testMethodName = "testWithTestRunEvidence";
        LauncherDiscoveryRequest discoveryRequest = request()
                .selectors(selectMethod(TEST_EXAMPLES_CLASS, testMethodName, "app.getxray.xray.junit.customjunitxml.XrayTestReporter"))
                .build();
        Launcher launcher = LauncherFactory.create();

        EnhancedLegacyXmlReportGeneratingListener listener = mock(EnhancedLegacyXmlReportGeneratingListener.class);
        launcher.execute(discoveryRequest, listener);    

		InOrder inOrder = inOrder(listener);
        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        inOrder.verify(listener, times(1)).reportingEntryPublished(any(TestIdentifier.class), reportEntryArgumentCaptor.capture());
        assertThat(reportEntryArgumentCaptor.getAllValues().get(0).getKeyValuePairs()).containsExactly(entry("xray:evidence", FileSystems.getDefault().getPath("src/test/java/app/getxray/xray/junit/customjunitxml/xray.png").toAbsolutePath().toString()));
    }

	private Match readValidXmlFile(Path xmlFile) throws Exception {
		assertTrue(Files.exists(xmlFile), () -> "File does not exist: " + xmlFile);
		try (BufferedReader reader = Files.newBufferedReader(xmlFile)) {
			Match xml = $(reader);
            assertValidAccordingToJenkinsSchema(xml.document());
			return xml;
		}
	}

	static void assertValidAccordingToJenkinsSchema(Document document) throws Exception {
		try {
			// Schema is thread-safe, Validator is not
			Validator validator = CachedSchema.JENKINS.newValidator();
			validator.validate(new DOMSource(document));
		}
		catch (Exception e) {
			fail("Invalid XML document: " + document, e);
		}
	}

	private enum CachedSchema {

		JENKINS("/enhanced-jenkins-junit.xsd");

		private final Schema schema;

		CachedSchema(String resourcePath) {
			URL schemaFile = EnhancedLegacyXmlReportGeneratingListener.class.getResource(resourcePath);
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				this.schema = schemaFactory.newSchema(schemaFile);
			}
			catch (SAXException e) {
				throw new RuntimeException("Failed to create schema using " + schemaFile, e);
			}
		}

		Validator newValidator() {
			return schema.newValidator();
		}
	}

}
