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

import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.*;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;
import static org.stone.tools.CommonUtil.NCPU;

/**
 * @author Chris Liao
 */

public class Tc0007ConnectionSizeTest {

    @Test
    public void testValidConnectionSize() {
        BeeDataSourceConfig config = createEmpty();
        config.setInitialSize(0);
        assertEquals(0, config.getInitialSize());
        config.setInitialSize(1);
        assertEquals(1, config.getInitialSize());

        config.setMaxActive(1);
        assertEquals(1, config.getMaxActive());
        assertEquals(1, config.getBorrowSemaphoreSize());

        config.setInitialSize(10);
        config.setMaxActive(20);
        assertEquals(10, config.getInitialSize());
        assertEquals(20, config.getMaxActive());

        int borrowSemaphoreExpectSize = Math.min(20 / 2, NCPU);
        assertEquals(config.getBorrowSemaphoreSize(), borrowSemaphoreExpectSize);
    }

    @Test
    public void testInvalidConnectionSize() {
        BeeDataSourceConfig config = createEmpty();
        try {
            config.setInitialSize(-1);
            fail("[testInvalidConnectionSize]Setting test failed on configuration item[initial-size]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for the configuration item 'initial-size' cannot be less than zero", e.getMessage());
        }
        try {
            config.setMaxActive(-1);
            fail("[testInvalidConnectionSize]Setting test failed on configuration item[initial-size]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'max-active' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxActive(0);
            fail("[testInvalidConnectionSize]Setting test failed on configuration item[initial-size]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'max-active' must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void testInitializeSizeCheckFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(5);
        config.setInitialSize(10);

        try {
            config.check();
            fail("[testInitializeSizeCheckFail]Not threw check exception when max-active-size(" + config.getMaxActive() + ") is less than initial-size(" + config.getInitialSize() + ")");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            assertTrue(message != null && message.contains("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'"));
        }
    }
}
