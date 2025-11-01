/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeTransactionIsolationNames;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0010TransactionIsolationTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getDefaultTransactionIsolation());
        config.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, config.getDefaultTransactionIsolation());
        config.setDefaultTransactionIsolation(null);
        Assertions.assertNull(config.getDefaultTransactionIsolation());
        config.setDefaultTransactionIsolation(Integer.valueOf(123));
        Assertions.assertEquals(123, config.getDefaultTransactionIsolation());

        //transactionIsolation name
        Assertions.assertNull(config.getDefaultTransactionIsolationName());
        config.setDefaultTransactionIsolationName(BeeTransactionIsolationNames.TRANSACTION_READ_UNCOMMITTED);
        Assertions.assertEquals(BeeTransactionIsolationNames.TRANSACTION_READ_UNCOMMITTED, config.getDefaultTransactionIsolationName());
        Assertions.assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, config.getDefaultTransactionIsolation());

        try {
            config.setDefaultTransactionIsolationName(null);
            fail("[testConfigurationSet]not thew exception when set null isolation name");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("The given value for configuration item 'default-transaction-isolation-name' cannot be null or empty"));
        }

        try {
            config.setDefaultTransactionIsolationName("");
            fail("[testConfigurationSet]not thew exception when set blank isolation name");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("The given value for configuration item 'default-transaction-isolation-name' cannot be null or empty"));
        }

        try {
            config.setDefaultTransactionIsolationName("Test");
            fail("[testConfigurationSet]not thew exception when set invalid transaction isolation name");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Invalid transaction isolation name"));
        }
    }
}
