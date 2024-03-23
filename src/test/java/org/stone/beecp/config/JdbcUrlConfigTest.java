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

public class JdbcUrlConfigTest extends TestCase {

    public void testOnNullUrl() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.check();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("jdbcUrl can't be null"))) throw new TestException();
        }
    }
}
