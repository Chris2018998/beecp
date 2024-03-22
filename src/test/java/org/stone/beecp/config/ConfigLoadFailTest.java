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

import java.io.File;

public class ConfigLoadFailTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        try {
            config.loadFromProperties(null);
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




    }
}
