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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

public class ConfigCheckTest extends TestCase {

    public void testOnCheckNumber() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(10);
        config.setMaxActive(5);
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            //do nothing
        }

        config.setInitialSize(5);
        config.setMaxActive(10);

        try {
            config.setValidTestSql("SELECT1");
            config.check();//valid alive test sql
        } catch (BeeDataSourceConfigException e) {
            //do nothing
        }
    }
}
