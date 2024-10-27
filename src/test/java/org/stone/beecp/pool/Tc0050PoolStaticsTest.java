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
import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.driver.MockDriver;
import org.stone.beecp.driver.MockXaDataSource;
import org.stone.beecp.objects.MockBlockPoolImplementation1;
import org.stone.beecp.objects.MockBlockPoolImplementation2;
import org.stone.beecp.objects.MockDriverConnectionFactory;
import org.stone.beecp.objects.MockObjectForPropertiesSet;
import org.stone.tools.exception.BeanException;

import javax.sql.XAConnection;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.stone.beecp.config.DsConfigFactory.MOCK_URL;
import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.loadDriver;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.tools.BeanUtil.*;

public class Tc0050PoolStaticsTest extends TestCase {

    public void testInvalidDriverClass() {
        try {
            loadDriver("org.stone.beecp.mock.MockDriver2");
        } catch (BeeDataSourceConfigException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to create jdbc driver by class:"));
        }
    }

    public void testTwiceClose() {
        MockDriver driver = new MockDriver();
        MockXaDataSource xaDataSource = new MockXaDataSource();
        String url = MOCK_URL;

        Connection con = null;
        Statement statement = null;
        ResultSet resultSet = null;
        XAConnection xaCon = null;

        try {
            con = driver.connect(url, null);
            xaCon = xaDataSource.getXAConnection();
            statement = con.createStatement();
            resultSet = statement.executeQuery("select * from Test_User");
        } catch (Exception e) {
            //do nothing
        } finally {
            oclose(resultSet);
            oclose(statement);
            oclose(con);
            oclose(xaCon);
        }

        oclose(resultSet);
        oclose(statement);
        oclose(con);
        oclose(xaCon);
    }

    public void testOnDummyCommonDataSource() {
        BeeDataSourceConfig config = createDefault();
        config.setConnectionFactoryClass(MockDriverConnectionFactory.class);
        BeeDataSource ds = new BeeDataSource(config);
        try {
            ds.getLogWriter();
            fail("Failed to test getLogWriter on dummy ds");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.setLogWriter(null);
            fail("Failed to test setLogWriter on dummy ds");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.getLoginTimeout();
            fail("Failed to test getLoginTimeout on dummy ds");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.setLoginTimeout(10);
            fail("Failed to test setLoginTimeout on dummy ds");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.getParentLogger();
            fail("Failed to test getParentLogger on dummy ds");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Not supported"));
        }
    }

    public void testOnCloseMethod() throws SQLException {
        BeeDataSource ds = new BeeDataSource(createDefault());
        Connection con = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            con = ds.getConnection();
            statement = con.createStatement();
            resultSet = statement.executeQuery("select * from Test_User");
        } finally {
            oclose(resultSet);
            oclose(statement);
            oclose(con);
        }

        //test on closed resultSet
        assert resultSet != null;
        Assert.assertTrue(resultSet.isClosed());
        Assert.assertTrue(resultSet.toString().contains("ResultSet has been closed"));

        try {
            resultSet.getString(0);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after resultSet closed"));
        }

        //test on closed statement
        Assert.assertTrue(statement.toString().contains("Statement has been closed"));
        Assert.assertTrue(statement.isClosed());

        try {
            statement.executeQuery("select * from Test_User");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        //test on closed connection
        Assert.assertTrue(con.toString().contains("Connection has been closed"));
        Assert.assertTrue(con.isClosed());

        try {
            con.createStatement();
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }
    }

    public void testSetMethodsLookup() {
        Map<String, Method> map = getClassSetMethodMap(Tc0050PoolStaticsTest.SetTestBean.class);
        Assert.assertEquals(1, map.size());
    }

    public void testPropertyValueGet() {
        Map<String, String> map1 = new HashMap<>(3);
        map1.put("maxActive", "5");
        map1.put("max-active", "10");
        map1.put("max_active", "20");
        map1.put("URL", "http://localhost:8080/testdb");
        Assert.assertEquals("http://localhost:8080/testdb", getPropertyValue(map1, "URL"));
        Assert.assertEquals("http://localhost:8080/testdb", getPropertyValue(map1, "uRL"));
        Assert.assertEquals("5", getPropertyValue(map1, "maxActive"));
        map1.remove("maxActive");
        Assert.assertEquals("10", getPropertyValue(map1, "maxActive"));
        map1.remove("max-active");
        Assert.assertEquals("20", getPropertyValue(map1, "maxActive"));
        map1.remove("max_active");
        Assert.assertNull(getPropertyValue(map1, "maxActive"));
    }

    public void testOnPropertiesValueSet() throws Exception {
        try {
            setPropertiesValue(null, null);
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("Bean can't be null"));
        }

        try {
            setPropertiesValue(null, null, null);
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("Bean can't be null"));
        }

        BeeDataSourceConfig bean = new BeeDataSourceConfig();
        Map<String, Method> emptySetMethodMap = new HashMap<>();
        Map<String, Object> emptySetValueMap = new HashMap<>();
        Map<String, Method> nonEmptySetMethodMap = new HashMap<>();
        Map<String, Object> nonEmptySetValueMap = new HashMap<>();
        nonEmptySetValueMap.put("maxActive", 10);
        nonEmptySetMethodMap.put("maxActive", BeeDataSourceConfig.class.getDeclaredMethod("setMaxActive", int.class));
        setPropertiesValue(bean, null, null);//null null
        setPropertiesValue(bean, null, emptySetValueMap);//null,empty
        setPropertiesValue(bean, null, nonEmptySetValueMap);//null,not empty
        setPropertiesValue(bean, emptySetMethodMap, null);//empty ---> null
        setPropertiesValue(bean, emptySetMethodMap, emptySetValueMap);//empty ---> empty
        setPropertiesValue(bean, emptySetMethodMap, nonEmptySetValueMap);//empty ---> not empty
        setPropertiesValue(bean, nonEmptySetMethodMap, null);//not empty ---> null
        setPropertiesValue(bean, nonEmptySetMethodMap, emptySetValueMap);//not empty ---> empty
        setPropertiesValue(bean, nonEmptySetMethodMap, nonEmptySetValueMap);//not empty ---> not empty

        Map<String, String> valueMap = new HashMap<>(3);
        valueMap.put("maxActive", "10");
        valueMap.put("max-active", "20");
        valueMap.put("max_active", "30");
        setPropertiesValue(bean, valueMap);
        Assert.assertEquals(10, bean.getMaxActive());

        valueMap.remove("maxActive");
        setPropertiesValue(bean, valueMap);
        Assert.assertEquals(20, bean.getMaxActive());

        valueMap.remove("max-active");
        setPropertiesValue(bean, valueMap);
        Assert.assertEquals(30, bean.getMaxActive());

    }

    public void testClassInstanceCreation() throws Exception {
        Class<?> clazz = MockBlockPoolImplementation2.class;
        BeeConnectionPool pool1 = (BeeConnectionPool) createClassInstance(clazz, (Class<?>) null, "pool");
        BeeConnectionPool pool2 = (BeeConnectionPool) createClassInstance(clazz, BeeConnectionPool.class, "pool");
        Assert.assertNotNull(pool1);
        Assert.assertNotNull(pool2);

        try {
            createClassInstance(null, BeeConnectionPool.class, "pool");
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("Bean class can't be null"));
        }

        try {
            createClassInstance(SetTestBean.class, BeeConnectionPool.class, "pool");
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("Bean class can't be abstract"));
        }

        try {
            createClassInstance(SetTestBean2.class, BeeConnectionPool.class, "pool");
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("Not defined public constructor in bean class"));
        }

        createClassInstance(clazz, (Class<?>[]) null, "pool");
        createClassInstance(clazz, new Class[0], "pool");
        createClassInstance(clazz, new Class[]{null, null, null}, "pool");
        createClassInstance(clazz, new Class[]{BeeConnectionPool.class, null, null}, "pool");
        try {
            createClassInstance(clazz, new Class[]{Number.class, null, String.class}, "pool");
        } catch (BeanException e) {
            Assert.assertTrue(e.getMessage().contains("which must extend from one of type"));
        }
    }

    public void testSetPropertiesValue() throws Exception {
        Map<String, Object> localConnectProperties = new HashMap<>(100);
        localConnectProperties.put("nullTxt", " ");
        localConnectProperties.put("string", 1L);
        localConnectProperties.put("workLongTime", 1000L);
        localConnectProperties.put("defaultCatalog", "MyDB");
        localConnectProperties.put("char1", "char1");
        localConnectProperties.put("char2", "char2");
        localConnectProperties.put("boolean1", "true");
        localConnectProperties.put("boolean2", "false");
        localConnectProperties.put("byte1", "1");
        localConnectProperties.put("byte2", "10");
        localConnectProperties.put("short1", "1");
        localConnectProperties.put("short2", "10");
        localConnectProperties.put("int1", "1");
        localConnectProperties.put("int2", "10");
        localConnectProperties.put("long1", "1");
        localConnectProperties.put("long2", "10");
        localConnectProperties.put("float1", "1");
        localConnectProperties.put("float2", "10");
        localConnectProperties.put("double1", "1");
        localConnectProperties.put("double2", "10");
        localConnectProperties.put("bigInteger", "1");
        localConnectProperties.put("bigDecimal", "10.0");
        localConnectProperties.put("clazz", "java.lang.String");
        localConnectProperties.put("URL", "http://localhost:8080/testdb");
        localConnectProperties.put("intArray", "1");
        localConnectProperties.put("collection", "java.util.ArrayList");
        localConnectProperties.put("map", "java.util.HashMap");
        MockObjectForPropertiesSet bean = new MockObjectForPropertiesSet();
        setPropertiesValue(bean, localConnectProperties);
        Assert.assertNotNull(bean.getURL());
        Assert.assertNotNull(bean.getMap());
        Assert.assertNotNull(bean.getCollection());
    }

    private static class SetTestBean2 {
    }

    private static abstract class SetTestBean {
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public void nameSet(String name) {
            this.name = name;
        }

        public void set() {
            //do nothing
        }

        public void set(String name) {
            this.name = name;
        }
    }
}

