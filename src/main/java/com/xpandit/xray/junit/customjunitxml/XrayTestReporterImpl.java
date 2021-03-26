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

import org.junit.jupiter.api.extension.ExtensionContext;

public class XrayTestReporterImpl implements XrayTestReporter {

    private final ExtensionContext extensionContext;

    public XrayTestReporterImpl(ExtensionContext extensionContext) {
        this.extensionContext = extensionContext;
    }

    public void addComment(String comment) {
        this.extensionContext.publishReportEntry(TESTRUN_COMMENT, comment);
    }

    public void setTestRunCustomField(String field, String value) {
        this.extensionContext.publishReportEntry(TESTRUN_CUSTOMFIELD_PREFIX + field, value);
    }

    public void addTestRunEvidence(String filepath) {
        this.extensionContext.publishReportEntry(TESTRUN_EVIDENCE, filepath);
    }
}
