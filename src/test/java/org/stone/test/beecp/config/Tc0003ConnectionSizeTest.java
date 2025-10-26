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

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.tools.CommonUtil.NCPU;

/**
 * @author Chris Liao
 */

public class Tc0003ConnectionSizeTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertEquals(0, config.getInitialSize());//default value check
        Assertions.assertEquals(Math.min(Math.max(10, NCPU), 50), config.getMaxActive());//default value check
        Assertions.assertEquals(Math.min(config.getMaxActive() / 2, NCPU), config.getSemaphoreSize());//default value check

        //InitialSize
        config.setInitialSize(1);
        Assertions.assertEquals(1, config.getInitialSize());//default value check
        try {
            config.setInitialSize(-1);
            fail("[testSetAndGet]Setting test failed on configuration item[semaphore-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for the configuration item 'initial-size' cannot be less than zero", e.getMessage());
        }
        Assertions.assertEquals(1, config.getInitialSize());

        //maxActive
        config.setMaxActive(5);
        Assertions.assertEquals(5, config.getMaxActive());
        config.setMaxActive(1);
        Assertions.assertEquals(1, config.getMaxActive());

        try {
            config.setMaxActive(0);
            fail("[testSetAndGet]Setting test failed on configuration item[max-active]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-active' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxActive(-1);
            fail("[testSetAndGet]Setting test failed on configuration item[max-active]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-active' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(1, config.getMaxActive());

        //borrowSemaphoreSize
        config.setSemaphoreSize(1);//positive number is acceptable
        Assertions.assertEquals(1, config.getSemaphoreSize());
        try {
            config.setSemaphoreSize(0);//zero is not acceptable
            fail("[testOnSetAndGet]Setting test failed on configuration item[semaphore-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'semaphore-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setSemaphoreSize(-1);//negative number is not acceptable
            fail("[testOnSetAndGet]Setting test failed on configuration item[semaphore-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'semaphore-size' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(1, config.getSemaphoreSize());//check value is whether changed
    }

    @Test
    public void testCheckFailed_InitializeSizeGreaterThanMaxActive() throws Exception {
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
