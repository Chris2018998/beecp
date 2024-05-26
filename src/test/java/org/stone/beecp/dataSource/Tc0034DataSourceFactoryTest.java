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
import org.stone.beecp.BeeDataSourceFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

public class Tc0034DataSourceFactoryTest extends TestCase {

    public void testGetObjectInstance() throws Exception {
        BeeDataSourceFactory factory = new BeeDataSourceFactory();

        Object ob = factory.getObjectInstance(new Object(), null, null, null);
        Assert.assertNull(ob);
        Reference ref = new Reference("javax2.sql.DataSource");
        ob = factory.getObjectInstance(ref, null, null, null);
        Assert.assertNull(ob);

        //jndi test
        ref = new Reference("javax.sql.DataSource");
        ref.add(new StringRefAddr(CONFIG_TM_JNDI, "transactionManagerName"));
        try {
            factory.getObjectInstance(ref, null, new InitialContext(), null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NamingException);
        }

        ref.add(new StringRefAddr("poolName", null));
        ref.add(new StringRefAddr("defaultCatalog", ""));
        ref.add(new StringRefAddr("initial-size", "10"));
        ref.add(new StringRefAddr("max_active", "20"));
        ref.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref.add(new StringRefAddr(CONFIG_CONNECT_PROP, "A=a&B=b"));
        ref.add(new StringRefAddr(CONFIG_CONNECT_PROP_SIZE, "2"));
        ref.add(new StringRefAddr("connectProperties.1", "2"));
        ref.add(new StringRefAddr("connectProperties.2", "2"));
        ref.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2"));
        ref.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, "A,B,C"));
        BeeDataSource ds = (BeeDataSource) factory.getObjectInstance(ref, null, null, null);
        Assert.assertNotNull(ds);
        Assert.assertEquals(10, ds.getInitialSize());
        Assert.assertEquals(20, ds.getMaxActive());
        Assert.assertEquals("a", ds.getConnectProperty("A"));
        Assert.assertEquals("b", ds.getConnectProperty("B"));
    }
}


