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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeDataSourceFactory;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;

public class DataSourceFactoryTest extends TestCase {

    public void testGetObjectInstance() throws Exception {
        BeeDataSourceFactory factory = new BeeDataSourceFactory();

        Object ob = factory.getObjectInstance(new Object(), null, null, null);
        Assert.assertNull(ob);
        //if (ob != null) throw new TestException();

        Reference ref = new Reference("javax2.sql.DataSource");
        ob = factory.getObjectInstance(ref, null, null, null);
        Assert.assertNull(ob);
        //if (ob != null) throw new TestException();

        ref = new Reference("javax.sql.DataSource");
        ref.add(new StringRefAddr("jdbcUrl", "jdbc:beecp://localhost/testdb"));
        ref.add(new StringRefAddr("driverClassName", "org.stone.beecp.mock.MockDriver"));
        ref.add(new StringRefAddr(CONFIG_CONNECT_PROP, "A=c&b=2"));
        ref.add(new StringRefAddr(CONFIG_CONNECT_PROP_SIZE, "2"));
        ref.add(new StringRefAddr("connectProperties.1", "2"));
        ref.add(new StringRefAddr("connectProperties.2", "2"));
        ref.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_CODE, "1,2,A"));
        ref.add(new StringRefAddr(CONFIG_SQL_EXCEPTION_STATE, "A,B,C"));

        try {
            ob = factory.getObjectInstance(ref, null, null, null);
        } catch (BeeDataSourceConfigException e) {

        }

        //StringRefAddr tmName = new StringRefAddr("transactionManagerName", "transactionManagerName");
    }
}


