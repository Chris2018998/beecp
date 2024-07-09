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
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0007ConfigSetAndGetTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = createEmpty();

        //fairMode
        Assert.assertFalse(config.isFairMode());
        config.setFairMode(true);
        Assert.assertTrue(config.isFairMode());

        //asyncCreateInitConnection
        Assert.assertFalse(config.isAsyncCreateInitConnection());
        config.setAsyncCreateInitConnection(true);
        Assert.assertTrue(config.isAsyncCreateInitConnection());

        //borrowSemaphoreSize
        config.setBorrowSemaphoreSize(0);
        Assert.assertNotEquals(config.getBorrowSemaphoreSize(), 0);
        config.setBorrowSemaphoreSize(5);
        Assert.assertEquals(config.getBorrowSemaphoreSize(), 5);

        //maxWait
        config.setMaxWait(0L);
        Assert.assertNotEquals(config.getMaxWait(), 0);
        config.setMaxWait(5000L);
        Assert.assertEquals(config.getMaxWait(), 5000L);

//        //connectTimeout
//        config.setCreateTimeout(-1);
//        Assert.assertNotEquals(config.getCreateTimeout(), -1);
//        config.setCreateTimeout(0);
//        Assert.assertEquals(config.getCreateTimeout(), 0);
//        config.setCreateTimeout(3);
//        Assert.assertEquals(config.getCreateTimeout(), 3);

        //idleTimeout
        config.setIdleTimeout(0);
        Assert.assertNotEquals(config.getIdleTimeout(), 0);
        config.setIdleTimeout(3000);
        Assert.assertEquals(config.getIdleTimeout(), 3000);

        //holdTimeout
        config.setHoldTimeout(-1);
        Assert.assertNotEquals(config.getHoldTimeout(), -1);
        config.setHoldTimeout(0);
        Assert.assertEquals(config.getHoldTimeout(), 0);
        config.setHoldTimeout(3000L);
        Assert.assertEquals(config.getHoldTimeout(), 3000L);

        //aliveTestTimeout
        config.setAliveTestTimeout(-1);
        Assert.assertNotEquals(config.getAliveTestTimeout(), -1);
        config.setAliveTestTimeout(0);
        Assert.assertEquals(config.getAliveTestTimeout(), 0);
        config.setAliveTestTimeout(3);
        Assert.assertEquals(config.getAliveTestTimeout(), 3);

        //aliveAssumeTime
        config.setAliveAssumeTime(-1);
        Assert.assertNotEquals(config.getAliveAssumeTime(), -1);
        config.setAliveAssumeTime(0);
        Assert.assertEquals(config.getAliveAssumeTime(), 0);
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(config.getAliveAssumeTime(), 3000L);

        //timerCheckInterval
        config.setTimerCheckInterval(0);
        Assert.assertNotEquals(config.getTimerCheckInterval(), 0);
        config.setTimerCheckInterval(3000);
        Assert.assertEquals(config.getTimerCheckInterval(), 3000);

        //forceCloseUsingOnClear
        config.setForceCloseUsingOnClear(true);
        Assert.assertTrue(config.isForceCloseUsingOnClear());

        //delayTimeForNextClear
        config.setDelayTimeForNextClear(-1);
        Assert.assertNotEquals(config.getDelayTimeForNextClear(), -1);
        config.setDelayTimeForNextClear(0);
        Assert.assertEquals(config.getDelayTimeForNextClear(), 0L);
        config.setDelayTimeForNextClear(3000L);
        Assert.assertEquals(config.getDelayTimeForNextClear(), 3000L);

        //defaultCatalog
        config.setDefaultCatalog(null);
        Assert.assertNull(config.getDefaultCatalog());
        config.setDefaultCatalog("catalog");
        Assert.assertEquals(config.getDefaultCatalog(), "catalog");

        //defaultSchema
        config.setDefaultSchema(null);
        Assert.assertNull(config.getDefaultSchema());
        config.setDefaultSchema("schema");
        Assert.assertEquals(config.getDefaultSchema(), "schema");

        //defaultReadOnly
        config.setDefaultReadOnly(false);
        Assert.assertFalse(config.isDefaultReadOnly());
        config.setDefaultReadOnly(true);
        Assert.assertTrue(config.isDefaultReadOnly());

        //defaultAutoCommit
        config.setDefaultAutoCommit(false);
        Assert.assertFalse(config.isDefaultAutoCommit());
        config.setDefaultAutoCommit(true);
        Assert.assertTrue(config.isDefaultAutoCommit());

        //enableDefaultOnCatalog
        config.setEnableDefaultOnCatalog(false);
        Assert.assertFalse(config.isEnableDefaultOnCatalog());

        //enableDefaultOnSchema
        config.setEnableDefaultOnSchema(false);
        Assert.assertFalse(config.isEnableDefaultOnSchema());

        //enableDefaultOnReadOnly
        config.setEnableDefaultOnReadOnly(false);
        Assert.assertFalse(config.isEnableDefaultOnReadOnly());

        //enableDefaultOnReadOnly
        config.setEnableDefaultOnAutoCommit(false);
        Assert.assertFalse(config.isEnableDefaultOnAutoCommit());

        //enableDefaultOnTransactionIsolation
        config.setEnableDefaultOnTransactionIsolation(false);
        Assert.assertFalse(config.isEnableDefaultOnTransactionIsolation());

        //forceDirtyOnSchemaAfterSet
        Assert.assertFalse(config.isForceDirtyOnSchemaAfterSet());
        config.setForceDirtyOnSchemaAfterSet(true);
        Assert.assertTrue(config.isForceDirtyOnSchemaAfterSet());

        //forceDirtyOnCatalogAfterSet
        Assert.assertFalse(config.isForceDirtyOnCatalogAfterSet());
        config.setForceDirtyOnCatalogAfterSet(true);
        Assert.assertTrue(config.isForceDirtyOnCatalogAfterSet());

        //enableJmx
        config.setEnableJmx(true);
        Assert.assertTrue(config.isEnableJmx());

        //printConfigInfo
        config.setPrintConfigInfo(true);
        Assert.assertTrue(config.isPrintConfigInfo());

        //printRuntimeLog
        config.setPrintRuntimeLog(true);
        Assert.assertTrue(config.isPrintRuntimeLog());

        //printRuntimeLog
        config.setEnableThreadLocal(true);
        Assert.assertTrue(config.isEnableThreadLocal());

        //poolImplementClassName
        config.setPoolImplementClassName(null);
        Assert.assertNotNull(config.getPoolImplementClassName());
        config.setPoolImplementClassName("org.stone.beecp.pool.FastConnectionPool");
        Assert.assertEquals(config.getPoolImplementClassName(), "org.stone.beecp.pool.FastConnectionPool");
    }
}
