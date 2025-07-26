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

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0006ConnectionAliveSqlTest {

    @Test
    public void testInvalidConfigTestSQL() {
        BeeDataSourceConfig config = createEmpty();

        try {
            config.setAliveTestSql(null);
            fail("[testInvalidConfigTestSQL]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql("");
            fail("[testInvalidConfigTestSQL]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql(" ");
            fail("[testInvalidConfigTestSQL]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'alive-test-sql' cannot be null or empty", e.getMessage());
        }


        try {
            config.setAliveTestSql("SELECT1");
            fail("[testInvalidConfigTestSQL]Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            assertEquals("The given value for configuration item 'alive-test-sql' must start with 'select '", e.getMessage());
        }

        config.setAliveTestSql("SELECT 1");
        assertEquals("SELECT 1", config.getAliveTestSql());
    }
}
