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

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.UriSource;
import org.junit.platform.launcher.TestIdentifier;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DefaultXrayTestMetadataReader implements XrayTestMetadataReader {

    @Override
    public Optional<String> getId(TestIdentifier testIdentifier) {
        Optional<String> id = getTestMethodAnnotation(testIdentifier, XrayTest.class)
                .map(XrayTest::id)
                .filter(s -> !s.isEmpty());
        if (id.isPresent()) {
            return id;
        }

        Optional<String> uriSourceId = readQueryValue(testIdentifier, XrayURI.QUERY_ID);
        if (uriSourceId.isPresent()) {
            return uriSourceId;
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getKey(TestIdentifier testIdentifier) {
        Optional<String> key = getTestMethodAnnotation(testIdentifier, XrayTest.class)
                .map(XrayTest::key)
                .filter(s -> !s.isEmpty());
        if (key.isPresent()) {
            return key;
        }

        Optional<String> uriSourceKey = readQueryValue(testIdentifier, XrayURI.QUERY_KEY);
        if (uriSourceKey.isPresent()) {
            return uriSourceKey;
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getSummary(TestIdentifier testIdentifier) {
        Optional<String> testSummary = getTestMethodAnnotation(testIdentifier, XrayTest.class)
                .map(XrayTest::summary)
                .filter(s -> !s.isEmpty());
        if (testSummary.isPresent()) {
            return testSummary;
        }

        Optional<DisplayName> displayName = getTestMethodAnnotation(testIdentifier, DisplayName.class);
        if (displayName.isPresent()) {
            return Optional.of(displayName.get().value());
        }


        Optional<TestFactory> dynamicTest = getTestMethodAnnotation(testIdentifier, TestFactory.class);
        Optional<DisplayNameGeneration> displayNameGenerator = getTestClassAnnotation(testIdentifier, DisplayNameGeneration.class);
        if (dynamicTest.isPresent() || displayNameGenerator.isPresent()) {
            return Optional.of(testIdentifier.getDisplayName());
        }

        Optional<String> uriSourceSummary = readQueryValue(testIdentifier, XrayURI.QUERY_SUMMARY);
        if (uriSourceSummary.isPresent()) {
            return uriSourceSummary;
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getDescription(TestIdentifier testIdentifier) {
        Optional<String> description = getTestMethodAnnotation(testIdentifier, XrayTest.class)
                .map(XrayTest::description)
                .filter(s -> !s.isEmpty());
        if (description.isPresent()) {
            return description;
        }

        Optional<String> uriSourceDescription = readQueryValue(testIdentifier, XrayURI.QUERY_DESCRIPTION);
        if (uriSourceDescription.isPresent()) {
            return uriSourceDescription;
        }

        return Optional.empty();
    }

    @Override
    public List<String> getRequirements(TestIdentifier testIdentifier) {
        return getTestMethodAnnotation(testIdentifier, Requirement.class)
                .map(Requirement::value)
                .map(arr -> Collections.unmodifiableList(Arrays.asList(arr)))
                .orElse(Collections.emptyList());
    }

    protected <A extends Annotation> Optional<A> getTestMethodAnnotation(TestIdentifier testIdentifier, Class<A> aClass) {
        return testIdentifier.getSource()
                .filter(a -> a instanceof MethodSource)
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaMethod)
                .flatMap(a -> AnnotationSupport.findAnnotation(a, aClass));
    }

    protected <A extends Annotation> Optional<A> getTestClassAnnotation(TestIdentifier testIdentifier, Class<A> aClass) {
        return testIdentifier.getSource()
                .filter(a -> a instanceof MethodSource)
                .map(MethodSource.class::cast)
                .map(MethodSource::getJavaClass)
                .flatMap(a -> AnnotationSupport.findAnnotation(a, aClass));
    }

    private Optional<String> readQueryValue(TestIdentifier testIdentifier, String key) {
        return testIdentifier.getSource()
                .filter(UriSource.class::isInstance)
                .map(UriSource.class::cast)
                .map(UriSource::getUri)
                .flatMap(x -> XrayURI.readQueryValue(x, key));
    }
}
