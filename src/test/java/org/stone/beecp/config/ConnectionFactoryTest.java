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
import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.config.customization.DataSourceConfigFactory;
import org.stone.beecp.config.customization.NullConnectionFactory;
import org.stone.beecp.config.customization.NullXaConnectionFactory;

public class ConnectionFactoryTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Class factClass = NullXaConnectionFactory.class;
        config.setConnectionFactoryClass(factClass);
        if (!factClass.equals(config.getConnectionFactoryClass())) throw new TestException();

        String factClassName = "org.stone.beecp.config.customization.NullConnectionFactory";
        config.setConnectionFactoryClassName(factClassName);
        if (!factClassName.equals(config.getConnectionFactoryClassName())) throw new TestException();

        RawConnectionFactory factory1 = new NullConnectionFactory();
        config.setRawConnectionFactory(factory1);
        if (factory1 != config.getConnectionFactory()) throw new TestException();

        RawXaConnectionFactory factory2 = new NullXaConnectionFactory();
        config.setRawXaConnectionFactory(factory2);
        if (factory2 != config.getConnectionFactory()) throw new TestException();
    }


    public void testOnInvalidFactoryClass() throws Exception {
        try {
            BeeDataSourceConfig config = DataSourceConfigFactory.createDefault();
            config.setConnectionFactoryClass(String.class);//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("which must extend from one of class");
            if (!matched) throw new TestException();
        }
    }

    public void testOnInvalidFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = DataSourceConfigFactory.createDefault();
            config.setConnectionFactoryClassName("java.lang.String");//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            boolean matched = msg != null && msg.contains("which must extend from one of class");
            if (!matched) throw new TestException();
        }
    }

    public void testOnNotFoundFactoryClassName() throws Exception {
        try {
            BeeDataSourceConfig config = DataSourceConfigFactory.createDefault();
            config.setConnectionFactoryClassName("xx.xx.xx");//class not found
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            if (!(cause != null && cause instanceof ClassNotFoundException)) throw new TestException();
        }
    }

}
