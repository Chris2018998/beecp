/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeDataSourceFactory;
import org.stone.beecp.jta.BeeJtaDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.Hashtable;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

public class Tc0034DataSourceFactoryTest extends TestCase {

    public void testGetObjectInstance() throws Exception {
        BeeDataSourceFactory factory = new BeeDataSourceFactory();

        Object ob = factory.getObjectInstance(new Object(), null, null, null);
        Assert.assertNull(ob);
        Reference ref1 = new Reference("javax2.sql.DataSource");
        //jndi test1(jndi name ==null && nameCtx ==null)
        ob = factory.getObjectInstance(ref1, null, null, null);
        Assert.assertNull(ob);

        //jndi test2(jndi name !=null && nameCtx ==null)
        Reference ref2 = new Reference("javax.sql.DataSource");
        ref2.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref2.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref2.add(new StringRefAddr(CONFIG_TM_JNDI, "transactionManagerName"));
        BeeDataSource ds2 = (BeeDataSource) factory.getObjectInstance(ref2, null, null, null);
        Assert.assertNotNull(ds2);
        ds2.close();

        //jndi test3(jndi name ==null && tm!=null)
        Reference ref3 = new Reference("javax.sql.DataSource");
        ref3.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref3.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        BeeDataSource ds3 = (BeeDataSource) factory.getObjectInstance(ref3, null, new TestInitialContext(new TransactionManagerImpl()), null);
        Assert.assertNotNull(ds3);
        ds3.close();

        //jndi test4(jndi name !=null && tm!=null)
        Reference ref4 = new Reference("javax.sql.DataSource");
        ref4.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref4.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref4.add(new StringRefAddr(CONFIG_TM_JNDI, "transactionManagerName"));
        BeeJtaDataSource ds4 = (BeeJtaDataSource) factory.getObjectInstance(ref4, null, new TestInitialContext(new TransactionManagerImpl()), null);
        Assert.assertNotNull(ds4);
        ds4.close();

        Reference ref5 = new Reference("javax.sql.DataSource");
        ref5.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref5.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref5.add(new StringRefAddr("poolName", null));
        ref5.add(new StringRefAddr("defaultCatalog", ""));
        ref5.add(new StringRefAddr("initial-size", "10"));
        ref5.add(new StringRefAddr("max_active", "20"));
        ref5.add(new StringRefAddr(CONFIG_CONNECT_PROP, "A=a&B=b"));
        ref5.add(new StringRefAddr(CONFIG_CONNECT_PROP_SIZE, "2"));
        ref5.add(new StringRefAddr("connectProperties.1", "2"));
        ref5.add(new StringRefAddr("connectProperties.2", "2"));
        ref5.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2"));
        ref5.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, "A,B,C"));
        BeeDataSource ds5 = (BeeDataSource) factory.getObjectInstance(ref5, null, null, null);
        Assert.assertNotNull(ds5);
        Assert.assertEquals(10, ds5.getInitialSize());
        Assert.assertEquals(20, ds5.getMaxActive());
        Assert.assertEquals("a", ds5.getConnectProperty("A"));
        Assert.assertEquals("b", ds5.getConnectProperty("B"));
        ds5.close();

        Reference ref6 = new Reference("javax.sql.DataSource");
        ref6.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref6.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref6.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, ""));
        ref6.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, ""));
        BeeDataSource ds6 = (BeeDataSource) factory.getObjectInstance(ref6, null, null, null);
        ds6.close();

        Reference ref7 = new Reference("javax.sql.DataSource");
        ref7.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref7.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref7.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2,A"));
        BeeDataSource ds7 = null;
        try {
            ds7 = (BeeDataSource) factory.getObjectInstance(ref7, null, null, null);
        } catch (BeeDataSourceConfigException e) {
            String errorMsg = e.getMessage();
            Assert.assertTrue(errorMsg != null && errorMsg.contains("is not valid error code"));
        } finally {
            if (ds7 != null) ds7.close();
        }
    }

    //a dummy impl
    private static class TestInitialContext extends InitialContext {
        private final TransactionManager transactionManager;

        TestInitialContext(TransactionManager transactionManager) throws NamingException {
            this.transactionManager = transactionManager;
        }

        protected void init(Hashtable<?, ?> environment) {
        }

        public Object lookup(String name) {
            return transactionManager;
        }
    }

    //a dummy impl
    private static class TransactionManagerImpl implements TransactionManager {
        public void begin() {
        }

        public void commit() {
        }

        public int getStatus() {
            return 1;
        }

        public Transaction getTransaction() {
            return null;
        }

        public void resume(Transaction tobj) {
        }

        public void rollback() {
        }

        public void setRollbackOnly() {
        }

        public void setTransactionTimeout(int seconds) {
        }

        public Transaction suspend() {
            return null;
        }
    }
}


