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

/**
 * Configuration items value set/get
 *
 * @author Chris Liao
 */

public class Tc0009ConnectionDefaultValueTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //defaultCatalog
        Assertions.assertNull(config.getDefaultCatalog());//default value check(null)
        config.setDefaultCatalog("catalog");
        Assertions.assertEquals("catalog", config.getDefaultCatalog());
        config.setDefaultCatalog(null);
        Assertions.assertNull(config.getDefaultCatalog());

        //defaultSchema
        Assertions.assertNull(config.getDefaultSchema());//default value check(null)
        config.setDefaultSchema("schema");
        Assertions.assertEquals("schema", config.getDefaultSchema());
        config.setDefaultSchema(null);
        Assertions.assertNull(config.getDefaultSchema());

        //defaultReadOnly
        Assertions.assertNull(config.isDefaultReadOnly());//default value check(null)
        config.setDefaultReadOnly(Boolean.TRUE);
        Assertions.assertTrue(config.isDefaultReadOnly().booleanValue());
        config.setDefaultReadOnly(Boolean.FALSE);
        Assertions.assertFalse(config.isDefaultReadOnly().booleanValue());

        //defaultAutoCommit
        Assertions.assertNull(config.isDefaultAutoCommit());//default value check(true)
        config.setDefaultAutoCommit(Boolean.FALSE);
        Assertions.assertFalse(config.isDefaultAutoCommit().booleanValue());
        config.setDefaultAutoCommit(Boolean.TRUE);
        Assertions.assertTrue(config.isDefaultAutoCommit().booleanValue());

        //enableDefaultOnCatalog
        Assertions.assertTrue(config.isUseDefaultCatalog());//default value check(true)
        config.setUseDefaultCatalog(false);
        Assertions.assertFalse(config.isUseDefaultCatalog());
        config.setUseDefaultCatalog(true);
        Assertions.assertTrue(config.isUseDefaultCatalog());

        //enableDefaultOnSchema
        Assertions.assertTrue(config.isUseDefaultSchema());//default value check(true)
        config.setUseDefaultSchema(false);
        Assertions.assertFalse(config.isUseDefaultSchema());
        config.setUseDefaultSchema(true);
        Assertions.assertTrue(config.isUseDefaultSchema());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isUseDefaultReadOnly());//default value check(true)
        config.setUseDefaultReadOnly(false);
        Assertions.assertFalse(config.isUseDefaultReadOnly());
        config.setUseDefaultReadOnly(true);
        Assertions.assertTrue(config.isUseDefaultReadOnly());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isUseDefaultAutoCommit());//default check
        config.setUseDefaultAutoCommit(false);
        Assertions.assertFalse(config.isUseDefaultAutoCommit());
        config.setUseDefaultAutoCommit(true);
        Assertions.assertTrue(config.isUseDefaultAutoCommit());

        //enableDefaultOnTransactionIsolation
        Assertions.assertTrue(config.isUseDefaultTransactionIsolation());//default check
        config.setUseDefaultTransactionIsolation(false);
        Assertions.assertFalse(config.isUseDefaultTransactionIsolation());
        config.setUseDefaultTransactionIsolation(true);
        Assertions.assertTrue(config.isUseDefaultTransactionIsolation());

        //forceDirtyOnSchemaAfterSet
        Assertions.assertFalse(config.isForceDirtyWhenSetSchema());//default check
        config.setForceDirtyWhenSetSchema(true);
        Assertions.assertTrue(config.isForceDirtyWhenSetSchema());
        config.setForceDirtyWhenSetSchema(false);
        Assertions.assertFalse(config.isForceDirtyWhenSetSchema());

        //forceDirtyOnCatalogAfterSet
        Assertions.assertFalse(config.isForceDirtyWhenSetCatalog());//default check
        config.setForceDirtyWhenSetCatalog(true);
        Assertions.assertTrue(config.isForceDirtyWhenSetCatalog());
        config.setForceDirtyWhenSetCatalog(false);
        Assertions.assertFalse(config.isForceDirtyWhenSetCatalog());
    }
}
