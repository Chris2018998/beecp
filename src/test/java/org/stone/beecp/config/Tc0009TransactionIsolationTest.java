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
import org.stone.beecp.BeeTransactionIsolationLevels;

import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0009TransactionIsolationTest extends TestCase {

    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();

        config.setDefaultTransactionIsolationCode(123);
        Assert.assertEquals(config.getDefaultTransactionIsolationCode(), Integer.valueOf(123));

        config.setDefaultTransactionIsolationName(BeeTransactionIsolationLevels.LEVEL_READ_COMMITTED);
        Assert.assertEquals(BeeTransactionIsolationLevels.LEVEL_READ_COMMITTED, config.getDefaultTransactionIsolationName());
    }

    public void testOnInvalidIsolationName() {
        BeeDataSourceConfig config = createEmpty();
        try {
            config.setDefaultTransactionIsolationName("Test");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Invalid transaction isolation name"));
        }
    }
}
