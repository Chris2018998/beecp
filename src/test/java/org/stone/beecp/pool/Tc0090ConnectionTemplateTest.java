/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.MockDefaultExceptionConnectionFactory;
import org.stone.beecp.objects.MockValidFailConnectionFactory;

import java.sql.Connection;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0090ConnectionTemplateTest extends TestCase {

    public void testEnableDefault() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        //enable all default
        config1.setEnableDefaultOnSchema(true);
        config1.setEnableDefaultOnCatalog(true);
        config1.setEnableDefaultOnReadOnly(true);
        config1.setEnableDefaultOnAutoCommit(true);
        config1.setEnableDefaultOnTransactionIsolation(true);

        FastConnectionPool pool1 = new FastConnectionPool();
        pool1.init(config1);

        Connection con1 = pool1.getConnection();
        Assert.assertFalse(con1.getAutoCommit());
        Assert.assertFalse(con1.isReadOnly());
        Assert.assertEquals(0, con1.getTransactionIsolation());
        Assert.assertNull(con1.getSchema());
        Assert.assertNull(con1.getCatalog());
        con1.close();
        pool1.close();

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        //enable all default
        config2.setEnableDefaultOnSchema(true);
        config2.setEnableDefaultOnCatalog(true);
        config2.setEnableDefaultOnReadOnly(true);
        config2.setEnableDefaultOnAutoCommit(true);
        config2.setEnableDefaultOnTransactionIsolation(true);

        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");

        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        Connection con2 = pool2.getConnection();
        Assert.assertTrue(con2.getAutoCommit());
        Assert.assertTrue(con2.isReadOnly());
        Assert.assertEquals(1, con2.getTransactionIsolation());
        Assert.assertEquals("schema", con2.getSchema());
        Assert.assertEquals("catalog", con2.getCatalog());
        con2.close();
        pool2.close();
    }

    public void testDisableDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        //disable all default
        config.setPrintRuntimeLog(true);
        config.setEnableDefaultOnSchema(false);
        config.setEnableDefaultOnCatalog(false);
        config.setEnableDefaultOnSchema(false);
        config.setEnableDefaultOnReadOnly(false);
        config.setEnableDefaultOnAutoCommit(false);
        config.setEnableDefaultOnTransactionIsolation(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con = pool.getConnection();
        Assert.assertFalse(con.getAutoCommit());
        Assert.assertFalse(con.isReadOnly());
        Assert.assertEquals(0, con.getTransactionIsolation());
        Assert.assertNull(con.getSchema());
        Assert.assertNull(con.getCatalog());
        con.close();
        pool.close();

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        //disable all default
        config2.setPrintRuntimeLog(true);
        config2.setEnableDefaultOnSchema(false);
        config2.setEnableDefaultOnCatalog(false);
        config2.setEnableDefaultOnSchema(false);
        config2.setEnableDefaultOnReadOnly(false);
        config2.setEnableDefaultOnAutoCommit(false);
        config2.setEnableDefaultOnTransactionIsolation(false);

        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");

        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        Connection con2 = pool2.getConnection();
        Assert.assertFalse(con2.getAutoCommit());
        Assert.assertFalse(con2.isReadOnly());
        Assert.assertEquals(0, con2.getTransactionIsolation());
        Assert.assertNull(con2.getSchema());
        Assert.assertNull(con2.getCatalog());
        con2.close();
        pool2.close();
    }

    public void testPrintRuntimeLog() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("failed to get auto-commit on first connection"));
        Assert.assertTrue(logs.contains("failed to get transaction isolation on first connection"));
        Assert.assertTrue(logs.contains("failed to get read-only on first connection"));
        Assert.assertTrue(logs.contains("failed to get catalog on first connection"));
        Assert.assertTrue(logs.contains("failed to get schema on first connection"));

        Assert.assertTrue(logs.contains("as auto-commit default value"));
        Assert.assertTrue(logs.contains("as transaction-isolation default value"));
        Assert.assertTrue(logs.contains("as read-only default value"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        config2.setEnableDefaultOnCatalog(true);
        config2.setEnableDefaultOnSchema(true);
        config2.setEnableDefaultOnReadOnly(true);
        config2.setEnableDefaultOnAutoCommit(true);
        config2.setEnableDefaultOnTransactionIsolation(true);
        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");
        config2.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs2.contains("failed to set auto-commit default"));
        Assert.assertTrue(logs2.contains("failed to set transaction-isolation default"));
        Assert.assertTrue(logs2.contains("failed to set read-only default"));
        Assert.assertTrue(logs2.contains("failed to set catalog default"));
        Assert.assertTrue(logs2.contains("failed to set schema default"));
    }

    public void testDisablePrintRuntimeLog() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(false);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("failed to get auto-commit on first connection"));
        Assert.assertFalse(logs.contains("failed to get transaction isolation on first connection"));
        Assert.assertFalse(logs.contains("failed to get read-only on first connection"));
        Assert.assertFalse(logs.contains("failed to get catalog on first connection"));
        Assert.assertFalse(logs.contains("failed to get schema on first connection"));

        Assert.assertFalse(logs.contains("as auto-commit default value"));
        Assert.assertFalse(logs.contains("as transaction-isolation default value"));
        Assert.assertFalse(logs.contains("as read-only default value"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(false);
        config2.setEnableDefaultOnCatalog(true);
        config2.setEnableDefaultOnSchema(true);
        config2.setEnableDefaultOnReadOnly(true);
        config2.setEnableDefaultOnAutoCommit(true);
        config2.setEnableDefaultOnTransactionIsolation(true);
        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");
        config2.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertFalse(logs2.contains("failed to set auto-commit default"));
        Assert.assertFalse(logs2.contains("failed to set transaction-isolation default"));
        Assert.assertFalse(logs2.contains("failed to set read-only default"));
        Assert.assertFalse(logs2.contains("failed to set catalog default"));
        Assert.assertFalse(logs2.contains("failed to set schema default"));
    }

    public void testValidateFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        MockValidFailConnectionFactory connectionFactory = new MockValidFailConnectionFactory();
        connectionFactory.setValidate(false);
        config.setConnectionFactory(connectionFactory);
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("'isValid' method of connection not supported by driver"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        MockValidFailConnectionFactory connectionFactory2 = new MockValidFailConnectionFactory();
        connectionFactory2.setErrorCode(100);
        config2.setConnectionFactory(connectionFactory2);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs2.contains("'isValid' method check failed for driver"));
    }

    public void testNetworkFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        MockValidFailConnectionFactory connectionFactory = new MockValidFailConnectionFactory();
        connectionFactory.setNetWorkTimeout(-1);
        config.setConnectionFactory(connectionFactory);
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("'networkTimeout' property of connection not supported by driver"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        MockValidFailConnectionFactory connectionFactory2 = new MockValidFailConnectionFactory();
        connectionFactory2.setExceptionOnNetworkTimeout(true);
        config2.setConnectionFactory(connectionFactory2);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs2.contains("'networkTimeout' property check failed for driver"));
    }

    public void testNotPrintLogForNetworkFail() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(false);
        MockValidFailConnectionFactory connectionFactory = new MockValidFailConnectionFactory();
        connectionFactory.setNetWorkTimeout(-1);
        config.setConnectionFactory(connectionFactory);
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("'networkTimeout' property of connection not supported by driver"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(false);
        MockValidFailConnectionFactory connectionFactory2 = new MockValidFailConnectionFactory();
        connectionFactory2.setExceptionOnNetworkTimeout(true);
        config2.setConnectionFactory(connectionFactory2);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertFalse(logs2.contains("'networkTimeout' property check failed for driver"));
    }
}
