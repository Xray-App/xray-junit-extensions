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

import static org.assertj.core.api.Assertions.assertThat;

class DefaultXrayTestInfoReaderTest {

    private final XrayTestMetadataReader xrayTestMetadataReader = new DefaultXrayTestMetadataReader();
    private static TestDescriptor XRAYTEST_ANNOTATED;
    private static TestDescriptor XRAYTEST_EMPTY_ANNOTATION;
    private static TestDescriptor NOT_ANNOTATED;
    private static TestDescriptor DISPLAY_NAME_ANNOTATED;
    private static TestDescriptor XRAYTEST_AND_DISPLAY_NAME_ANNOTATED;
    private static TestDescriptor WITH_DISPLAY_NAME_GENERATOR;
    private static TestDescriptor WITH_TEST_FACTORY;

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
                }
        );

        XRAYTEST_ANNOTATED = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithValues"),
                jupiterConfiguration
        );
        XRAYTEST_EMPTY_ANNOTATION = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithEmptyValues"),
                jupiterConfiguration
        );
        NOT_ANNOTATED = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("notAnnotatedTest"),
                jupiterConfiguration
        );
        DISPLAY_NAME_ANNOTATED = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithDisplayName"),
                jupiterConfiguration
        );
        XRAYTEST_AND_DISPLAY_NAME_ANNOTATED = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("annotatedWithXrayTestAndDisplayName"),
                jupiterConfiguration
        );
        WITH_DISPLAY_NAME_GENERATOR = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestWithDisplayNameGeneratorClass.class,
                MockedTestWithDisplayNameGeneratorClass.class.getMethod("withDisplayNameGenerator"),
                jupiterConfiguration
        );
        WITH_TEST_FACTORY = new TestMethodTestDescriptor(
                UniqueId.forEngine("foo"),
                MockedTestClass.class,
                MockedTestClass.class.getMethod("withTestFactory"),
                jupiterConfiguration
        );
    }

    @Test
    void shouldReturnKeyFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_ANNOTATED);
        // WHEN
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .contains("ABC-123");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyKey() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_EMPTY_ANNOTATION);
        // WHEN
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyKeyWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(NOT_ANNOTATED);
        // WHEN
        Optional<String> keyOpt = xrayTestMetadataReader.getKey(testIdentifier);
        // THEN
        assertThat(keyOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnIdFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_ANNOTATED);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .contains("868");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyId() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_EMPTY_ANNOTATION);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyIdWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(NOT_ANNOTATED);
        // WHEN
        Optional<String> idOpt = xrayTestMetadataReader.getId(testIdentifier);
        // THEN
        assertThat(idOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnSummaryFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_ANNOTATED);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("some summary");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptySummary() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_EMPTY_ANNOTATION);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptySummaryWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(NOT_ANNOTATED);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnDisplayNameWhenNotAnnotatedWithXrayTest() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(DISPLAY_NAME_ANNOTATED);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("display name");
    }

    @Test
    void shouldReturnDisplayNameFromXrayTestAnnotationWhenAlsoAnnotatedWithDisplayName() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_AND_DISPLAY_NAME_ANNOTATED);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("xray summary");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithDisplayNameGenerator() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(WITH_DISPLAY_NAME_GENERATOR);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("DisplayNameForMethod");
    }

    @Test
    void shouldReturnDefaultDisplayNameWhenAnnotatedWithTestFactory() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(WITH_TEST_FACTORY);
        // WHEN
        Optional<String> summaryOpt = xrayTestMetadataReader.getSummary(testIdentifier);
        // THEN
        assertThat(summaryOpt)
                .contains("withTestFactory()");
    }

    @Test
    void shouldReturnDescriptionFromXrayTestAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_ANNOTATED);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .contains("some description");
    }

    @Test
    void shouldReturnOptionalEmptyFromXrayTestAnnotationWithEmptyDescription() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_EMPTY_ANNOTATION);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyDescriptionWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(NOT_ANNOTATED);
        // WHEN
        Optional<String> descriptionOpt = xrayTestMetadataReader.getDescription(testIdentifier);
        // THEN
        assertThat(descriptionOpt)
                .isEmpty();
    }

    @Test
    void shouldReturnRequirementsFromRequirementsAnnotation() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_ANNOTATED);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .containsExactly("REQ-1", "REQ-2");
    }

    @Test
    void shouldReturnEmptyListFromRequirementsAnnotationWithEmptyArray() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(XRAYTEST_EMPTY_ANNOTATION);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyListOfRequirementsWhenMethodNotAnnotated() {
        // GIVEN
        TestIdentifier testIdentifier = TestIdentifier.from(NOT_ANNOTATED);
        // WHEN
        List<String> requirements = xrayTestMetadataReader.getRequirements(testIdentifier);
        // THEN
        assertThat(requirements)
                .isEmpty();
    }
}