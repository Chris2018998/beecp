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
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.JdbcConfig;

public class Case4_ConnectionSizeTest extends TestCase {

    public void testOnSetAndGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        final int initialSize = config.getInitialSize();
        final int maxActiveSize = config.getMaxActive();

        config.setInitialSize(-1);
        config.setMaxActive(-1);
        if (initialSize != config.getInitialSize()) throw new TestException();
        if (maxActiveSize != config.getMaxActive()) throw new TestException();

        config.setInitialSize(0);
        config.setMaxActive(0);
        if (0 != config.getInitialSize()) throw new TestException();
        if (0 == config.getMaxActive()) throw new TestException();

        config.setInitialSize(10);
        config.setMaxActive(20);
        if (10 != config.getInitialSize()) throw new TestException();
        if (20 != config.getMaxActive()) throw new TestException();
    }

    public void testOnErrorInitialSize() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setMaxActive(5);
        config.setInitialSize(10);

        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            if (!TestUtil.containsMessage(e, "initialSize must not be greater than maxActive"))
                throw new TestException();
        }
    }
}
