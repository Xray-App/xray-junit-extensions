/*
 * Copyright 2024-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package app.getxray.xray.junit.customjunitxml;

import app.getxray.xray.junit.customjunitxml.data.MockedTestClass;
import app.getxray.xray.junit.customjunitxml.data.MockedTestWithDisplayNameGeneratorClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.discoveryRequest;
import static org.mockito.Mockito.mock;
import org.junit.platform.engine.OutputDirectoryCreator;

class CustomXrayTestMetadataReaderTest {

    private final XrayTestMetadataReader customMetadataReader = new CustomXrayTestMetadataReader();
    private final Launcher launcher = LauncherFactory.create();

    private static TestDescriptor testWithXraytestAnnotation;
    private static TestDescriptor testWithXraytestEmptyAnnotation;
    private static TestDescriptor testNotAnnotated;
    private static TestDescriptor testWithDisplayNameAnnotation;
    private static TestDescriptor testWithXraytestAndDisplayNameAnnotation;
    private static TestDescriptor testWithDisplayNameGenerator;
    private static TestDescriptor testWithTestFactoryAnnotation;

    private TestPlan discoverTestPlan(DiscoverySelector selector) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selector)
                .build();
        CapturingTestExecutionListener listener = new CapturingTestExecutionListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request); // executes and triggers plan callbacks
        return listener.getPlan();
    }

    private Set<TestIdentifier> getTestIdentifiers(TestPlan testPlan) {
        return testPlan.getRoots().stream()
                .flatMap(root ->
                        testPlan.getDescendants(root).stream().filter(TestIdentifier::isTest)
                )
                .collect(Collectors.toSet());
    }

    @BeforeAll
    static void setUp() throws NoSuchMethodException {
        LauncherDiscoveryRequest discoveryRequest = discoveryRequest()
                .selectors(selectClass(MockedTestClass.class), selectClass(MockedTestWithDisplayNameGeneratorClass.class))
                .build();
        OutputDirectoryCreator dummyOutputDirectoryCreator = discoveryRequest.getOutputDirectoryCreator();
        JupiterConfiguration jupiterConfiguration = new DefaultJupiterConfiguration(mock(), dummyOutputDirectoryCreator, mock());

        Supplier<List<Class<?>>> emptySupplier = () -> Collections.emptyList();

        testWithXraytestAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithValues"),
                emptySupplier, jupiterConfiguration
        );
        testWithXraytestEmptyAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithEmptyValues"),
                emptySupplier, jupiterConfiguration
        );
        testNotAnnotated = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("notAnnotatedTest"),
                emptySupplier, jupiterConfiguration
        );
        testWithDisplayNameAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithDisplayName"),
                emptySupplier, jupiterConfiguration
        );
        testWithXraytestAndDisplayNameAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithXrayTestAndDisplayName"),
                emptySupplier, jupiterConfiguration
        );
        testWithDisplayNameGenerator = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestWithDisplayNameGeneratorClass.class,
                MockedTestWithDisplayNameGeneratorClass.class.getMethod("withDisplayNameGenerator"),
                emptySupplier, jupiterConfiguration
        );
        testWithTestFactoryAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("withTestFactory"),
                emptySupplier, jupiterConfiguration
        );
    }

    @Test
    void shouldReturnKeyFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> keyOpt = customMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .contains("KEY:annotatedWithValues");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyKey() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> keyOpt = customMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .contains("KEY:annotatedWithEmptyValues");
    }

    @Test
    void shouldReturnOptionalEmptyKeyWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> keyOpt = customMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .contains("KEY:notAnnotatedTest");
    }

    @Test
    void shouldReturnIdFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> idOpt = customMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .contains("ID:annotatedWithValues");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyId() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> idOpt = customMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .contains("ID:annotatedWithEmptyValues");
    }

    @Test
    void shouldReturnOptionalEmptyIdWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> idOpt = customMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .contains("ID:notAnnotatedTest");
    }

    @Test
    void shouldReturnSummaryFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:annotatedWithValues");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptySummary() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:annotatedWithEmptyValues");
    }

    @Test
    void shouldReturnOptionalEmptySummaryWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:notAnnotatedTest");
    }

    @Test
    void shouldReturnDisplayNameWhenNotAnnotatedWithXrayTest() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithDisplayNameAnnotation);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:annotatedWithDisplayName");
    }

    @Test
    void shouldReturnDisplayNameFromXrayTestAnnotationWhenAlsoAnnotatedWithDisplayName() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAndDisplayNameAnnotation);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:annotatedWithXrayTestAndDisplayName");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithDisplayNameGenerator() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithDisplayNameGenerator);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:withDisplayNameGenerator");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithTestFactory() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithTestFactoryAnnotation);
        // WHEN
        Optional<String> summaryOpt = customMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("SUMMARY:withTestFactory");
    }

    @Test
    void shouldReturnDescriptionFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> descriptionOpt = customMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .contains("DESCRIPTION:annotatedWithValues");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyDescription() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> descriptionOpt = customMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .contains("DESCRIPTION:annotatedWithEmptyValues");
    }

    @Test
    void shouldReturnOptionalEmptyDescriptionWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> descriptionOpt = customMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .contains("DESCRIPTION:notAnnotatedTest");
    }

    @Test
    void shouldReturnRequirementsFromRequirementsAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        List<String> requirements = customMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyListFromRequirementsAnnotationWithEmptyArray() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        List<String> requirements = customMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyListOfRequirementsWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        List<String> requirements = customMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }

    @Test
    void readsNameAndClassStandardTest() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "legacyTest"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(1)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:legacyTest");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassDisplayNameTest() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "annotatedWithDisplayName"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(1)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:annotatedWithDisplayName");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassParameterizedTest() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "parameterizedTestWithoutDisplayName", "int"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(4)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:parameterizedTestWithoutDisplayName");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassParameterizedTestWithDisplayName() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "parameterizedTestAnnotatedWithDisplayName", "int"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(4)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:parameterizedTestAnnotatedWithDisplayName");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassRepeatedTestWithDisplayName() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "repeatedTestAnnotatedWithDisplayName"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(3)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:repeatedTestAnnotatedWithDisplayName");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassRepeatedTestWithoutDisplayName() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "repeatedTestAnnotatedWithoutDisplayName"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(3)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:repeatedTestAnnotatedWithoutDisplayName");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassTestFactoryTest() {
        TestPlan testPlan = discoverTestPlan(selectMethod(XrayEnabledTestExamples.class, "dynamicTestsFromCollection"));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(1)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:dynamicTestsFromCollection");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.class.getName());
                });
    }

    @Test
    void readsNameAndClassNestedTest() {
        TestPlan testPlan = discoverTestPlan(selectClass(XrayEnabledTestExamples.NestedClass.class));
        Set<TestIdentifier> testIdentifiers = getTestIdentifiers(testPlan);

        assertThat(testIdentifiers)
                .hasSize(1)
                .allSatisfy(testIdentifier -> {
                    assertThat(customMetadataReader.getName(testIdentifier))
                            .isEqualTo("NAME:nestedTest");
                    assertThat(customMetadataReader.getClassName(testIdentifier, testPlan))
                            .isEqualTo("CLASSNAME:" + XrayEnabledTestExamples.NestedClass.class.getName());
                });
    }
}
