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

public class Case7_ConfigSetAndGetTest extends TestCase {

    public void testOnSetAndGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        //fairMode
        if (config.isFairMode()) throw new TestException();
        config.setFairMode(true);
        if (!config.isFairMode()) throw new TestException();

        //asyncCreateInitConnection
        if (config.isAsyncCreateInitConnection()) throw new TestException();
        config.setAsyncCreateInitConnection(true);
        if (!config.isAsyncCreateInitConnection()) throw new TestException();

        //borrowSemaphoreSize
        config.setBorrowSemaphoreSize(0);
        if (config.getBorrowSemaphoreSize() == 0) throw new TestException();
        config.setBorrowSemaphoreSize(5);
        if (config.getBorrowSemaphoreSize() != 5) throw new TestException();

        //maxWait
        config.setMaxWait(0L);
        if (config.getMaxWait() == 0) throw new TestException();
        config.setMaxWait(5000L);
        if (config.getMaxWait() != 5000L) throw new TestException();

        //connectTimeout
        config.setConnectTimeout(-1);
        if (config.getConnectTimeout() == -1) throw new TestException();
        config.setConnectTimeout(0);
        if (config.getConnectTimeout() != 0) throw new TestException();
        config.setConnectTimeout(3);
        if (config.getConnectTimeout() != 3) throw new TestException();

        //idleTimeout
        config.setIdleTimeout(0);
        if (config.getIdleTimeout() == 0) throw new TestException();
        config.setIdleTimeout(3000);
        if (config.getIdleTimeout() != 3000) throw new TestException();

        //holdTimeout
        config.setHoldTimeout(-1);
        if (config.getHoldTimeout() == -1) throw new TestException();
        config.setHoldTimeout(0);
        if (config.getHoldTimeout() != 0) throw new TestException();
        config.setHoldTimeout(3000L);
        if (config.getHoldTimeout() != 3000L) throw new TestException();

        //aliveTestTimeout
        config.setAliveTestTimeout(-1);
        if (config.getAliveTestTimeout() == -1) throw new TestException();
        config.setAliveTestTimeout(0);
        if (config.getAliveTestTimeout() != 0) throw new TestException();
        config.setAliveTestTimeout(3);
        if (config.getAliveTestTimeout() != 3) throw new TestException();

        //aliveAssumeTime
        config.setAliveAssumeTime(-1);
        if (config.getAliveAssumeTime() == -1) throw new TestException();
        config.setAliveAssumeTime(0);
        if (config.getAliveAssumeTime() != 0) throw new TestException();
        config.setAliveAssumeTime(3000L);
        if (config.getAliveAssumeTime() != 3000L) throw new TestException();

        //timerCheckInterval
        config.setTimerCheckInterval(0);
        if (config.getTimerCheckInterval() == 0) throw new TestException();
        config.setTimerCheckInterval(3000);
        if (config.getTimerCheckInterval() != 3000L) throw new TestException();

        //forceCloseUsingOnClear
        config.setForceCloseUsingOnClear(true);
        if (!config.isForceCloseUsingOnClear()) throw new TestException();

        //delayTimeForNextClear
        config.setDelayTimeForNextClear(-1);
        if (config.getDelayTimeForNextClear() == -1) throw new TestException();
        config.setDelayTimeForNextClear(0);
        if (config.getDelayTimeForNextClear() != 0) throw new TestException();
        config.setDelayTimeForNextClear(3000L);
        if (config.getDelayTimeForNextClear() != 3000L) throw new TestException();

        //defaultCatalog
        config.setDefaultCatalog(null);
        if (config.getDefaultCatalog() != null) throw new TestException();
        config.setDefaultCatalog("catlog");
        if (!"catlog".equals(config.getDefaultCatalog())) throw new TestException();

        //defaultSchema
        config.setDefaultSchema(null);
        if (config.getDefaultSchema() != null) throw new TestException();
        config.setDefaultSchema("schema");
        if (!"schema".equals(config.getDefaultSchema())) throw new TestException();

        //defaultReadOnly
        config.setDefaultReadOnly(false);
        if (config.isDefaultReadOnly()) throw new TestException();
        config.setDefaultReadOnly(true);
        if (!config.isDefaultReadOnly()) throw new TestException();

        //defaultAutoCommit
        config.setDefaultAutoCommit(false);
        if (config.isDefaultAutoCommit()) throw new TestException();
        config.setDefaultAutoCommit(true);
        if (!config.isDefaultAutoCommit()) throw new TestException();


        //enableDefaultOnCatalog
        config.setEnableDefaultOnCatalog(false);
        if (config.isEnableDefaultOnCatalog()) throw new TestException();

        //enableDefaultOnSchema
        config.setEnableDefaultOnSchema(false);
        if (config.isEnableDefaultOnSchema()) throw new TestException();

        //enableDefaultOnReadOnly
        config.setEnableDefaultOnReadOnly(false);
        if (config.isEnableDefaultOnReadOnly()) throw new TestException();

        //enableDefaultOnReadOnly
        config.setEnableDefaultOnAutoCommit(false);
        if (config.isEnableDefaultOnAutoCommit()) throw new TestException();

        //enableDefaultOnTransactionIsolation
        config.setEnableDefaultOnTransactionIsolation(false);
        if (config.isEnableDefaultOnTransactionIsolation()) throw new TestException();

        //forceDirtyOnSchemaAfterSet
        if (config.isForceDirtyOnSchemaAfterSet()) throw new TestException();
        config.setForceDirtyOnSchemaAfterSet(true);
        if (!config.isForceDirtyOnSchemaAfterSet()) throw new TestException();

        //forceDirtyOnCatalogAfterSet
        if (config.isForceDirtyOnCatalogAfterSet()) throw new TestException();
        config.setForceDirtyOnCatalogAfterSet(true);
        if (!config.isForceDirtyOnSchemaAfterSet()) throw new TestException();

        //enableJmx
        config.setEnableJmx(true);
        if (!config.isEnableJmx()) throw new TestException();

        //printConfigInfo
        config.setPrintConfigInfo(true);
        if (!config.isPrintConfigInfo()) throw new TestException();

        //printRuntimeLog
        config.setPrintRuntimeLog(true);
        if (!config.isPrintRuntimeLog()) throw new TestException();

        //poolImplementClassName
        config.setPoolImplementClassName(null);
        if (config.getPoolImplementClassName() == null) throw new TestException();
        config.setPoolImplementClassName("org.stone.beecp.pool.FastConnectionPool");
        if (!"org.stone.beecp.pool.FastConnectionPool".equals(config.getPoolImplementClassName()))
            throw new TestException();
    }
}
