/*
 * Copyright 2021-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */


package com.xpandit.xray.junit.customjunitxml;

//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.joox.Match;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import com.xpandit.xray.junit.customjunitxml.EnhancedLegacyXmlReportGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.joox.JOOX.$;

public class EnhancedLegacyXmlTest {

    private static final Class TEST_EXAMPLES_CLASS = XrayEnabledTestExamples.class;
    private static final Runnable FAILING_BLOCK = () -> fail("should fail");
    private static final Runnable SUCCEEDING_TEST = () -> {
    };

	@TempDir
	Path tempDirectory;
    private static final String REPORT_NAME = "TEST-junit-jupiter.xml";


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
		ZoneId zone = ZoneId.systemDefault();
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
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "test_summary").attr("value")).isEqualTo("custom summary");
    }


    @Test
    public void shouldMapXrayTestDescriptionToTestcaseProperty() throws Exception {
        String testMethodName = "annotatedXrayTestWithDescription";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
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
    public void shouldStoreTestRunCommentsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunComments";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.xpandit.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_comment").cdata()).isEqualTo("hello\nworld");
    }

    @Test
    public void shouldStoreTestRunCustomFieldsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunCustomFields";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.xpandit.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfield:cf1").attr("value")).isEqualTo("field1_value");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfield:cf2").attr("value")).isEqualTo("field2_value");
    }

    @Test
    public void shouldStoreTestRunEvidenceToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunEvidence";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.xpandit.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").child("item").attr("name")).isEqualTo("xray.png");

        String file = "src/test/java/com/xpandit/xray/junit/customjunitxml/xray.png";
        Base64.Encoder enc = Base64.getEncoder();
        byte[] fileContent = Files.readAllBytes(Paths.get(file));
        byte[] encoded = enc.encode(fileContent);
        String contentInBase64 = new String(encoded, "UTF-8");


        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").child("item").text()).isEqualTo(contentInBase64);
    }

 

	private Match readValidXmlFile(Path xmlFile) throws Exception {
		assertTrue(Files.exists(xmlFile), () -> "File does not exist: " + xmlFile);
		try (BufferedReader reader = Files.newBufferedReader(xmlFile)) {
			Match xml = $(reader);
			return xml;
		}
	}

}
