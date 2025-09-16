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

import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;

import java.util.List;
import java.util.Optional;

public interface XrayTestMetadataReader {

    /**
     * Extracts the test case name from the provided TestIdentifier.
     * <p>
     * This method parses the legacy reporting name, typically in the format "testMethod(params)",
     * and returns only the test method name. This is useful for generating concise test names
     * for reporting purposes.
     * Override it to customize how test names are extracted or formatted, for example, to support
     * alternative naming conventions, add prefixes/suffixes, or adapt to specific reporting
     * requirements in your organization.
     * </p>
     *
     * @param testIdentifier test identifier
     * @return test case name generated from the provided TestIdentifier.
     */
    default String getName(TestIdentifier testIdentifier) {
        String legacyName = testIdentifier.getLegacyReportingName();
        int pos = legacyName.indexOf('(');
        if (pos > 0) {
            return legacyName.substring(0, pos);
        } else {
            return legacyName;
        }
    }

    /**
     * Retrieves the fully qualified class name associated with the given TestIdentifier.
     * <p>
     * This method delegates to LegacyReportingUtils to obtain the class name from the test plan,
     * ensuring compatibility with JUnit Platform's reporting conventions.
     * Override it to customize how class names are extracted or formatted, for example, to support
     * alternative naming conventions, add prefixes/suffixes, or adapt to specific reporting
     * requirements in your organization.
     * </p>
     *
     * @param testIdentifier test identifier
     * @param testPlan the JUnit test structure
     * @return the fully qualified class name for the test
     */
    default String getClassName(TestIdentifier testIdentifier, TestPlan testPlan) {
        return LegacyReportingUtils.getClassName(testPlan, testIdentifier);
    }

    /**
     * @param testIdentifier test identifier
     * @return test id if any or empty
     */
    Optional<String> getId(TestIdentifier testIdentifier);

    /**
     * @param testIdentifier test identifier
     * @return test key if any
     */
    Optional<String> getKey(TestIdentifier testIdentifier);

    /**
     * @param testIdentifier test identifier
     * @return test summary if any
     */
    Optional<String> getSummary(TestIdentifier testIdentifier);

    /**
     * @param testIdentifier test identifier
     * @return test description if any
     */
    Optional<String> getDescription(TestIdentifier testIdentifier);

    /**
     * @param testIdentifier test identifier
     * @return Unmodifiable list of requirements
     */
    List<String> getRequirements(TestIdentifier testIdentifier);

    /**
     * @param testIdentifier test identifier
     * @return Unmodifiable list of tags
     */
    List<String> getTags(TestIdentifier testIdentifier);
}
