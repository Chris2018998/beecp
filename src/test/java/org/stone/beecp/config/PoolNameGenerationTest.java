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

public class PoolNameGenerationTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        BeeDataSourceConfig checkConfig = config.check();
        if (!checkConfig.getPoolName().startsWith("FastPool-")) throw new TestException();

        BeeDataSourceConfig config2 = ConfigFactory.createDefault();
        config2.setPoolName("BeeCP-Pool");
        BeeDataSourceConfig checkConfig2 = config2.check();
        if (!"BeeCP-Pool".equals(checkConfig2.getPoolName())) throw new TestException();
    }
}
