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
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.TransactionIsolation;

import javax.sql.XAConnection;
import java.sql.SQLException;

public class ConfigObjectOperationTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername("test");
        if (!"test".equals(config.getUsername())) throw new TestException();
        config.setPassword("123");
        if (!"123".equals(config.getPassword())) throw new TestException();
        config.setPoolName("beePool");
        if (!"beePool".equals(config.getPoolName())) throw new TestException();

        String url = "jdbc:beecp://localhost/testdb";
        config.setUrl(url);
        if (!url.equals(config.getUrl())) throw new TestException();
        if (!url.equals(config.getJdbcUrl())) throw new TestException();
        config.setAsyncCreateInitConnection(true);
        if (!config.isAsyncCreateInitConnection()) throw new TestException();

        config.setForceCloseUsingOnClear(true);
        if (!config.isForceCloseUsingOnClear()) throw new TestException();

        config.setEnableJmx(true);
        if (!config.isEnableJmx()) throw new TestException();

        config.setFairMode(true);
        if (!config.isFairMode()) throw new TestException();

        config.removeSqlExceptionCode(5);
        config.addSqlExceptionCode(5);
        config.removeSqlExceptionCode(5);

        config.removeSqlExceptionState("ABC");
        config.addSqlExceptionState("ABC");
        config.addSqlExceptionState("7788");
        config.removeSqlExceptionState("ABC");

        config.setPrintConfigInfo(true);
        config.setPrintRuntimeLog(true);
        if (!config.isPrintConfigInfo()) throw new TestException();
        if (!config.isPrintRuntimeLog()) throw new TestException();

        config.setEnableDefaultOnCatalog(true);
        config.setEnableDefaultOnSchema(true);
        config.setEnableDefaultOnAutoCommit(true);
        config.setEnableDefaultOnTransactionIsolation(true);
        config.setEnableDefaultOnReadOnly(true);

        config.setForceDirtyOnSchemaAfterSet(true);
        config.setForceDirtyOnCatalogAfterSet(true);

        config.setInitialSize(-1);
        config.setInitialSize(5);
        config.setMaxActive(0);
        config.setMaxActive(5);
        config.setBorrowSemaphoreSize(0);
        config.setBorrowSemaphoreSize(5);
        config.setMaxWait(0L);
        config.setMaxWait(5000L);
        config.setConnectTimeout(0);
        config.setConnectTimeout(5);
        config.setIdleTimeout(0L);
        config.setIdleTimeout(3000L);
        config.setHoldTimeout(-1);
        config.setHoldTimeout(3000L);
        config.setValidTestTimeout(-1);
        config.setValidTestTimeout(3);
        config.setValidAssumeTime(-1);
        config.setValidAssumeTime(3000);
        config.setTimerCheckInterval(0);
        config.setTimerCheckInterval(3000);
        config.setDelayTimeForNextClear(-1);
        config.setDelayTimeForNextClear(3000);
        config.setValidTestSql(null);
        config.setValidTestSql("SELECT 2");
        if (!"SELECT 2".equals(config.getValidTestSql())) throw new TestException();

        config.setPoolImplementClassName(null);
        config.setPoolImplementClassName("org.stone.beecp.pool.FastConnectionPool");
        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        if (!TransactionIsolation.LEVEL_READ_COMMITTED.equals(config.getDefaultTransactionIsolationName()))
            throw new TestException();
        config.setDefaultTransactionIsolationCode(123);
        if (123 != config.getDefaultTransactionIsolationCode()) throw new TestException();

        try {
            config.setDefaultTransactionIsolationName("Test");
        } catch (BeeDataSourceConfigException e) {
            //do nothing
        }
        config.setRawXaConnectionFactory(new RawXaConnectionFactory() {
            public XAConnection create() throws SQLException {
                return null;
            }
        });

        Class factClass = org.stone.beecp.config.DummyThreadFactory.class;
        config.setThreadFactoryClass(factClass);
        if (!factClass.equals(config.getThreadFactoryClass())) throw new TestException();
        String factClassName = "org.stone.beecp.config.DummyThreadFactory";
        config.setThreadFactoryClassName(factClassName);
        if (!factClassName.equals(config.getThreadFactoryClassName())) throw new TestException();
        DummyThreadFactory threadFactory = new DummyThreadFactory();
        config.setThreadFactory(threadFactory);
        if (threadFactory != config.getThreadFactory()) throw new TestException();

        Class conFactClass = org.stone.beecp.pool.ConnectionFactoryByDriver.class;
        config.setConnectionFactoryClass(conFactClass);
        if (!conFactClass.equals(config.getConnectionFactoryClass())) throw new TestException();

        String conFactClassName = "org.stone.beecp.pool.ConnectionFactoryByDriver";
        config.setConnectionFactoryClassName(conFactClassName);
        if (!conFactClassName.equals(config.getConnectionFactoryClassName())) throw new TestException();

        Class prediClass = org.stone.beecp.config.DummySqlExceptionPredication.class;
        config.setSqlExceptionPredicationClass(prediClass);
        if (!prediClass.equals(config.getSqlExceptionPredicationClass())) throw new TestException();

        String prediClassName = "org.stone.beecp.config.DummySqlExceptionPredication";
        config.setSqlExceptionPredicationClassName(prediClassName);
        if (!prediClassName.equals(config.getSqlExceptionPredicationClassName())) throw new TestException();

        DummySqlExceptionPredication predication = new DummySqlExceptionPredication();
        config.setSqlExceptionPredication(predication);
        if (predication != config.getSqlExceptionPredication()) throw new TestException();

        Class decodeClass = DummyJdbcLinkInfoDecoder.class;
        config.setJdbcLinkInfoDecoderClass(decodeClass);
        if (decodeClass != config.getJdbcLinkInfoDecoderClass()) throw new TestException();
        String decodeClassName = "org.stone.beecp.config.DummyJdbcLinkInfoDecoder";
        config.setJdbcLinkInfDecoderClassName(decodeClassName);
        if (!decodeClassName.equals(config.getJdbcLinkInfDecoderClassName())) throw new TestException();


        config.addConnectProperty(null, null);
        config.addConnectProperty(null, "123");
        config.addConnectProperty("123", null);
        config.addConnectProperty("123", "123");
        config.removeConnectProperty(null);
        config.removeConnectProperty("123");
        config.addConnectProperty("a:123&b:124");

    }
}
