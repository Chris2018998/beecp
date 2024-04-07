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

import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class ConfigFactory {

    public static BeeDataSourceConfig createEmpty() {
        return new BeeDataSourceConfig();
    }

    public static BeeDataSourceConfig createDefault() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        return config;
    }

    static void clearBeeCPInfoFromSystemProperties() {
        Properties properties = System.getProperties();
        Iterator iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            if (entry.getKey().startsWith("beecp.")) iterator.remove();
        }
    }
}
