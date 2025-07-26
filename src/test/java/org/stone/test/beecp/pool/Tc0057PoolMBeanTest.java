/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.LogCollector;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.base.TestUtil.oclose;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0057PoolMBeanTest {
    @Test
    public void testJmxRegister() throws Exception {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setEnableJmx(true);
        config.setPoolName(poolName);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        String name1 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        String name2 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
        pool.close();
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
    }

    @Test
    public void testJmxBeanMethods() throws Exception {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setPoolName(poolName);
        config.setEnableJmx(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeDataSourceConfig config2 = createDefault();
        config2.setPoolName(poolName);
        config2.setEnableJmx(true);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assertions.assertEquals(poolName, pool2.getPoolName());

        String name1 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        String name2 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));

    }

    @Test
    public void testOnPrintRuntimeLog() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: not print
        LogCollector logCollector = LogCollector.startLogCollector();
        pool.setPrintRuntimeLog(false);//not print
        Connection con = null;
        try {
            con = pool.getConnection();
        } finally {
            oclose(con);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.isEmpty());

        //2: print runtime log
        pool.clear(false);//remove all connection

        logCollector = LogCollector.startLogCollector();
        pool.setPrintRuntimeLog(true);//print
        Assertions.assertTrue(pool.isPrintRuntimeLog());
        Connection con2 = null;
        try {
            con2 = pool.getConnection();
        } finally {
            oclose(con2);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.isEmpty());
    }
}


