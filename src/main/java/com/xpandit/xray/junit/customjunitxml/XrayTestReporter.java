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

    static final String XRAY_PREFIX = "xray:";
    static final String TESTRUN_COMMENT = "xray:comment";
    static final String TESTRUN_EVIDENCE = "xray:evidence";
    static final String TESTRUN_CUSTOMFIELD_PREFIX = "xray:testrun_customfield:";

    public void addComment(String comment);

    public void setTestRunCustomField(String field, String value);

    public void addTestRunEvidence(String filepath);
}
