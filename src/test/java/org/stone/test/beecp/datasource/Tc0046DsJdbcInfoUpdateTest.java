/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockDataSource;
import org.stone.test.beecp.driver.MockXaDataSource;
import org.stone.test.beecp.objects.factory.SimpleMockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0046DsJdbcInfoUpdateTest {
    private final String username1 = "user1";
    private final String password1 = "password1";
    private final String url1 = "jdbc:beecp://localhost/mock1-testdb";

    private final String username2 = "user2";
    private final String password2 = "password2";
    private final String url2 = "jdbc:beecp://localhost/mock2-testdb";

    @Test
    public void testUpdateJdbcInfoToConfig() {
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            Assertions.assertNull(ds.getUsername());
            Assertions.assertNull(ds.getPassword());
            Assertions.assertNull(ds.getJdbcUrl());

            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setJdbcUrl(url1);

            Assertions.assertEquals(username1, ds.getUsername());
            Assertions.assertEquals(password1, ds.getPassword());
            Assertions.assertEquals(url1, ds.getJdbcUrl());

            ds.setUrl(url2);
            Assertions.assertEquals(url2, ds.getUrl());
            Assertions.assertEquals(url2, ds.getJdbcUrl());
        }
    }

    @Test
    public void testUnsupportableUpdateOnFactory() throws SQLException {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactory(new SimpleMockConnectionFactory());

            //ds pool ready
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            try {
                ds.setUsername(username2);
                Assertions.fail("[testUnsupportableUpdateToFactory]Test failed");
            } catch (UnsupportedOperationException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }

            try {
                ds.setPassword(password2);
                Assertions.fail("[testUnsupportableUpdateToFactory]Test failed");
            } catch (UnsupportedOperationException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }

            try {
                ds.setJdbcUrl(url2);
                Assertions.fail("[testUnsupportableUpdateToFactory]Test failed");
            } catch (UnsupportedOperationException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }

            try {
                ds.setUrl(url2);
                Assertions.fail("[testUnsupportableUpdateToFactory]Test failed");
            } catch (UnsupportedOperationException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }
        }
    }

    @Test
    public void testUpdateToConnectionFactoryByDriver() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setJdbcUrl(url1);

            //ds pool ready
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            //value check from set methods
            Object subDs = TestUtil.getFieldValue(ds, "subDs");
            Properties properties = (Properties) TestUtil.getFieldValue(subDs, "properties");
            Assertions.assertEquals(username1, properties.getProperty("user"));
            Assertions.assertEquals(password1, properties.getProperty("password"));
            Assertions.assertEquals(url1, TestUtil.getFieldValue(subDs, "url"));

            //runtime change test
            ds.setUsername(username2);
            Assertions.assertEquals(username2, properties.getProperty("user"));
            ds.setPassword(password2);
            Assertions.assertEquals(password2, properties.getProperty("password"));
            ds.setJdbcUrl(url2);
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));
            String url3 = "jdbc:beecp://localhost/mock3-testdb";
            ds.setUrl(url3);
            Assertions.assertEquals(url3, TestUtil.getFieldValue(subDs, "url"));
            ds.setUrl(url2);

            //null test on username and password
            ds.setUsername(null);
            Assertions.assertNull(properties.getProperty("user"));
            ds.setPassword(null);
            Assertions.assertNull(properties.getProperty("password"));
            //blank test on username and password
            ds.setUsername(username2);
            Assertions.assertEquals(username2, properties.getProperty("user"));
            ds.setPassword(password2);
            Assertions.assertEquals(password2, properties.getProperty("password"));
            ds.setUsername("");
            Assertions.assertEquals("", properties.getProperty("user"));
            ds.setPassword("");
            Assertions.assertEquals("", properties.getProperty("password"));

            //blank test on url
            ds.setJdbcUrl("");
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));
            ds.setUrl("");
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));

            //null test on url
            ds.setJdbcUrl(null);
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));
            ds.setUrl(null);
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));

        }
    }

    @Test
    public void testUpdateToConnectionFactoryByDriverDs() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setConnectionFactoryClassName(MockDataSource.class.getName());//driver data source

            //ds pool ready
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            //only support method: setUsername(),setPassword()
            Object subDs = TestUtil.getFieldValue(ds, "subDs");
            Assertions.assertEquals(username1, TestUtil.getFieldValue(subDs, "username"));
            Assertions.assertEquals(password1, TestUtil.getFieldValue(subDs, "password"));

            //runtime change test
            ds.setUsername(username2);
            Assertions.assertEquals(username2, TestUtil.getFieldValue(subDs, "username"));
            ds.setPassword(password2);
            Assertions.assertEquals(password2, TestUtil.getFieldValue(subDs, "password"));

            //null and blank test
            ds.setUsername("");
            Assertions.assertEquals("", TestUtil.getFieldValue(subDs, "username"));
            ds.setPassword("");
            Assertions.assertEquals("", TestUtil.getFieldValue(subDs, "password"));
            ds.setUsername(null);
            Assertions.assertNull(TestUtil.getFieldValue(subDs, "username"));
            ds.setPassword(null);
            Assertions.assertNull(TestUtil.getFieldValue(subDs, "password"));
        }
    }

    @Test
    public void testUpdateToXaConnectionFactoryByDriverDs() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setConnectionFactoryClassName(MockXaDataSource.class.getName());//driver data source

            //ds pool ready
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            //only support method: setUsername(),setPassword()
            Object subDs = TestUtil.getFieldValue(ds, "subDs");
            Assertions.assertEquals(username1, TestUtil.getFieldValue(subDs, "username"));
            Assertions.assertEquals(password1, TestUtil.getFieldValue(subDs, "password"));

            //runtime change test
            ds.setUsername(username2);
            Assertions.assertEquals(username2, TestUtil.getFieldValue(subDs, "username"));
            ds.setPassword(password2);
            Assertions.assertEquals(password2, TestUtil.getFieldValue(subDs, "password"));

            //blank or null are not acceptable
            ds.setUsername("");
            Assertions.assertEquals("", TestUtil.getFieldValue(subDs, "username"));
            ds.setPassword("");
            Assertions.assertEquals("", TestUtil.getFieldValue(subDs, "password"));
        }
    }
}
