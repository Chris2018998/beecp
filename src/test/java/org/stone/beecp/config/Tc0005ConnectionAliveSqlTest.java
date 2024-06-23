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

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0005ConnectionAliveSqlTest extends TestCase {

    public void testOnSetAndGet() {
        BeeDataSourceConfig config = createEmpty();
        config.setAliveTestSql(null);
        Assert.assertNotNull(config.getAliveTestSql());

        config.setAliveTestSql("SELECT1");
        Assert.assertEquals("SELECT1", config.getAliveTestSql());
    }

    public void testOnInvalidSql() throws Exception {
        BeeDataSourceConfig config = createDefault();

        config.setAliveTestSql("SELECT1");
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Alive test sql must be start with 'select '"));
        }
    }
}
