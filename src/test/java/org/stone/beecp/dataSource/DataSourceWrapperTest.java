/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DataSourceWrapperTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testDataSourceWrapper() throws Exception {
        if (!ds.isWrapperFor(DataSource.class)) throw new TestException();
        if (ds.unwrap(DataSource.class) != ds) throw new TestException();

        try {
            ds.unwrap(String.class);
            throw new TestException();
        } catch (SQLException e) {
            //do nothing
        }
    }
}
