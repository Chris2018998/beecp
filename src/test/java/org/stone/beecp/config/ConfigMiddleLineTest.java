/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.config;

import org.stone.base.TestCase;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.net.URL;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class ConfigMiddleLineTest extends TestCase {
    public void test() throws Exception {
        String filename = "beecp/config1.properties";
        URL url = ConfigMiddleLineTest.class.getResource(filename);
        url = ConfigMiddleLineTest.class.getClassLoader().getResource(filename);

        BeeDataSourceConfig testConfig = new BeeDataSourceConfig();
        testConfig.loadFromPropertiesFile(url.getFile());

        if (!"test1".equals(testConfig.getDefaultCatalog()))
            throw new BeeDataSourceConfigException("defaultCatalog error");
        if (!testConfig.isDefaultAutoCommit()) throw new BeeDataSourceConfigException("defaultAutoCommit error");
        if (!testConfig.isFairMode()) throw new BeeDataSourceConfigException("fairMode error");
        if (testConfig.getInitialSize() != 1) throw new BeeDataSourceConfigException("initialSize error");
        if (testConfig.getMaxActive() != 10) throw new BeeDataSourceConfigException("maxActive error");
    }
}