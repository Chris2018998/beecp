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
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.objects.MockCommonConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

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
        Assert.assertTrue(con1.getAutoCommit());
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
        Assert.assertTrue(con.getAutoCommit());
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
        Assert.assertTrue(con2.getAutoCommit());
        Assert.assertFalse(con2.isReadOnly());
        Assert.assertEquals(0, con2.getTransactionIsolation());
        Assert.assertNull(con2.getSchema());
        Assert.assertNull(con2.getCatalog());
        con2.close();
        pool2.close();
    }

    public void testNotSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.enableExceptionOnMethod("getAutoCommit,setAutoCommit,isReadOnly,setReadOnly");
        connectionProperties.enableExceptionOnMethod("getTransactionIsolation,setTransactionIsolation");
        connectionProperties.enableExceptionOnMethod("setCatalog,getCatalog,setSchema,getSchema");
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

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

        //not print logs
        config.setPrintRuntimeLog(false);
        pool = new FastConnectionPool();
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.init(config);
        pool.close();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("failed to get auto-commit on first connection"));
        Assert.assertFalse(logs.contains("failed to get transaction isolation on first connection"));
        Assert.assertFalse(logs.contains("failed to get read-only on first connection"));
        Assert.assertFalse(logs.contains("failed to get catalog on first connection"));
        Assert.assertFalse(logs.contains("failed to get schema on first connection"));
        Assert.assertFalse(logs.contains("as auto-commit default value"));
        Assert.assertFalse(logs.contains("as transaction-isolation default value"));
        Assert.assertFalse(logs.contains("as read-only default value"));
    }

    public void testSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        //set defaults to config
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.enableExceptionOnMethod("getAutoCommit,setAutoCommit,isReadOnly,setReadOnly");
        connectionProperties.enableExceptionOnMethod("getTransactionIsolation,setTransactionIsolation");
        connectionProperties.enableExceptionOnMethod("setCatalog,getCatalog,setSchema,getSchema");
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("failed to set auto-commit default"));
        Assert.assertTrue(logs.contains("failed to set transaction-isolation default"));
        Assert.assertTrue(logs.contains("failed to set read-only default"));
        Assert.assertTrue(logs.contains("failed to set catalog default"));
        Assert.assertTrue(logs.contains("failed to set schema default"));

        //not print logs
        config.setPrintRuntimeLog(false);
        pool = new FastConnectionPool();
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.init(config);
        pool.close();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("failed to set auto-commit default"));
        Assert.assertFalse(logs.contains("failed to set transaction-isolation default"));
        Assert.assertFalse(logs.contains("failed to set read-only default"));
        Assert.assertFalse(logs.contains("failed to set catalog default"));
        Assert.assertFalse(logs.contains("failed to set schema default"));
    }

    public void testSupportOnIsValidMethod() throws Exception {
        //return false from isValid
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setPrintRuntimeLog(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setValid(false);
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(connectionProperties);
        config1.setConnectionFactory(factory);
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config1);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("'isValid' method of connection not supported by driver"));

        //not print logs
        config1.setPrintRuntimeLog(false);
        pool = new FastConnectionPool();
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.init(config1);
        pool.close();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("'isValid' method of connection not supported by driver"));

        //exception from isValid
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        connectionProperties.setValid(true);
        connectionProperties.enableExceptionOnMethod("isValid");
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        config2.setConnectionFactory(factory);

        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs2.contains("'isValid' method check failed for driver"));

        //not print logs
        config2.setPrintRuntimeLog(false);
        pool2 = new FastConnectionPool();
        logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        pool2.init(config2);
        pool2.close();
        logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertFalse(logs2.contains("'isValid' method check failed for driver"));
    }

    public void testSupportOnNetworkTimeoutMethod() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLog(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setNetworkTimeout(-1);
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        FastConnectionPool pool = new FastConnectionPool();
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.init(config);
        pool.close();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("'networkTimeout' property of connection not supported by driver"));

        //not print logs
        config.setPrintRuntimeLog(false);
        pool = new FastConnectionPool();
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.init(config);
        pool.close();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("'networkTimeout' property of connection not supported by driver"));

        //exception from  getNetworkTimeout
        connectionProperties.setNetworkTimeout(0);
        connectionProperties.enableExceptionOnMethod("getNetworkTimeout");
        connectionProperties.setMockException1(new SQLException("NetworkTimeout"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLog(true);
        config2.setConnectionFactory(factory);

        FastConnectionPool pool2 = new FastConnectionPool();
        StoneLogAppender logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        pool2.init(config2);
        pool2.close();
        String logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs2.contains("'networkTimeout' property check failed for driver"));

        //not print logs
        config2.setPrintRuntimeLog(false);
        logAppender2 = getStoneLogAppender();
        logAppender2.beginCollectStoneLog();
        pool2 = new FastConnectionPool();
        pool2.init(config2);
        pool2.close();
        logs2 = logAppender2.endCollectedStoneLog();
        Assert.assertFalse(logs2.contains("'networkTimeout' property check failed for driver"));

        //exception from setNetworkTimeout
        connectionProperties.setNetworkTimeout(10);
        connectionProperties.disableExceptionOnMethod("getNetworkTimeout");
        connectionProperties.enableExceptionOnMethod("setNetworkTimeout");
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setPrintRuntimeLog(true);
        config3.setConnectionFactory(factory);

        FastConnectionPool pool3 = new FastConnectionPool();
        StoneLogAppender logAppender3 = getStoneLogAppender();
        logAppender3.beginCollectStoneLog();
        pool3.init(config3);
        pool3.close();
        String logs3 = logAppender2.endCollectedStoneLog();
        Assert.assertTrue(logs3.contains("'networkTimeout' property check failed for driver"));
    }
}
