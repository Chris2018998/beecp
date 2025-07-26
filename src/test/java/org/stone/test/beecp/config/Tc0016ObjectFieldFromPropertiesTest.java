/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;

import java.util.Properties;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0016ObjectFieldFromPropertiesTest {

    @Test
    public void testObjectTypeFields() {
        Properties prop = new Properties();
        prop.put("evictPredicate", "org.stone.test.beecp.objects.MockEvictConnectionPredicate");
        prop.put("evictPredicateClass", "org.stone.test.beecp.objects.MockEvictConnectionPredicate");
        prop.put("jdbcLinkInfoDecoder", "org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("jdbcLinkInfoDecoderClass", "org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("connectionFactory", "org.stone.test.beecp.objects.MockCommonConnectionFactory");
        prop.put("connectionFactoryClass", "org.stone.test.beecp.objects.MockCommonConnectionFactory");

        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop);
        Assertions.assertNotNull(config.getEvictPredicate());
        Assertions.assertNotNull(config.getEvictPredicateClass());
        Assertions.assertNotNull(config.getJdbcLinkInfoDecoder());
        Assertions.assertNotNull(config.getJdbcLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNotNull(config.getConnectionFactoryClass());
    }

    @Test
    public void testKeyPrefix() {
        Properties prop = new Properties();
        prop.put("beecp.evictPredicate", "org.stone.test.beecp.objects.MockEvictConnectionPredicate");
        prop.put("beecp.evictPredicateClass", "org.stone.test.beecp.objects.MockEvictConnectionPredicate");
        prop.put("beecp.jdbcLinkInfoDecoder", "org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.jdbcLinkInfoDecoderClass", "org.stone.test.beecp.objects.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.connectionFactory", "org.stone.test.beecp.objects.MockCommonConnectionFactory");
        prop.put("bee.connectionFactoryClass", "org.stone.test.beecp.objects.MockCommonConnectionFactory");

        String prefix = "beecp";
        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop, prefix);
        Assertions.assertNotNull(config.getEvictPredicate());
        Assertions.assertNotNull(config.getEvictPredicateClass());
        Assertions.assertNotNull(config.getJdbcLinkInfoDecoder());
        Assertions.assertNotNull(config.getJdbcLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNull(config.getConnectionFactoryClass());

        String prefix2 = "beecp.";
        BeeDataSourceConfig config2 = createDefault();
        config2.loadFromProperties(prop, prefix2);
        Assertions.assertNotNull(config2.getEvictPredicate());
        Assertions.assertNotNull(config2.getEvictPredicateClass());
        Assertions.assertNotNull(config2.getJdbcLinkInfoDecoder());
        Assertions.assertNotNull(config2.getJdbcLinkInfoDecoderClass());
        Assertions.assertNotNull(config2.getConnectionFactory());
        Assertions.assertNull(config2.getConnectionFactoryClass());
    }
}
