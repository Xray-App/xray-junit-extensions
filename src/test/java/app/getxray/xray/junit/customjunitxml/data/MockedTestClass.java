/*
 * Copyright 2024-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package app.getxray.xray.junit.customjunitxml.data;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestFactory;

@Disabled
public class MockedTestClass {

    @XrayTest(
            key = "ABC-123",
            id = "868",
            summary = "some summary",
            description = "some description"
    )
    @Requirement({"REQ-1", "REQ-2"})
    public void annotatedWithValues() {
        // it's a dummy test definition, no implementation needed
    }

    @XrayTest
    @Requirement({})
    public void annotatedWithEmptyValues() {
        // it's a dummy test definition, no implementation needed
    }

    public void notAnnotatedTest() {
        // it's a dummy test definition, no implementation needed
    }

    @DisplayName("display name")
    public void annotatedWithDisplayName() {
        // it's a dummy test definition, no implementation needed
    }

    @XrayTest(summary = "xray summary")
    @DisplayName("display name")
    public void annotatedWithXrayTestAndDisplayName() {
        // it's a dummy test definition, no implementation needed
    }

    @TestFactory
    public void withTestFactory() {
        // it's a dummy test definition, no implementation needed
    }
}
