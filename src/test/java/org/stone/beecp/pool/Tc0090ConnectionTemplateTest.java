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

import java.sql.Connection;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0090ConnectionTemplateTest extends TestCase {

    public void testDisableDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setEnableDefaultOnSchema(false);
        config.setEnableDefaultOnCatalog(false);
        config.setEnableDefaultOnSchema(false);
        config.setEnableDefaultOnReadOnly(false);
        config.setEnableDefaultOnAutoCommit(false);
        config.setEnableDefaultOnTransactionIsolation(false);
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con1 = pool.getConnection();
        Assert.assertFalse(con1.getAutoCommit());
        Assert.assertFalse(con1.isReadOnly());
        Assert.assertEquals(0, con1.getTransactionIsolation());
        Assert.assertNull(con1.getSchema());
        Assert.assertNull(con1.getCatalog());
        con1.close();
        pool.close();
    }

    public void testEnableDefault() throws Exception {
        //setEnableDefaultOnSchema=true
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();
        Assert.assertEquals("schema", con.getSchema());
        Assert.assertEquals("catalog", con.getCatalog());
        Assert.assertTrue(con.getAutoCommit());
        Assert.assertTrue(con.isReadOnly());
        Assert.assertEquals(1, con.getTransactionIsolation());
        con.close();
        pool.close();
    }


    public void testLExceptionOnDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(2);
        config.setPrintRuntimeLog(false);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");
        config.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains("failed to get transaction isolation on first connection"));
        Assert.assertFalse(logs.contains("as transaction-isolation default value"));
        Assert.assertFalse(logs.contains("failed to set transaction-isolation default"));

        Assert.assertFalse(logs.contains("failed to get read-only on first connection"));
        Assert.assertFalse(logs.contains("as read-only default value"));
        Assert.assertFalse(logs.contains("failed to set tread-only default"));

        Assert.assertFalse(logs.contains("failed to get catalog on first connection"));
        Assert.assertFalse(logs.contains("failed to set catalog default"));

        Assert.assertFalse(logs.contains("failed to get schema on first connection"));
        Assert.assertFalse(logs.contains("failed to set schema default"));
    }

    public void testNotPrintExceptionOnDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(2);
        config.setPrintRuntimeLog(true);
        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnReadOnly(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");
        config.setConnectionFactoryClass(MockDefaultExceptionConnectionFactory.class);

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains("failed to get transaction isolation on first connection"));
        Assert.assertTrue(logs.contains("as transaction-isolation default value"));
        Assert.assertTrue(logs.contains("failed to set transaction-isolation default"));

        Assert.assertTrue(logs.contains("failed to get read-only on first connection"));
        Assert.assertTrue(logs.contains("as read-only default value"));
        Assert.assertTrue(logs.contains("failed to set tread-only default"));

        Assert.assertTrue(logs.contains("failed to get catalog on first connection"));
        Assert.assertTrue(logs.contains("failed to set catalog default"));

        Assert.assertTrue(logs.contains("failed to get schema on first connection"));
        Assert.assertTrue(logs.contains("failed to set schema default"));
    }
}
