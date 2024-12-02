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
        Assert.assertNotEquals(0, config.getBorrowSemaphoreSize());
        config.setBorrowSemaphoreSize(5);
        Assert.assertEquals(5, config.getBorrowSemaphoreSize());

        //maxWait
        config.setMaxWait(0L);
        Assert.assertNotEquals(0, config.getMaxWait());
        config.setMaxWait(5000L);
        Assert.assertEquals(5000L, config.getMaxWait());

        //idleTimeout
        config.setIdleTimeout(0);
        Assert.assertNotEquals(0, config.getIdleTimeout());
        config.setIdleTimeout(3000);
        Assert.assertEquals(3000, config.getIdleTimeout());

        //holdTimeout
        config.setHoldTimeout(-1);
        Assert.assertNotEquals(-1, config.getHoldTimeout());
        config.setHoldTimeout(0);
        Assert.assertEquals(0, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assert.assertEquals(3000L, config.getHoldTimeout());

        //aliveTestTimeout
        config.setAliveTestTimeout(-1);
        Assert.assertNotEquals(-1, config.getAliveTestTimeout());
        config.setAliveTestTimeout(0);
        Assert.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assert.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        config.setAliveAssumeTime(-1);
        Assert.assertNotEquals(-1, config.getAliveAssumeTime());
        config.setAliveAssumeTime(0);
        Assert.assertEquals(0, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        config.setTimerCheckInterval(0);
        Assert.assertNotEquals(0, config.getTimerCheckInterval());
        config.setTimerCheckInterval(3000);
        Assert.assertEquals(3000, config.getTimerCheckInterval());

        //forceCloseUsingOnClear
        config.setForceCloseUsingOnClear(true);
        Assert.assertTrue(config.isForceCloseUsingOnClear());

        //delayTimeForNextClear
        config.setParkTimeForRetry(-1);
        Assert.assertNotEquals(-1, config.getParkTimeForRetry());
        config.setParkTimeForRetry(0);
        Assert.assertEquals(0L, config.getParkTimeForRetry());
        config.setParkTimeForRetry(3000L);
        Assert.assertEquals(3000L, config.getParkTimeForRetry());

        //defaultCatalog
        config.setDefaultCatalog(null);
        Assert.assertNull(config.getDefaultCatalog());
        config.setDefaultCatalog("catalog");
        Assert.assertEquals("catalog", config.getDefaultCatalog());

        //defaultSchema
        config.setDefaultSchema(null);
        Assert.assertNull(config.getDefaultSchema());
        config.setDefaultSchema("schema");
        Assert.assertEquals("schema", config.getDefaultSchema());

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
        Assert.assertEquals("org.stone.beecp.pool.FastConnectionPool", config.getPoolImplementClassName());
    }
}
