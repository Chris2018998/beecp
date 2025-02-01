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

import java.security.InvalidParameterException;

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
        try {
            config.setBorrowSemaphoreSize(-1);
            fail("Semaphore size test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Max permit size of semaphore must be greater than zero", e.getMessage());
        }
        try {
            config.setBorrowSemaphoreSize(0);
            fail("Semaphore size test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Max permit size of semaphore must be greater than zero", e.getMessage());
        }
        config.setBorrowSemaphoreSize(1);
        Assert.assertEquals(1, config.getBorrowSemaphoreSize());

        //maxWait
        try {
            config.setMaxWait(-1L);
            fail("Max wait time test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Max wait time must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxWait(0L);
            fail("Max wait time test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Max wait time must be greater than zero", e.getMessage());
        }
        config.setMaxWait(5000L);
        Assert.assertEquals(5000L, config.getMaxWait());

        //idleTimeout
        try {
            config.setIdleTimeout(-1L);
            fail("Idle Timeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Idle timeout must be greater than zero", e.getMessage());
        }
        try {
            config.setIdleTimeout(0L);
            fail("Idle Timeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Idle timeout must be greater than zero", e.getMessage());
        }
        config.setIdleTimeout(3000);
        Assert.assertEquals(3000, config.getIdleTimeout());

        //holdTimeout
        try {
            config.setHoldTimeout(-1L);
            fail("Max hold time test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Max hold time can't be less than zero", e.getMessage());
        }
        config.setHoldTimeout(0L);
        Assert.assertEquals(0L, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assert.assertEquals(3000L, config.getHoldTimeout());


        //aliveTestTimeout
        try {
            config.setAliveTestTimeout(-1);
            fail("AliveTestTimeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Timeout of alive test can't be less than zero", e.getMessage());
        }
        config.setAliveTestTimeout(0);
        Assert.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assert.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        try {
            config.setAliveAssumeTime(-1L);
            fail("AliveTestTimeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Alive assume time can't be less than zero", e.getMessage());
        }
        config.setAliveAssumeTime(0L);
        Assert.assertEquals(0L, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        try {
            config.setTimerCheckInterval(-1L);
            fail("AliveTestTimeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Interval Time of pool worker must be greater than zero", e.getMessage());
        }
        try {
            config.setTimerCheckInterval(0L);
            fail("AliveTestTimeout test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Interval Time of pool worker must be greater than zero", e.getMessage());
        }
        config.setTimerCheckInterval(3000L);
        Assert.assertEquals(3000L, config.getTimerCheckInterval());

        //forceCloseUsingOnClear
        config.setForceRecycleBorrowedOnClose(true);
        Assert.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        try {
            config.setParkTimeForRetry(-1);
            fail("ParkTimeForRetry test failed");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("Park time can't be less than zero", e.getMessage());
        }
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
