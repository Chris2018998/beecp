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
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.objects.MockCommonXaConnectionFactory;

import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0011ConnectionFactoryTest extends TestCase {

    public void testSetGetOnConfiguration() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeConnectionFactory> factClass = MockCommonConnectionFactory.class;
        config.setConnectionFactoryClass(factClass);
        Assert.assertEquals(factClass, config.getConnectionFactoryClass());

        String factClassName = MockCommonConnectionFactory.class.getName();
        config.setConnectionFactoryClassName(factClassName);
        Assert.assertEquals(config.getConnectionFactoryClassName(), factClassName);

        BeeConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config.setConnectionFactory(connectionFactory);
        Assert.assertEquals(config.getConnectionFactory(), connectionFactory);

        MockCommonXaConnectionFactory xaConnectionFactory = new MockCommonXaConnectionFactory();
        config.setXaConnectionFactory(xaConnectionFactory);
        Assert.assertEquals(config.getConnectionFactory(), xaConnectionFactory);
    }

    public void testAbandoningJdbcLinkInfo() throws SQLException {
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setUsername(DsConfigFactory.JDBC_USER);
        config1.setConnectionFactory(connectionFactory);
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        BeeDataSourceConfig checkedConfig = config1.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assert.assertNull(checkedConfig.getUsername());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setPassword(DsConfigFactory.JDBC_PASSWORD);
        config2.setConnectionFactory(connectionFactory);
        logAppender.beginCollectStoneLog();
        checkedConfig = config2.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assert.assertNull(checkedConfig.getPassword());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setUrl(DsConfigFactory.MOCK_URL);
        config3.setConnectionFactory(connectionFactory);
        logAppender.beginCollectStoneLog();
        checkedConfig = config3.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assert.assertNull(checkedConfig.getUrl());

        BeeDataSourceConfig config4 = createEmpty();
        config4.setDriverClassName(DsConfigFactory.MOCK_DRIVER);
        config4.setConnectionFactory(connectionFactory);
        logAppender.beginCollectStoneLog();
        checkedConfig = config4.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("configured jdbc link info abandoned according that a connection factory has been existed"));
        Assert.assertNull(checkedConfig.getDriverClassName());
    }

    public void testConnectionFactoryCreationFailure() throws SQLException {
        try {//error factory class
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClass(String.class);//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }

        try {//error factory class name
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("java.lang.String");//invalid config
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            String message = cause.getMessage();
            Assert.assertTrue(message != null && message.contains("which must extend from one of type"));
        }

        try {//not found factory class
            BeeDataSourceConfig config = createDefault();
            config.setConnectionFactoryClassName("xx.xx.xx");//class not found
            config.check();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        }
    }
}
