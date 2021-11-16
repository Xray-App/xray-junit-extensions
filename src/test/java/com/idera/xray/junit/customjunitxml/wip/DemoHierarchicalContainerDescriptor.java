/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.idera.xray.junit.customjunitxml.wip;

import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.doNotSkip;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 1.0
 */
public class DemoHierarchicalContainerDescriptor extends AbstractTestDescriptor
		implements Node<DemoEngineExecutionContext> {

	private final Runnable beforeBlock;

	public DemoHierarchicalContainerDescriptor(UniqueId uniqueId, String displayName, TestSource source,
			Runnable beforeBlock) {
		super(uniqueId, displayName, source);
		this.beforeBlock = beforeBlock;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public boolean mayRegisterTests() {
		return true;
	}

	@Override
	public SkipResult shouldBeSkipped(DemoEngineExecutionContext context) {
		return doNotSkip();
	}

	@Override
	public DemoEngineExecutionContext before(DemoEngineExecutionContext context) {
		if (this.beforeBlock != null) {
			this.beforeBlock.run();
		}
		return context;
	}

}
