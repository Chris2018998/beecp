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

public class ConfigObjectSetGetTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setPoolName("beePool");
        if (!"beePool".equals(config.getPoolName())) throw new TestException();

        config.setAsyncCreateInitConnection(true);
        if (!config.isAsyncCreateInitConnection()) throw new TestException();

        config.setForceCloseUsingOnClear(true);
        if (!config.isForceCloseUsingOnClear()) throw new TestException();

        config.setEnableJmx(true);
        if (!config.isEnableJmx()) throw new TestException();

        config.setFairMode(true);
        if (!config.isFairMode()) throw new TestException();

        config.setPrintConfigInfo(true);
        config.setPrintRuntimeLog(true);
        if (!config.isPrintConfigInfo()) throw new TestException();
        if (!config.isPrintRuntimeLog()) throw new TestException();

        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setEnableDefaultOnReadOnly(true);

        config.setForceDirtyOnSchemaAfterSet(true);
        config.setForceDirtyOnCatalogAfterSet(true);

        config.setInitialSize(-1);
        config.setInitialSize(5);
        config.setMaxActive(0);
        config.setMaxActive(5);
        config.setBorrowSemaphoreSize(0);
        config.setBorrowSemaphoreSize(5);
        config.setMaxWait(0L);
        config.setMaxWait(5000L);
        config.setConnectTimeout(0);
        config.setConnectTimeout(5);
        config.setIdleTimeout(0L);
        config.setIdleTimeout(3000L);
        config.setHoldTimeout(-1);
        config.setHoldTimeout(3000L);
        config.setAliveTestTimeout(-1);
        config.setAliveTestTimeout(3);
        config.setValidAssumeTime(-1);
        config.setValidAssumeTime(3000);
        config.setTimerCheckInterval(0);
        config.setTimerCheckInterval(3000);
        config.setDelayTimeForNextClear(-1);
        config.setDelayTimeForNextClear(3000);
        config.setPoolImplementClassName(null);
        config.setPoolImplementClassName("org.stone.beecp.pool.FastConnectionPool");
    }
}
