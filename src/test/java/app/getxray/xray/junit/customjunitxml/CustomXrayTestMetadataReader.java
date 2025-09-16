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

import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CustomXrayTestMetadataReader implements XrayTestMetadataReader {
    @Override
    public String getName(TestIdentifier testIdentifier) {
        return "NAME:" + XrayTestMetadataReader.super.getName(testIdentifier);
    }

    @Override
    public String getClassName(TestIdentifier testIdentifier, TestPlan testPlan) {
        return "CLASSNAME:" + XrayTestMetadataReader.super.getClassName(testIdentifier, testPlan);
    }

    @Override
    public Optional<String> getId(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaMethod)
                .map(Method::getName)
                .map(a -> "ID:" + a);
    }

    @Override
    public Optional<String> getKey(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaMethod)
                .map(Method::getName)
                .map(a -> "KEY:" + a);
    }

    @Override
    public Optional<String> getSummary(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaMethod)
                .map(Method::getName)
                .map(a -> "SUMMARY:" + a);
    }

    @Override
    public Optional<String> getDescription(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaMethod)
                .map(Method::getName)
                .map(a -> "DESCRIPTION:" + a);
    }

    @Override
    public List<String> getRequirements(TestIdentifier testIdentifier) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTags(TestIdentifier testIdentifier) {
        return Collections.emptyList();
    }
}
