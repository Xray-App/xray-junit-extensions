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

import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

@Disabled
@DisplayNameGeneration(MockedTestWithDisplayNameGeneratorClass.CustomGenerator.class)
public class MockedTestWithDisplayNameGeneratorClass {

    public static class CustomGenerator implements DisplayNameGenerator {
        @Override
        public String generateDisplayNameForClass(Class<?> testClass) {
            return "DisplayNameForClass";
        }

        @Override
        public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
            return "DisplayNameForNestedClass";
        }

        @Override
        public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
            return "DisplayNameForMethod";
        }
    }

    @XrayTest()
    public void withDisplayNameGenerator() {
        // it's a dummy test definition, no implementation needed
    }

}
