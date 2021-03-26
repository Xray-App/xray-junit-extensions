package com.xpandit.xray.junit.customjunitxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collection;

import com.xpandit.xray.junit.customjunitxml.annotations.Requirement;
import com.xpandit.xray.junit.customjunitxml.annotations.XrayTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DynamicTest;

@ExtendWith(XrayTestReporterParameterResolver.class)
public class XrayEnabledTestExamples {

    @Test
    public void legacyTest() {
    }

    @Test
    public void testWithTestRunComments(XrayTestReporter xrayReporter) {
        xrayReporter.addComment("hello");
        xrayReporter.addComment("world");
    }

    @Test
    public void testWithTestRunCustomFields(XrayTestReporter xrayReporter) {
        xrayReporter.setTestRunCustomField("cf1", "field1_value");
        xrayReporter.setTestRunCustomField("cf2", "field2_value");
    }

    @Test
    public void testWithTestRunEvidence(XrayTestReporter xrayReporter) {
        xrayReporter.addTestRunEvidence(FileSystems.getDefault().getPath("src/test/java/com/xpandit/xray/junit/customjunitxml/xray.png").toAbsolutePath().toString());
    }

    @Test
    @XrayTest(id = "myCustomId")
    public void annotatedXrayTestWithCustomId() {
        fail("this should have id: myCustomId");
    }

    @Test
    @XrayTest(key = "CALC-100")
    public void annotatedXrayTestWithKey() {
        //
    }
    
    @Test
    @XrayTest(summary = "custom summary")
    public void annotatedXrayTestWithSummary() {
        //
    }

    @Test
    @XrayTest(description = "custom description")
    public void annotatedXrayTestWithDescription() {
        //
    }

    @Test
    @Requirement("CALC-123")
    public void annotatedWithOneRequirement() {
        fail("this covering a requirement");
    }

    @Test
    @Requirement({"CALC-123", "CALC-124"})
    public void annotatedWithMultipleRequirements() {
        fail("this covering multiple requirements");
    }

    @Test
    @Tag("tag1")
    public void annotatedWithOneTag() {
        fail("tagged with one tag");
    }

    @Test
    @Tag("tag1")
    @Tag("tag2")
    public void annotatedWithMultipleTags() {
        fail("tagged with multiple tags");
    }

    @Test
    @DisplayName("custom name")
    public void annotatedWithDisplayName() {
        fail("with DisplayName");
    }

    @DisplayName("custom name")
    @ParameterizedTest
    @ValueSource(ints = {-1,0,5,-1001})
    public void parameterizedTestAnnotatedWithDisplayName(int number)
    {
        fail("with DisplayName");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1,0,5,-1001})
    public void parameterizedTestWithoutDisplayName(int number)
    {
        fail("without DisplayName");
    }

    @DisplayName("custom name")
    @RepeatedTest(3)
    void repeatedTestAnnotatedWithDisplayName() {
        fail("with DisplayName");
    }

    @RepeatedTest(3)
    void repeatedTestAnnotatedWithoutDisplayName() {
        fail("without DisplayName");
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsFromCollection() {
        return Arrays.asList(
            dynamicTest("1st dynamic test", () -> assertTrue(true))
        );
    }
}