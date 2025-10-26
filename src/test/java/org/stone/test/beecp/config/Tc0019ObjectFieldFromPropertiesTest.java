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
public class Tc0019ObjectFieldFromPropertiesTest {

    @Test
    public void testObjectTypeFields() {
        Properties prop = new Properties();
        prop.put("predicate", "org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate");
        prop.put("predicateClass", "org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate");
        prop.put("linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        prop.put("connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");

        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNotNull(config.getConnectionFactoryClass());
    }

    @Test
    public void testKeyPrefix() {
        Properties prop = new Properties();
        prop.put("beecp.predicate", "org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate");
        prop.put("beecp.predicateClass", "org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate");
        prop.put("beecp.linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        prop.put("bee.connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");

        String prefix = "beecp";
        BeeDataSourceConfig config = createDefault();
        config.loadFromProperties(prop, prefix);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNull(config.getConnectionFactoryClass());

        String prefix2 = "beecp.";
        BeeDataSourceConfig config2 = createDefault();
        config2.loadFromProperties(prop, prefix2);
        Assertions.assertNotNull(config2.getPredicate());
        Assertions.assertNotNull(config2.getPredicateClass());
        Assertions.assertNotNull(config2.getLinkInfoDecoder());
        Assertions.assertNotNull(config2.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config2.getConnectionFactory());
        Assertions.assertNull(config2.getConnectionFactoryClass());
    }
}
