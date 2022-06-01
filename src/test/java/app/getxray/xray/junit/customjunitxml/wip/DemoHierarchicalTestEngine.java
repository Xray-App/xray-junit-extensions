/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package app.getxray.xray.junit.customjunitxml.wip;

import java.util.function.Function;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 1.0
 */
public final class DemoHierarchicalTestEngine extends HierarchicalTestEngine<DemoEngineExecutionContext> {

    private final String engineId;
    private final DemoHierarchicalEngineDescriptor engineDescriptor;

    public DemoHierarchicalTestEngine() {
        this("dummy");
    }

    public DemoHierarchicalTestEngine(String engineId) {
        this.engineId = engineId;
        this.engineDescriptor = new DemoHierarchicalEngineDescriptor(UniqueId.forEngine(getId()));
    }

    @Override
    public String getId() {
        return engineId;
    }

    public DemoHierarchicalEngineDescriptor getEngineDescriptor() {
        return engineDescriptor;
    }

    public DemoHierarchicalTestDescriptor addTest(String uniqueName, Runnable executeBlock) {
        return addTest(uniqueName, uniqueName, executeBlock);
    }

    public DemoHierarchicalTestDescriptor addTest(String uniqueName, String displayName, Runnable executeBlock) {
        return addChild(uniqueName, uniqueId -> new DemoHierarchicalTestDescriptor(uniqueId, displayName, executeBlock),
                "test");
    }

    public DemoHierarchicalContainerDescriptor addContainer(String uniqueName, String displayName, TestSource source) {
        return addContainer(uniqueName, displayName, source, null);
    }

    public DemoHierarchicalContainerDescriptor addContainer(String uniqueName, Runnable beforeBlock) {
        return addContainer(uniqueName, uniqueName, null, beforeBlock);
    }

    public DemoHierarchicalContainerDescriptor addContainer(String uniqueName, String displayName, TestSource source,
            Runnable beforeBlock) {

        return addChild(uniqueName,
                uniqueId -> new DemoHierarchicalContainerDescriptor(uniqueId, displayName, source, beforeBlock),
                "container");
    }

    public <T extends TestDescriptor & Node<DemoEngineExecutionContext>> T addChild(String uniqueName,
            Function<UniqueId, T> creator, String segmentType) {

        UniqueId uniqueId = engineDescriptor.getUniqueId().append(segmentType, uniqueName);
        T child = creator.apply(uniqueId);
        engineDescriptor.addChild(child);
        return child;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        return engineDescriptor;
    }

    @Override
    protected DemoEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new DemoEngineExecutionContext();
    }

}