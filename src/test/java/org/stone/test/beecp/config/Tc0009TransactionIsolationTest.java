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
import org.stone.beecp.BeeTransactionIsolationLevels;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0009TransactionIsolationTest {

    @Test
    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();

        config.setDefaultTransactionIsolationCode(123);
        Assertions.assertEquals(config.getDefaultTransactionIsolationCode(), Integer.valueOf(123));

        config.setDefaultTransactionIsolationName(BeeTransactionIsolationLevels.LEVEL_READ_COMMITTED);
        Assertions.assertEquals(BeeTransactionIsolationLevels.LEVEL_READ_COMMITTED, config.getDefaultTransactionIsolationName());
    }

    @Test
    public void testOnInvalidIsolationName() {
        BeeDataSourceConfig config = createEmpty();
        try {
            config.setDefaultTransactionIsolationName("Test");
            fail("[testOnInvalidIsolationName]not thew exception when set invalid transaction isolation name");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Invalid transaction isolation name"));
        }
    }
}
