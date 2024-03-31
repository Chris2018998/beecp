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

public class Case5_ConnectionAliveSqlTest extends TestCase {

    public void testOnSetAndGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setAliveTestSql(null);
        if (config.getAliveTestSql() == null) throw new TestException();

        config.setAliveTestSql("SELECT1");
        if (!"SELECT1".equals(config.getAliveTestSql())) throw new TestException();
    }

    public void testOnInvalidSql() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);

        config.setAliveTestSql("SELECT1");
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            if (!TestUtil.containsMessage(e, "Alive test sql must be start with 'select '"))
                throw new TestException();
        }
    }
}
