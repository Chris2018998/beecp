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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import junit.framework.TestCase;
import org.junit.Assert;
import org.slf4j.LoggerFactory;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;

public class Case15_ConfigInfoLogPrintTest extends TestCase {
    private StoneLogAppender logAppender = null;

    private StoneLogAppender getStoneLogAppender() {
        if (logAppender != null) return logAppender;
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger loggerImpl : loggerContext.getLoggerList()) {
            Appender appender = loggerImpl.getAppender("console");
            if (appender instanceof StoneLogAppender) {
                logAppender = (StoneLogAppender) appender;
                break;
            }
        }
        return logAppender;
    }

    public void testNotPrintConfig() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(false);//not print config while pool initialize
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.isEmpty());
    }

    public void testPrintConfig() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);//not print config while pool initialize
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
    }

    public void testOnDefaultExclusionConfig() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();

        //default exclusion list: username, password, jdbcUrl,user,url
        Assert.assertTrue(!logs.contains("username"));
        Assert.assertTrue(!logs.contains("password"));
        Assert.assertTrue(!logs.contains("jdbcUrl"));
        Assert.assertTrue(!logs.contains("user"));
        Assert.assertTrue(!logs.contains("url"));
    }

    public void testOnExclusionConfig() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("maxActive"));//printed

        config.addConfigPrintExclusion("maxActive");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(!logs.contains("maxActive"));//not printed
    }
}
