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

import java.sql.SQLException;

public class ConfigObjectCheckTest extends TestCase {

    public void testOnCheck() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(10);
        config.setMaxActive(5);
        try {
            config.check();//check (initialSize > maxActive)
        } catch (BeeDataSourceConfigException e) {//fix
            config.setInitialSize(5);
            config.setMaxActive(10);
        }

        config.setConnectionFactoryClass(null);
        config.setJdbcLinkInfDecoderClassName(null);
        try {
            config.check();////check on url
        } catch (BeeDataSourceConfigException e) {
            config.setUrl("jdbc:beecp://localhost/testdb");//fix null url
        }
        try {
            config.check();//check on driver
        } catch (SQLException e) {
            config.setDriverClassName("org.stone.beecp.mock.MockDriver");//fix null driver
        }

        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
        }
        config.setDriverClassName(null);//fix null driver

        config.addSqlExceptionState("A");
        config.addSqlExceptionCode(500);
        config.setPrintConfigInfo(true);
        try {
            config.setValidTestSql("SELECT1");
            config.check();//valid alive test sql
        } catch (BeeDataSourceConfigException e) {
            //do nothing
        }
    }
}
