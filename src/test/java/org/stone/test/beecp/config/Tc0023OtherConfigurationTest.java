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
 * @author Chris Liao
 */
public class Tc0023OtherConfigurationTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //fairMode
        Assertions.assertFalse(config.isFairMode());//default is false
        config.setFairMode(true);
        Assertions.assertTrue(config.isFairMode());

        //asyncCreateInitConnection
        Assertions.assertFalse(config.isAsyncCreateInitConnections());//default is false
        config.setAsyncCreateInitConnections(true);
        Assertions.assertTrue(config.isAsyncCreateInitConnections());

        //enableJmx
        Assertions.assertFalse(config.isRegisterMbeans());//default check
        config.setRegisterMbeans(true);
        Assertions.assertTrue(config.isRegisterMbeans());
        config.setRegisterMbeans(false);
        Assertions.assertFalse(config.isRegisterMbeans());

        //enableThreadLocal
        Assertions.assertTrue(config.isUseThreadLocal());//default check
        config.setUseThreadLocal(false);
        Assertions.assertFalse(config.isUseThreadLocal());
        config.setUseThreadLocal(true);
        Assertions.assertTrue(config.isUseThreadLocal());

        //enableThreadLocal
        Assertions.assertTrue(config.isRegisterJvmHook());//default check
        config.setRegisterJvmHook(false);
        Assertions.assertFalse(config.isRegisterJvmHook());
        config.setRegisterJvmHook(true);
        Assertions.assertTrue(config.isRegisterJvmHook());
    }
}
