/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0037DsPoolWorkLogPrintTest {

    @Test
    public void testUpdateToConfig() throws SQLException {
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertFalse(ds.isPrintRuntimeLogs());
            ds.setPrintRuntimeLogs(true);
            Assertions.assertTrue(ds.isPrintRuntimeLogs());

            //lazy create pool
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
            ds.setForceRecycleBorrowedOnClose(true);
            LogCollector logCollector = LogCollector.startLogCollector();

            //print log
            Connection con = ds.getConnection();
            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledLogPrinter());
            String logContent = logCollector.endLogCollector();
            Assertions.assertTrue(logContent.contains("start to create a connection"));
            con.abort(null);

            //2: not print log
            ds.enableLogPrinter(false);
            Assertions.assertFalse(ds.getPoolMonitorVo().isEnabledLogPrinter());
            logCollector = LogCollector.startLogCollector();
            try (Connection ignored = ds.getConnection()) {
                logContent = logCollector.endLogCollector();
                Assertions.assertEquals("", logContent);
            }
        }
    }

    @Test
    public void testLogPrint() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(10);
        config.setForceRecycleBorrowedOnClose(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            //1: not print runtime log
            Assertions.assertFalse(ds.getPoolMonitorVo().isEnabledLogPrinter());
            LogCollector logCollector = LogCollector.startLogCollector();
            Connection con = ds.getConnection();
            String logContent = logCollector.endLogCollector();
            Assertions.assertEquals("", logContent);
            con.abort(null);

            //2: print runtime log
            ds.enableLogPrinter(true);
            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledLogPrinter());
            logCollector = LogCollector.startLogCollector();
            try (Connection ignored = ds.getConnection()) {
                logContent = logCollector.endLogCollector();
                Assertions.assertTrue(logContent.contains("start to create a connection"));
            }
        }
    }
}
