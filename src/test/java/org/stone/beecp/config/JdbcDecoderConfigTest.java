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
import org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder;

public class JdbcDecoderConfigTest extends TestCase {

    public void testOnSetGet() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Class decodeClass = DummyJdbcLinkInfoDecoder.class;
        config.setJdbcLinkInfoDecoderClass(decodeClass);
        if (decodeClass != config.getJdbcLinkInfoDecoderClass()) throw new TestException();

        String decodeClassName = "org.stone.beecp.config.customization.DummyJdbcLinkInfoDecoder";
        config.setJdbcLinkInfDecoderClassName(decodeClassName);
        if (!decodeClassName.equals(config.getJdbcLinkInfDecoderClassName())) throw new TestException();

        DummyJdbcLinkInfoDecoder decoder = new DummyJdbcLinkInfoDecoder();
        config.setJdbcLinkInfoDecoder(decoder);
        if (decoder != config.getJdbcLinkInfoDecoder()) throw new TestException();
    }

    public void testOnErrorClass() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("user");
        config.setPassword("passwor");
        config.setUrl("jdbc:beecp://localhost/testdb");
        config.setDriverClassName("org.stone.beecp.mock.MockDriver");
        config.setJdbcLinkInfoDecoderClass(String.class);//error config
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("decoder"))) {
                throw new TestException();
            }
        }
    }

    public void testOnErrorClassName() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("user");
        config.setPassword("passwor");
        config.setUrl("jdbc:beecp://localhost/testdb");
        config.setDriverClassName("org.stone.beecp.mock.MockDriver");
        config.setJdbcLinkInfDecoderClassName("String");//error config
        try {
            config.check();
        } catch (BeeDataSourceConfigException e) {
            String msg = e.getMessage();
            if (!(msg != null && msg.contains("decoder"))) {
                throw new TestException();
            }
        }
    }
}
