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

public class Case4_ConnectionSizeTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = ConfigFactory.createEmpty();

        config.setInitialSize(-1);
        config.setMaxActive(-1);
        Assert.assertNotEquals(config.getInitialSize(), -1);
        Assert.assertNotEquals(config.getMaxActive(), -1);

        config.setInitialSize(0);
        config.setMaxActive(0);
        Assert.assertEquals(config.getInitialSize(), 0);
        Assert.assertNotEquals(config.getMaxActive(), 0);


        config.setInitialSize(10);
        config.setMaxActive(20);
        Assert.assertEquals(config.getInitialSize(), 10);
        Assert.assertEquals(config.getMaxActive(), 20);
    }

    public void testOnErrorInitialSize() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setMaxActive(5);
        config.setInitialSize(10);

        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("initialSize must not be greater than maxActive"));
        }
    }
}
