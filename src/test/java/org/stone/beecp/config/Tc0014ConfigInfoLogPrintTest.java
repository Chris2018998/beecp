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
import org.stone.beecp.BeeDataSourceConfig;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0014ConfigInfoLogPrintTest extends TestCase {

    public void testOnConfigPrintInd() throws Exception {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeDataSourceConfig config = createDefault();

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
        BeeDataSourceConfig config = createDefault();
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

        //situation2: print out all items(clear all exclusion)
        config.clearAllConfigPrintExclusion();//test point
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".username"));
        Assert.assertTrue(logs.contains(".password"));
        Assert.assertTrue(logs.contains(".jdbcUrl"));

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
        config.addConfigPrintExclusion("connectProperties");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".connectProperties.dbGroup"));
        Assert.assertFalse(logs.contains(".connectProperties.dbName"));
    }
}
