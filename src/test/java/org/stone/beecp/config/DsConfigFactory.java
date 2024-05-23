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
import java.util.Properties;

public class DsConfigFactory {

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
        Iterator<Object> iterator = properties.keySet().iterator();

        while (iterator.hasNext()) {
            Object key = iterator.next();
            if (key.toString().startsWith("beecp.")) iterator.remove();
        }
    }
}
