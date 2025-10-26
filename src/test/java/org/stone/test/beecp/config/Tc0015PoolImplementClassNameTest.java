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
import org.stone.test.beecp.objects.pool.BaseSimplePoolImpl;

/**
 * @author Chris Liao
 */
public class Tc0015PoolImplementClassNameTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getPoolImplementClassName());//default check
        config.setPoolImplementClassName(BaseSimplePoolImpl.class.getName());
        Assertions.assertEquals(BaseSimplePoolImpl.class.getName(), config.getPoolImplementClassName());
        config.setPoolImplementClassName(null);
        Assertions.assertNull(config.getPoolImplementClassName());
    }
}
