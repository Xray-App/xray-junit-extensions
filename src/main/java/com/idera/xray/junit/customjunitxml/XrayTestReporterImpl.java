/*
 * Copyright 2021-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html"
 */

package com.idera.xray.junit.customjunitxml;

import java.util.List;
import java.util.ArrayList;

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

    public void setTestRunCustomField(String field, String[] values) {
        ArrayList<String> arrList = new ArrayList<String>();
        for (int i=0;i<values.length;i++) {
            String value = values[i];
            value = value.replaceAll("\\\\", "\\\\\\\\").replaceAll(";", "\\\\;");
            arrList.add(value);
        } 
        String serializedValue =String.join(";", (String [])(arrList.toArray(new String[0])) );
        this.extensionContext.publishReportEntry(TESTRUN_CUSTOMFIELD_PREFIX + field, serializedValue);
    }

    public void addTestRunEvidence(String filepath) {
        this.extensionContext.publishReportEntry(TESTRUN_EVIDENCE, filepath);
    }
}
