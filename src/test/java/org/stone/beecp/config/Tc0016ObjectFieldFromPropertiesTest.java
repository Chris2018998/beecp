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

    public void testKeyPrefix() {
        Properties prop = new Properties();
        prop.put("beecp.evictPredicate", "org.stone.beecp.objects.MockEvictConnectionPredicate");
        prop.put("beecp.evictPredicateClass", "org.stone.beecp.objects.MockEvictConnectionPredicate");
        prop.put("beecp.jdbcLinkInfoDecoder", "org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.jdbcLinkInfoDecoderClass", "org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.connectionFactory", "org.stone.beecp.objects.MockCommonConnectionFactory");
        prop.put("bee.connectionFactoryClass", "org.stone.beecp.objects.MockCommonConnectionFactory");

        String prefix = "beecp";
        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop, prefix);
        Assert.assertNotNull(config.getEvictPredicate());
        Assert.assertNotNull(config.getEvictPredicateClass());
        Assert.assertNotNull(config.getJdbcLinkInfoDecoder());
        Assert.assertNotNull(config.getJdbcLinkInfoDecoderClass());
        Assert.assertNotNull(config.getConnectionFactory());
        Assert.assertNull(config.getConnectionFactoryClass());

        String prefix2 = "beecp.";
        BeeDataSourceConfig config2 = createDefault();
        config2.loadFromProperties(prop, prefix2);
        Assert.assertNotNull(config2.getEvictPredicate());
        Assert.assertNotNull(config2.getEvictPredicateClass());
        Assert.assertNotNull(config2.getJdbcLinkInfoDecoder());
        Assert.assertNotNull(config2.getJdbcLinkInfoDecoderClass());
        Assert.assertNotNull(config2.getConnectionFactory());
        Assert.assertNull(config2.getConnectionFactoryClass());
    }
}
