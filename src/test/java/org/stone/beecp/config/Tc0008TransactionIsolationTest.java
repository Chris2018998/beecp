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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.TransactionIsolation;

public class Tc0008TransactionIsolationTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();

        config.setDefaultTransactionIsolationCode(123);
        Assert.assertEquals(config.getDefaultTransactionIsolationCode(), Integer.valueOf(123));

        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        Assert.assertEquals(config.getDefaultTransactionIsolationName(), TransactionIsolation.LEVEL_READ_COMMITTED);
    }

    public void testOnInvalidIsolationName() {
        BeeDataSourceConfig config = DsConfigFactory.createEmpty();
        try {
            config.setDefaultTransactionIsolationName("Test");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Invalid transaction isolation name"));
        }
    }
}
