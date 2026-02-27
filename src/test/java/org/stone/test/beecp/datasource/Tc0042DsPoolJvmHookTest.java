/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.TestUtil;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0042DsPoolJvmHookTest {
    @Test
    public void testOnStart() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setRegisterJvmHook(true);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object pool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "exitHook"));
        }

        config.setRegisterJvmHook(false);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object pool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "exitHook"));
        }
    }

    @Test
    public void testOnRestart() throws Exception {
        //true ---> false
        BeeDataSourceConfig config = createDefault();
        config.setRegisterJvmHook(true);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object pool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "exitHook"));

            config.setRegisterJvmHook(false);
            ds.restart(true, config);
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "exitHook"));
        }

        //false-->true
        BeeDataSourceConfig config2 = createDefault();
        config2.setRegisterJvmHook(false);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object pool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "exitHook"));

            config.setRegisterJvmHook(true);
            ds.restart(true, config);
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "exitHook"));
        }
    }
}
