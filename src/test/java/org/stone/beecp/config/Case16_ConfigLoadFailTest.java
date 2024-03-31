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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;

import java.io.File;
import java.util.Properties;

public class Case16_ConfigLoadFailTest extends TestCase {

    public void testNullLoad() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.loadFromProperties(null);
        } catch (IllegalArgumentException e) {
            //do noting
        }
        try {
            config.loadFromProperties(new Properties());
        } catch (IllegalArgumentException e) {
            //do noting
        }

        try {
            config.loadFromPropertiesFile((File) null);
        } catch (IllegalArgumentException e) {
            //do noting
        }

        try {
            config.loadFromPropertiesFile((String) null);
        } catch (IllegalArgumentException e) {
            //do noting
        }

        try {
            config.loadFromPropertiesFile(new File("dd/dd/dd/dd/config1"));
        } catch (IllegalArgumentException e) {
            //do noting
        }

        try {
            config.loadFromPropertiesFile(new File("dd/dd/dd/dd/config1.properties"));
        } catch (IllegalArgumentException e) {
            //do noting
        }

        try {
            Properties configProperties = new Properties();
            configProperties.put("sqlExceptionCodeList", "1,A,C");//test on invalid error code
            config.loadFromProperties(configProperties);
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("SQLException error code")))
                throw new TestException();
        }
    }
}
