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

public interface XrayTestReporter {

    String XRAY_PREFIX = "xray:";
    String TESTRUN_COMMENT = "xray:comment";
    String TESTRUN_EVIDENCE = "xray:evidence";
    String TESTRUN_CUSTOMFIELD_PREFIX = "xray:testrun_customfield:";

    void addComment(String comment);

    void setTestRunCustomField(String field, String value);

    void addTestRunEvidence(String filepath);
}
