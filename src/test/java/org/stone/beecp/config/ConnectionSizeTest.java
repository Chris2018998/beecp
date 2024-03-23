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
import org.stone.beecp.BeeDataSourceConfigException;

public class ConnectionSizeTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(10);
        config.setMaxActive(5);
        try {
            config.check();//check (initialSize > maxActive)
        } catch (BeeDataSourceConfigException e) {
            if (!"initialSize must not be greater than maxActive".equals(e.getMessage()))
                throw new TestException();
        }
    }
}
