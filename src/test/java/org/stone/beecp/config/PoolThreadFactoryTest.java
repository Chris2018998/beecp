/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.customization.DummyThreadFactory;

public class PoolThreadFactoryTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Class factClass = DummyThreadFactory.class;
        config.setThreadFactoryClass(factClass);
        if (!factClass.equals(config.getThreadFactoryClass())) throw new TestException();
        String factClassName = "org.stone.beecp.config.customization.DummyThreadFactory";
        config.setThreadFactoryClassName(factClassName);
        if (!factClassName.equals(config.getThreadFactoryClassName())) throw new TestException();
        DummyThreadFactory threadFactory = new DummyThreadFactory();
        config.setThreadFactory(threadFactory);
        if (threadFactory != config.getThreadFactory()) throw new TestException();
    }
}
