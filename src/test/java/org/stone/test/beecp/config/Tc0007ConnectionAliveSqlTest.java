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

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0007ConnectionAliveSqlTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertEquals("SELECT 1", config.getAliveTestSql());//default check
        config.setAliveTestSql("SELECT 2");
        Assertions.assertEquals("SELECT 2", config.getAliveTestSql());

        try {
            config.setAliveTestSql(null);
            fail("[testOnSetAndGet]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql("");
            fail("[testOnSetAndGet]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql(" ");
            fail("[testOnSetAndGet]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql("SELECT1");
            fail("[testInvalidConfigTestSQL]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'alive-test-sql' must start with 'select '", e.getMessage());
        }

        Assertions.assertEquals("SELECT 2", config.getAliveTestSql());
    }
}
