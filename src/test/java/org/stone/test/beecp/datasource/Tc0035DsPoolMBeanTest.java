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
import org.stone.beecp.pool.FastConnectionPoolMBean;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.objects.BeeCPHello;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0035DsPoolMBeanTest {
    @Test
    public void testRegisterSuccess() throws Exception {
        int semaphoreSize = 5;
        int maxSize = 10;
        int initialSize = 5;

        BeeDataSourceConfig config = createDefault();
        config.setRegisterMbeans(false);
        String poolName = "JMX-POOL";
        config.setPoolName(poolName);
        config.setMaxActive(maxSize);
        config.setInitialSize(initialSize);
        config.setSemaphoreSize(semaphoreSize);

        String name1 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        String name2 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
            Object dsPool = getFieldValue(ds, "pool");
            Assertions.assertInstanceOf(FastConnectionPoolMBean.class, dsPool);
            FastConnectionPoolMBean mbean = (FastConnectionPoolMBean) dsPool;

            Assertions.assertEquals(poolName, mbean.getPoolName());
            Assertions.assertEquals(semaphoreSize, mbean.getSemaphoreSize());
            Assertions.assertEquals(0, mbean.getSemaphoreAcquiredSize());
            Assertions.assertEquals(0, mbean.getSemaphoreWaitingSize());
            Assertions.assertEquals(0, mbean.getTransferWaitingSize());
            Assertions.assertEquals(maxSize, mbean.getMaxSize());
            Assertions.assertEquals(initialSize, mbean.getIdleSize());
            Assertions.assertEquals(0, mbean.getBorrowedSize());
            Assertions.assertFalse(mbean.isPrintRuntimeLog());
            mbean.setPrintRuntimeLog(true);
            Assertions.assertTrue(mbean.isPrintRuntimeLog());
        }

        config.setRegisterMbeans(true);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
        }
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assertions.assertFalse(mBeanServer.isRegistered(jmxRegName2));
    }

    @Test
    public void testRegisterFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setPrintRuntimeLogs(true);
        config.setRegisterMbeans(true);
        String poolName = "JMX-POOL";
        config.setPoolName(poolName);

        String name1 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        String name2 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            Assertions.assertNotNull(mBeanServer.registerMBean(new BeeCPHello(), jmxRegName1));
            Assertions.assertNotNull(mBeanServer.registerMBean(new BeeCPHello(), jmxRegName2));

            LogCollector logCollector = LogCollector.startLogCollector();
            try (BeeDataSource ignored = new BeeDataSource(config)) {
                Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
                Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));
            }
            String logs = logCollector.endLogCollector();
            String msg1 = "BeeCP(" + poolName + ")failed to register jmx-bean:" + name1;
            String msg2 = "BeeCP(" + poolName + ")failed to register jmx-bean:" + name2;
            Assertions.assertTrue(logs.contains(msg1));
            Assertions.assertTrue(logs.contains(msg2));

        } finally {
            if (mBeanServer.isRegistered(jmxRegName1))
                mBeanServer.unregisterMBean(jmxRegName1);
            if (mBeanServer.isRegistered(jmxRegName2))
                mBeanServer.unregisterMBean(jmxRegName2);
        }
    }

    @Test
    public void testUnRegisterFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setPrintRuntimeLogs(true);
        config.setRegisterMbeans(true);
        String poolName = "JMX-POOL";
        config.setPoolName(poolName);

        String name1 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        String name2 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName1));
            Assertions.assertTrue(mBeanServer.isRegistered(jmxRegName2));

            mBeanServer.unregisterMBean(jmxRegName1);
            mBeanServer.unregisterMBean(jmxRegName2);
        }
        String logs = logCollector.endLogCollector();
        String msg1 = "BeeCP(" + poolName + ")failed to unregister jmx-bean:" + name1;
        String msg2 = "BeeCP(" + poolName + ")failed to unregister jmx-bean:" + name2;
        Assertions.assertTrue(logs.contains(msg1));
        Assertions.assertTrue(logs.contains(msg2));
    }
}
