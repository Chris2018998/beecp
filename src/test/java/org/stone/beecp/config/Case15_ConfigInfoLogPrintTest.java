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

    public void testOnConfigPrintInd() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();

        //situation1: not print config
        config.setPrintConfigInfo(false);//test point
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.isEmpty());

        //situation2: print config items
        config.setPrintConfigInfo(true);//test point
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
    }

    public void testOnExclusionConfigItems() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setPrintConfigInfo(true);

        //situation1: check default exclusion print
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".username"));
        Assert.assertFalse(logs.contains(".password"));
        Assert.assertFalse(logs.contains(".jdbcUrl"));
        Assert.assertFalse(logs.contains(".user"));
        Assert.assertFalse(logs.contains(".url"));
        Assert.assertFalse(logs.contains(".driverClassName"));

        //situation2: print out all items(clear all exclusion)
        config.clearAllConfigPrintExclusion();//test point
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".username"));
        Assert.assertTrue(logs.contains(".password"));
        Assert.assertTrue(logs.contains(".jdbcUrl"));
        Assert.assertTrue(logs.contains(".driverClassName"));

        //situation3: test on a not excluded item
        Assert.assertTrue(logs.contains(".maxActive"));

        //situation4: test on excluded item
        config.addConfigPrintExclusion("maxActive");//test point
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".maxActive"));

        //situation5: test on pint connectProperties
        config.addConnectProperty("dbGroup", "test");
        config.addConnectProperty("dbName", "test-Mysql1");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".connectProperties.dbGroup"));
        Assert.assertTrue(logs.contains(".connectProperties.dbName"));

        //situation6: test on exclusion connectProperties
        config.addConfigPrintExclusion("dbGroup");
        config.addConfigPrintExclusion("dbName");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".connectProperties.dbGroup"));
        Assert.assertFalse(logs.contains(".connectProperties.dbName"));
    }
}
