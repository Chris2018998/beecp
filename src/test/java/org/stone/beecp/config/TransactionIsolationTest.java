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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.TransactionIsolation;

public class TransactionIsolationTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        config.setDefaultTransactionIsolationCode(123);
        if (123 != config.getDefaultTransactionIsolationCode()) throw new TestException();

        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        if (!TransactionIsolation.LEVEL_READ_COMMITTED.equals(config.getDefaultTransactionIsolationName()))
            throw new TestException();
    }

    public void testOnInvalidIsolationName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.setDefaultTransactionIsolationName("Test");
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.startsWith("Invalid transaction isolation name")))
                throw new TestException();
        }
    }
}
