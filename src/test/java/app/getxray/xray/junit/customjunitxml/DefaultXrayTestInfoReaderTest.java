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
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultXrayTestInfoReaderTest {

    private final XrayTestMetadataReader xrayTestMetadataReader = new DefaultXrayTestMetadataReader();
    private static TestDescriptor testWithXraytestAnnotation;
    private static TestDescriptor testWithXraytestEmptyAnnotation;
    private static TestDescriptor testNotAnnotated;
    private static TestDescriptor testWithDisplayNameAnnotation;
    private static TestDescriptor testWithXraytestAndDisplayNameAnnotation;
    private static TestDescriptor testWithDisplayNameGenerator;
    private static TestDescriptor testWithTestFactoryAnnotation;

    @BeforeAll
    static void setUp() throws NoSuchMethodException {
        JupiterConfiguration jupiterConfiguration = new DefaultJupiterConfiguration(
                new ConfigurationParameters() {
                    @Override
                    public Optional<String> get(String key) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Boolean> getBoolean(String key) {
                        return Optional.empty();
                    }

                    @Override
                    public int size() {
                        return 0;
                    }

                    @Override
                    public Set<String> keySet() {
                        return Collections.emptySet();
                    }
                }, null
        );

        Supplier<List<Class<?>>> emptySupplier = () -> Collections.emptyList();

        // create a supplier with just one class
        Supplier<List<Class<?>>> oneClassSupplier = () -> Collections.singletonList(MockedTestClass.class);

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
                oneClassSupplier, jupiterConfiguration
        );
        testWithXraytestAndDisplayNameAnnotation = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithXrayTestAndDisplayName"),
                oneClassSupplier, jupiterConfiguration
        );
        testWithDisplayNameGenerator = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestWithDisplayNameGeneratorClass.class,
                MockedTestWithDisplayNameGeneratorClass.class.getMethod("withDisplayNameGenerator"),
                oneClassSupplier, jupiterConfiguration
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
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .contains("ABC-123");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyKey() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyKeyWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnIdFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .contains("868");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyId() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyIdWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnSummaryFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("some summary");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptySummary() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptySummaryWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnDisplayNameWhenNotAnnotatedWithXrayTest() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithDisplayNameAnnotation);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("display name");
    }

    @Test
    void shouldReturnDisplayNameFromXrayTestAnnotationWhenAlsoAnnotatedWithDisplayName() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAndDisplayNameAnnotation);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("xray summary");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithDisplayNameGenerator() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithDisplayNameGenerator);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("DisplayNameForMethod");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithTestFactory() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithTestFactoryAnnotation);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("withTestFactory()");
    }

    @Test
    void shouldReturnDescriptionFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .contains("some description");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyDescription() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyDescriptionWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnRequirementsFromRequirementsAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestAnnotation);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .containsExactly("REQ-1", "REQ-2");
    }

    @Test
    void shouldReturnEmptyListFromRequirementsAnnotationWithEmptyArray() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testWithXraytestEmptyAnnotation);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyListOfRequirementsWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(testNotAnnotated);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }
}
