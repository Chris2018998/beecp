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
        Assertions.assertTrue(config.isEnableDefaultCatalog());//default value check(true)
        config.setEnableDefaultCatalog(false);
        Assertions.assertFalse(config.isEnableDefaultCatalog());
        config.setEnableDefaultCatalog(true);
        Assertions.assertTrue(config.isEnableDefaultCatalog());

        //enableDefaultOnSchema
        Assertions.assertTrue(config.isEnableDefaultSchema());//default value check(true)
        config.setEnableDefaultSchema(false);
        Assertions.assertFalse(config.isEnableDefaultSchema());
        config.setEnableDefaultSchema(true);
        Assertions.assertTrue(config.isEnableDefaultSchema());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isEnableDefaultReadOnly());//default value check(true)
        config.setEnableDefaultReadOnly(false);
        Assertions.assertFalse(config.isEnableDefaultReadOnly());
        config.setEnableDefaultReadOnly(true);
        Assertions.assertTrue(config.isEnableDefaultReadOnly());

        //enableDefaultOnReadOnly
        Assertions.assertTrue(config.isEnableDefaultAutoCommit());//default check
        config.setEnableDefaultAutoCommit(false);
        Assertions.assertFalse(config.isEnableDefaultAutoCommit());
        config.setEnableDefaultAutoCommit(true);
        Assertions.assertTrue(config.isEnableDefaultAutoCommit());

        //enableDefaultOnTransactionIsolation
        Assertions.assertTrue(config.isEnableDefaultTransactionIsolation());//default check
        config.setEnableDefaultTransactionIsolation(false);
        Assertions.assertFalse(config.isEnableDefaultTransactionIsolation());
        config.setEnableDefaultTransactionIsolation(true);
        Assertions.assertTrue(config.isEnableDefaultTransactionIsolation());

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
