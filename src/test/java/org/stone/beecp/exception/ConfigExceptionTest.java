/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.exception;

import junit.framework.TestCase;
import org.stone.beecp.BeeDataSourceConfig;

import java.io.InputStream;
import java.util.Properties;

public class ConfigExceptionTest extends TestCase {

    public void testException() throws Exception {
        String configFile = "beecp/exception/ConfigExceptionTest.properties";
        InputStream propertiesStream = BeeDataSourceConfig.class.getResourceAsStream(configFile);
        propertiesStream = BeeDataSourceConfig.class.getClassLoader().getResourceAsStream(configFile);
        if (propertiesStream == null) propertiesStream = BeeDataSourceConfig.class.getResourceAsStream(configFile);

        Properties prop = new Properties();
        prop.load(propertiesStream);
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.loadFromProperties(prop);
    }
}
