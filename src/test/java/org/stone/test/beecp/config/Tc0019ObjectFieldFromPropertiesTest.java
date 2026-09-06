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
import org.stone.beecp.exception.BeeDataSourceConfigException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.beecp.pool.ConnectionPoolStatics.CONFIG_FACTORY_PROP_KEY_PREFIX;
import static org.stone.beecp.pool.ConnectionPoolStatics.CONFIG_FACTORY_PROP_SIZE;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0019ObjectFieldFromPropertiesTest {

    //****************************************************************************************************************//
    //                                                  test on Properties                                                   //
    //****************************************************************************************************************//
    @Test
    public void testLoadFailureFromProperties() {
        BeeDataSourceConfig config1 = createEmpty();
        try {
            config1.load((Properties) null);
            fail("[testLoadProperties]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            config1.load(new Properties());
            fail("[testLoadProperties]not threw exception when loading empty properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config1.load(properties);
            fail("[testLoadProperties]not threw exception when loading invalid properties item");
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    @Test
    public void testLoadSuccessFromProperties() {
        Properties prop = new Properties();
        prop.put("predicate", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        prop.put("predicateClass", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        prop.put("linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        prop.put("connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        prop.put(1L, 100L);//will be ignored
        BeeDataSourceConfig config = createDefault();
        config.load(prop);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNotNull(config.getConnectionFactoryClass());
    }


    @Test
    public void testKeyPrefixOnPropertiesKey() {
        Properties prop = new Properties();
        prop.put("beecp.predicate", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        prop.put("beecp.predicateClass", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        prop.put("beecp.linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        prop.put("beecp.connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        prop.put("bee.connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");

        String prefix = "beecp";
        BeeDataSourceConfig config = createDefault();
        config.load(prop, prefix);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNull(config.getConnectionFactoryClass());

        String prefix2 = "beecp.";
        BeeDataSourceConfig config2 = createDefault();
        config2.load(prop, prefix2);
        Assertions.assertNotNull(config2.getPredicate());
        Assertions.assertNotNull(config2.getPredicateClass());
        Assertions.assertNotNull(config2.getLinkInfoDecoder());
        Assertions.assertNotNull(config2.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config2.getConnectionFactory());
        Assertions.assertNull(config2.getConnectionFactoryClass());
    }

    //****************************************************************************************************************//
    //                                                  test on map                                                   //
    //****************************************************************************************************************//
    @Test
    public void testLoadFailureFromMap() {
        BeeDataSourceConfig config1 = createEmpty();
        try {
            config1.load((Map<String, Object>) null);
            fail("[testLoadMap]Load map cannot be null or empty");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load map cannot be null or empty"));
        }

        try {
            config1.load(new HashMap<String,Object>());
            fail("[testLoadMap]Load map cannot be null or empty");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load map cannot be null or empty"));
        }
    }

    @Test
    public void testLoadSuccessFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("predicate", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        map.put("predicateClass", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        map.put("linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        map.put("linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        map.put("connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        map.put("connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        BeeDataSourceConfig config = createDefault();
        config.load(map);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNotNull(config.getConnectionFactoryClass());
    }


    @Test
    public void testKeyPrefixOnMapKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("beecp.predicate", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        map.put("beecp.predicateClass", "org.stone.test.beecp.objects.predicate.MockEvictConnectionPredicate1");
        map.put("beecp.linkInfoDecoder", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        map.put("beecp.linkInfoDecoderClass", "org.stone.test.beecp.objects.decoder.SampleMockJdbcLinkInfoDecoder");
        map.put("beecp.connectionFactory", "org.stone.test.beecp.objects.factory.MockConnectionFactory");
        map.put("bee.connectionFactoryClass", "org.stone.test.beecp.objects.factory.MockConnectionFactory");

        String prefix = "beecp";
        BeeDataSourceConfig config = createDefault();
        config.load(map, prefix);
        Assertions.assertNotNull(config.getPredicate());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getLinkInfoDecoder());
        Assertions.assertNotNull(config.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config.getConnectionFactory());
        Assertions.assertNull(config.getConnectionFactoryClass());

        String prefix2 = "beecp.";
        BeeDataSourceConfig config2 = createDefault();
        config2.load(map, prefix2);
        Assertions.assertNotNull(config2.getPredicate());
        Assertions.assertNotNull(config2.getPredicateClass());
        Assertions.assertNotNull(config2.getLinkInfoDecoder());
        Assertions.assertNotNull(config2.getLinkInfoDecoderClass());
        Assertions.assertNotNull(config2.getConnectionFactory());
        Assertions.assertNull(config2.getConnectionFactoryClass());
    }

    @Test
    public void testConnectionFactoryConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put(CONFIG_FACTORY_PROP_SIZE, "3");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 1, "username=root&password=root");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 2, "parkNanos=1000");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 3, 10);
        BeeDataSourceConfig config = createDefault();
        config.load(map);
        Assertions.assertEquals("root", config.getConnectionFactoryProperty("username"));
        Assertions.assertEquals("root", config.getConnectionFactoryProperty("password"));
        Assertions.assertEquals("1000", config.getConnectionFactoryProperty("parkNanos"));

        map.put(CONFIG_FACTORY_PROP_SIZE, 3);
        BeeDataSourceConfig config2 = createDefault();
        config2.load(map);
        Assertions.assertEquals("root", config2.getConnectionFactoryProperty("username"));
        Assertions.assertEquals("root", config2.getConnectionFactoryProperty("password"));
        Assertions.assertEquals("1000", config2.getConnectionFactoryProperty("parkNanos"));
    }
}
