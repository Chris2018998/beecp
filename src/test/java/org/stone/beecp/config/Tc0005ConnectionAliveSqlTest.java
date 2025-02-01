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

import java.security.InvalidParameterException;

import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0005ConnectionAliveSqlTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = createEmpty();

        try {
            config.setAliveTestSql(null);
            fail("Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[alive-test-sql] can't be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql("");
            fail("Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[alive-test-sql] can't be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql(" ");
            fail("Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[alive-test-sql] can't be null or empty", e.getMessage());
        }

        try {
            config.setAliveTestSql("SELECT1");
            fail("Setting test failed on configuration item[alive-test-sql]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[alive-test-sql] must be start with 'select '", e.getMessage());
        }
    }
}
