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
        ob = factory.getObjectInstance(ref1, null, null, null);
        Assert.assertNull(ob);

        //jndi test1(transactionManager)
        Reference ref2 = new Reference("javax.sql.DataSource");
        ref2.add(new StringRefAddr(CONFIG_TM_JNDI, "transactionManagerName"));
        factory.getObjectInstance(ref2, null, new TestInitialContext(null), null);

        //jndi test2(transactionManager)
        Reference ref3 = new Reference("javax.sql.DataSource");
        ref3.add(new StringRefAddr(CONFIG_TM_JNDI, "transactionManagerName"));
        factory.getObjectInstance(ref3, null, new TestInitialContext(new TransactionManagerImpl()), null);


        Reference ref4 = new Reference("javax.sql.DataSource");
        ref4.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref4.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref4.add(new StringRefAddr("poolName", null));
        ref4.add(new StringRefAddr("defaultCatalog", ""));
        ref4.add(new StringRefAddr("initial-size", "10"));
        ref4.add(new StringRefAddr("max_active", "20"));
        ref4.add(new StringRefAddr(CONFIG_CONNECT_PROP, "A=a&B=b"));
        ref4.add(new StringRefAddr(CONFIG_CONNECT_PROP_SIZE, "2"));
        ref4.add(new StringRefAddr("connectProperties.1", "2"));
        ref4.add(new StringRefAddr("connectProperties.2", "2"));
        ref4.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2"));
        ref4.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, "A,B,C"));
        BeeDataSource ds = (BeeDataSource) factory.getObjectInstance(ref4, null, null, null);
        Assert.assertNotNull(ds);
        Assert.assertEquals(10, ds.getInitialSize());
        Assert.assertEquals(20, ds.getMaxActive());
        Assert.assertEquals("a", ds.getConnectProperty("A"));
        Assert.assertEquals("b", ds.getConnectProperty("B"));


        Reference ref5 = new Reference("javax.sql.DataSource");
        ref5.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref5.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref5.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, ""));
        ref5.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, ""));
        factory.getObjectInstance(ref5, null, null, null);


        Reference ref6 = new Reference("javax.sql.DataSource");
        ref6.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref6.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref6.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2,A"));
        try {
            factory.getObjectInstance(ref6, null, null, null);
        } catch (BeeDataSourceConfigException e) {
            String errorMsg = e.getMessage();
            Assert.assertTrue(errorMsg != null && errorMsg.contains("is not valid error code"));
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


