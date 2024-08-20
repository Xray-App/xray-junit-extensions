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

import java.util.List;
import java.util.Optional;

public interface XrayTestMetadataReader {

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

}
