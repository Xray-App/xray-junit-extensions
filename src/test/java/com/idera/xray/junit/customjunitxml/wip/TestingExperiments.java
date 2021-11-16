/*
 * Copyright 2021-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */


 package com.idera.xray.junit.customjunitxml.wip;


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

import com.idera.xray.junit.customjunitxml.EnhancedLegacyXmlReportGeneratingListener;
import com.idera.xray.junit.customjunitxml.XrayEnabledTestExamples;


import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
//import com.idera.xray.junit.customjunitxml.XrayEnabledTestCase;
import static org.joox.JOOX.$;

public class TestingExperiments {

    private static final Class TEST_EXAMPLES_CLASS = XrayEnabledTestExamples.class;
    private static final Runnable FAILING_BLOCK = () -> fail("should fail");
    private static final Runnable SUCCEEDING_TEST = () -> {
    };
    private DemoHierarchicalTestEngine dummyTestEngine = new DemoHierarchicalTestEngine();

	@TempDir
	Path tempDirectory;
    private static final String REPORT_NAME = "TEST-junit-jupiter.xml";
    
    @Test
    public void xpto() throws Exception {
        DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("engine");
        TestDescriptor test = engine.addTest("successfulTest", SUCCEEDING_TEST);
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectUniqueId(test.getUniqueId())).build();

        // var launcher = createLauncher(engine);
        Launcher launcher = LauncherFactory.create();
        TestExecutionListener listener = mock(TestExecutionListener.class);
        // ...
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(discoveryRequest, listener);

        verify(listener, never()).reportingEntryPublished(any(), any());
    }

    @Test
    @Disabled
    
    public void xpto2() throws Exception {
        DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("engine");
        TestDescriptor test = engine.addTest("successfulTest", SUCCEEDING_TEST);
        LauncherDiscoveryRequest discoveryRequest = request()//
                .selectors(selectUniqueId(test.getUniqueId())).build();

        Launcher launcher = LauncherFactory.create();

        TestExecutionListener listener = mock(TestExecutionListener.class);
        //EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener(
        //       tempDirectory.toString(), out, clock);

        launcher.registerTestExecutionListeners(listener);
        // launcher.execute(discoveryRequest, listener);

        // executeTests(selectClass(DisabledTestMethodsTestCase.class));
        executeTests(engine, listener, FileSystems.getDefault().getPath("./reports"));
        verify(listener, never()).reportingEntryPublished(any(), any());

    }

    @Test
    @Disabled
    public void verifyTestkitExample()  {
        PrintWriter writer = new PrintWriter(System.out);
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(TEST_EXAMPLES_CLASS))
            //.filters(includeClassNamePatterns(".*TestCase"))
            //.selectors(selectClass(Collections.singletonList(TEST_EXAMPLES_CLASS))) 
            .execute()
            .testEvents()
            //.debug(writer) 
            .assertStatistics(stats ->
                stats.skipped(0).started(3).succeeded(1).aborted(0).failed(2));
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
    public void shouldStoreTestRunCommentsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunComments";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.idera.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_comment").cdata()).isEqualTo("hello\nworld");
    }

    @Test
    public void shouldStoreTestRunCustomFieldsToTestcaseProperty() throws Exception {
        String testMethodName = "testWithStringBasedTestRunCustomFields";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.idera.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf1").cdata()).isEqualTo("field1_value");
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf2")).isNotEmpty();
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_customfields").children("item").matchAttr("name", "cf2").cdata()).isEqualTo("field2_value");
    }

    @Test
    public void shouldStoreTestRunEvidenceToTestcaseProperty() throws Exception {
        String testMethodName = "testWithTestRunEvidence";
        executeTestMethodWithParams(TEST_EXAMPLES_CLASS, testMethodName, "com.idera.xray.junit.customjunitxml.XrayTestReporter");
        Match testsuite = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testcase = testsuite.child("testcase");
        assertThat(testcase.attr("name", String.class)).isEqualTo(testMethodName);
        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").child("item").attr("name")).isEqualTo("xray.png");

        String file = "src/test/java/com/idera/xray/junit/customjunitxml/xray.png";
        Base64.Encoder enc = Base64.getEncoder();
        byte[] fileContent = Files.readAllBytes(Paths.get(file));
        byte[] encoded = enc.encode(fileContent);
        String contentInBase64 = new String(encoded, "UTF-8");


        assertThat(testcase.child("properties").children("property").matchAttr("name", "testrun_evidence").child("item").text()).isEqualTo(contentInBase64);
    }


    
    



    /*
     * @Test
     * 
     * @XrayTest(id = "dummyId") public void getXrayTestAnnotation() { XrayTest test
     * = new XrayTest(); Annotation[] annotations =
     * anAnnotatedClass.getClass().getAnnotations(); assertThat(annotations.length,
     * is(1)); }
     */

    /*
     * shouldAddStartedAtAsTestCaseAttribute shouldAddFinsihedAtAsTestCaseAttribute
     */

/*
    private DiscoverySelector selectClass(Class<?> testClass) {
        return DiscoverySelectors.selectClass(testClass);
    }

    private DiscoverySelector selectClasses(List<Class<?>> classes) {
        if (classes.size() == 1) {
            return DiscoverySelectors.selectClass(classes.get(0));
        }
        int lastIndex = classes.size() - 1;
        return DiscoverySelectors.selectNestedClass(classes.subList(0, lastIndex), classes.get(lastIndex));
    }

*/
    private void executeTestsForClass(Class<?> testClass) {
        executeTests(selectClass(testClass));
    }

    protected EngineExecutionResults executeTests(DiscoverySelector... selectors) {
		return executeTests(request().selectors(selectors).build());
	}

	protected EngineExecutionResults executeTests(LauncherDiscoveryRequest request) {
		//return EngineTestKit.execute(this.engine, request);
        return EngineTestKit.engine("junit-jupiter").execute();
	}
       



    private void executeTests(DemoHierarchicalTestEngine engine, TestExecutionListener listener, Path tempDirectory) {
        executeTests(engine, listener, tempDirectory, Clock.systemDefaultZone());
    }

    private void executeTests(DemoHierarchicalTestEngine engine, TestExecutionListener reportListener,
            Path tempDirectory, Clock clock) {
        PrintWriter out = new PrintWriter(new StringWriter());
        // EnhancedLegacyXmlReportGeneratingListener reportListener = new
        // EnhancedLegacyXmlReportGeneratingListener(tempDirectory.toString(), out,
        // clock);
        Launcher launcher = createLauncher(engine);

        launcher.registerTestExecutionListeners(reportListener);
        launcher.execute(request().selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))).build());
    }

    public static Launcher createLauncher(TestEngine... engines) {
        return LauncherFactory.create(createLauncherConfigBuilderWithDisabledServiceLoading() //
                .addTestEngines(engines) //
                .build());
    }

    public static LauncherConfig.Builder createLauncherConfigBuilderWithDisabledServiceLoading() {
        return LauncherConfig.builder() //
                .enableTestEngineAutoRegistration(false) //
                // .enableLauncherDiscoveryListenerAutoRegistration(false) //
                .enableTestExecutionListenerAutoRegistration(false) //
                .enablePostDiscoveryFilterAutoRegistration(false); //
        // .enableLauncherSessionListenerAutoRegistration(false);
    }


	private Match readValidXmlFile(Path xmlFile) throws Exception {
		assertTrue(Files.exists(xmlFile), () -> "File does not exist: " + xmlFile);
		try (BufferedReader reader = Files.newBufferedReader(xmlFile)) {
			Match xml = $(reader);
			//assertValidAccordingToJenkinsSchema(xml.document());
			return xml;
		}
	}

    
    
}
