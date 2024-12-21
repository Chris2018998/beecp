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
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.Properties;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0016ObjectFieldFromPropertiesTest extends TestCase {

    public void testObjectTypeFields() {
        Properties prop = new Properties();
        prop.put("evictPredicate", "org.stone.beecp.objects.MockEvictConnectionPredicate");
        prop.put("evictPredicateClass", "org.stone.beecp.objects.MockEvictConnectionPredicate");
        prop.put("jdbcLinkInfoDecoder", "org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("jdbcLinkInfoDecoderClass", "org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("connectionFactory", "org.stone.beecp.objects.MockCommonConnectionFactory");
        prop.put("connectionFactoryClass", "org.stone.beecp.objects.MockCommonConnectionFactory");

        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop);
        Assert.assertNotNull(config.getEvictPredicate());
        Assert.assertNotNull(config.getEvictPredicateClass());
        Assert.assertNotNull(config.getJdbcLinkInfoDecoder());
        Assert.assertNotNull(config.getJdbcLinkInfoDecoderClass());
        Assert.assertNotNull(config.getConnectionFactory());
        Assert.assertNotNull(config.getConnectionFactoryClass());
    }
}
