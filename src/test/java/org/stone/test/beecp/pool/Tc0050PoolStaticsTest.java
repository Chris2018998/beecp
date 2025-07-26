/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.driver.MockDriver;
import org.stone.test.beecp.driver.MockXaDataSource;
import org.stone.test.beecp.objects.MockBlockPoolImplementation;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockDriverConnectionFactory;
import org.stone.test.beecp.objects.MockObjectForPropertiesSet;
import org.stone.tools.exception.BeanException;

import javax.sql.XAConnection;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.beecp.pool.ConnectionPoolStatics.loadDriver;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.test.beecp.config.DsConfigFactory.MOCK_URL;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;
import static org.stone.tools.BeanUtil.*;

/**
 * @author Chris Liao
 */
public class Tc0050PoolStaticsTest {

    @Test
    public void testInvalidDriverClass() {
        try {
            loadDriver("org.stone.beecp.mock.MockDriver2");
            Assertions.fail("testInvalidDriverClass");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create jdbc driver by class:"));
        }
    }

    @Test
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

    @Test
    public void testOnDummyCommonDataSource() {
        BeeDataSourceConfig config = createDefault();
        config.setConnectionFactoryClass(MockDriverConnectionFactory.class);
        BeeDataSource ds = new BeeDataSource(config);
        try {
            ds.getLogWriter();
            fail("Failed to test getLogWriter on dummy ds");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.setLogWriter(null);
            fail("Failed to test setLogWriter on dummy ds");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.getLoginTimeout();
            fail("Failed to test getLoginTimeout on dummy ds");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.setLoginTimeout(10);
            fail("Failed to test setLoginTimeout on dummy ds");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Not supported"));
        }
        try {
            ds.getParentLogger();
            fail("Failed to test getParentLogger on dummy ds");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Not supported"));
        }
    }

    @Test
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
        Assertions.assertTrue(resultSet.isClosed());
        Assertions.assertTrue(resultSet.toString().contains("ResultSet has been closed"));

        try {
            resultSet.getString(0);
            Assertions.fail("testOnCloseMethod");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after resultSet closed"));
        }

        //test on closed statement
        Assertions.assertTrue(statement.toString().contains("Statement has been closed"));
        Assertions.assertTrue(statement.isClosed());

        try {
            statement.executeQuery("select * from Test_User");
            Assertions.fail("testOnCloseMethod");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        //test on closed connection
        Assertions.assertTrue(con.toString().contains("Connection has been closed"));
        Assertions.assertTrue(con.isClosed());

        try {
            con.createStatement();
            Assertions.fail("testOnCloseMethod");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }
    }

    @Test
    public void testSetMethodsLookup() {
        Map<String, Method> map = getClassSetMethodMap(SetTestBean.class);
        Assertions.assertEquals(1, map.size());
    }

    @Test
    public void testPropertyValueGet() {
        Map<String, String> map1 = new HashMap<>(4);
        map1.put("maxActive", "5");
        map1.put("max-active", "10");
        map1.put("max_active", "20");
        map1.put("URL", "http://localhost:8080/testdb");
        Assertions.assertEquals("http://localhost:8080/testdb", getPropertyValue(map1, "URL"));
        Assertions.assertEquals("http://localhost:8080/testdb", getPropertyValue(map1, "uRL"));
        Assertions.assertEquals("5", getPropertyValue(map1, "maxActive"));
        map1.remove("maxActive");
        Assertions.assertEquals("10", getPropertyValue(map1, "maxActive"));
        map1.remove("max-active");
        Assertions.assertEquals("20", getPropertyValue(map1, "maxActive"));
        map1.remove("max_active");
        Assertions.assertNull(getPropertyValue(map1, "maxActive"));
    }

    @Test
    public void testOnPropertiesValueSet() throws Exception {
        try {
            setPropertiesValue(null, null);
            Assertions.fail("testOnPropertiesValueSet");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("Bean can't be null"));
        }

        try {
            setPropertiesValue(null, null, null);
            Assertions.fail("testOnPropertiesValueSet");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("Bean can't be null"));
        }

        BeeDataSourceConfig bean = new BeeDataSourceConfig();
        Map<String, Method> emptySetMethodMap = new HashMap<>(0);
        Map<String, Object> emptySetValueMap = new HashMap<>(0);
        Map<String, Method> nonEmptySetMethodMap = new HashMap<>(1);
        Map<String, Object> nonEmptySetValueMap = new HashMap<>(1);
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
        Assertions.assertEquals(10, bean.getMaxActive());

        valueMap.remove("maxActive");
        setPropertiesValue(bean, valueMap);
        Assertions.assertEquals(20, bean.getMaxActive());

        valueMap.remove("max-active");
        setPropertiesValue(bean, valueMap);
        Assertions.assertEquals(30, bean.getMaxActive());
    }

    @Test
    public void testClassInstanceCreation() throws Exception {
        Class<?> clazz = MockBlockPoolImplementation.class;
        BeeConnectionPool pool1 = (BeeConnectionPool) createClassInstance(clazz, (Class<?>) null, "pool");
        BeeConnectionPool pool2 = (BeeConnectionPool) createClassInstance(clazz, BeeConnectionPool.class, "pool");
        Assertions.assertNotNull(pool1);
        Assertions.assertNotNull(pool2);

        try {
            createClassInstance((Class<?>) null, BeeConnectionPool.class, "pool");
            Assertions.fail("testClassInstanceCreation");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("Bean class can't be null"));
        }

        try {
            createClassInstance(SetTestBean.class, BeeConnectionPool.class, "pool");
            Assertions.fail("testClassInstanceCreation");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("Bean class can't be abstract"));
        }

        try {
            createClassInstance(SetTestBean2.class, BeeConnectionPool.class, "pool");
            Assertions.fail("testClassInstanceCreation");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("Bean class must be public"));
        }

        createClassInstance(clazz, (Class<?>[]) null, "pool");
        createClassInstance(clazz, new Class[0], "pool");
        createClassInstance(clazz, new Class[]{null, null, null}, "pool");
        createClassInstance(clazz, new Class[]{BeeConnectionPool.class, null, null}, "pool");
        try {
            createClassInstance(clazz, new Class[]{Number.class, null, String.class}, "pool");
            Assertions.fail("testClassInstanceCreation");
        } catch (BeanException e) {
            Assertions.assertTrue(e.getMessage().contains("which must extend from one of type"));
        }
    }

    @Test
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
        localConnectProperties.put("collection", "1");
        localConnectProperties.put("map", "java.util.HashMap");
        MockObjectForPropertiesSet bean = new MockObjectForPropertiesSet();
        setPropertiesValue(bean, localConnectProperties);
        Assertions.assertNotNull(bean.getURL());
        Assertions.assertNotNull(bean.getMap());
        Assertions.assertNotNull(bean.getCollection());
    }

    @Test
    public void testSetArray() throws Exception {
        Map<String, Object> localConnectProperties = new HashMap<>(10);
        localConnectProperties.put("hosts", "server1,server2,server3");
        localConnectProperties.put("ports", "80,90,100");
        localConnectProperties.put("passwordChars", "A,B,C");

        localConnectProperties.put("intArray", new int[]{0, 1, 2});
        localConnectProperties.put("intArray2", new Integer[]{0, 1, 2});

        MockObjectForPropertiesSet bean = new MockObjectForPropertiesSet();
        setPropertiesValue(bean, localConnectProperties);

        Assertions.assertNotNull(bean.getHosts());
        Assertions.assertEquals("server1", bean.getHosts()[0]);
        Assertions.assertEquals("server2", bean.getHosts()[1]);
        Assertions.assertEquals("server3", bean.getHosts()[2]);

        Assertions.assertNotNull(bean.getPorts());
        Assertions.assertEquals(80, bean.getPorts()[0]);
        Assertions.assertEquals(90, bean.getPorts()[1]);
        Assertions.assertEquals(100, bean.getPorts()[2]);

        Assertions.assertNotNull(bean.getPasswordChars());
        Assertions.assertEquals('A', bean.getPasswordChars()[0]);
        Assertions.assertEquals('B', bean.getPasswordChars()[1]);
        Assertions.assertEquals('C', bean.getPasswordChars()[2]);

        Assertions.assertEquals(0, bean.getIntArray()[0]);
        Assertions.assertEquals(1, bean.getIntArray()[1]);
        Assertions.assertEquals(2, bean.getIntArray()[2]);
        Assertions.assertEquals(Integer.valueOf(0), bean.getIntArray2()[0]);
        Assertions.assertEquals(Integer.valueOf(1), bean.getIntArray2()[1]);
        Assertions.assertEquals(Integer.valueOf(2), bean.getIntArray2()[2]);
    }

    @Test
    public void testSetList() throws Exception {
        Map<String, Object> localConnectProperties = new HashMap<>(10);
        localConnectProperties.put("hostNameList", "server1,server2,server3");
        localConnectProperties.put("hostPortList", "80,90,100");

        MockObjectForPropertiesSet bean = new MockObjectForPropertiesSet();
        setPropertiesValue(bean, localConnectProperties);

        List<String> hostNameList = bean.getHostNameList();
        Assertions.assertNotNull(hostNameList);
        Assertions.assertEquals("server1", hostNameList.get(0));
        Assertions.assertEquals("server2", hostNameList.get(1));
        Assertions.assertEquals("server3", hostNameList.get(2));

        List<Integer> hostPortList = bean.getHostPortList();
        Assertions.assertNotNull(hostPortList);
        Assertions.assertEquals(Integer.valueOf("80"), hostPortList.get(0));
        Assertions.assertEquals(Integer.valueOf("90"), hostPortList.get(1));
        Assertions.assertEquals(Integer.valueOf("100"), hostPortList.get(2));
    }

    @Test
    public void testValidateMethod() throws Exception {
        MockConnectionProperties properties = new MockConnectionProperties();
        properties.enableExceptionOnMethod("isValid");
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(properties);
        BeeDataSourceConfig config = createDefault();
        config.setConnectionFactory(factory);
        config.setInitialSize(1);
        config.setMaxActive(1);


        try {//1: success test
            FastConnectionPool pool1 = new FastConnectionPool();
            pool1.init(config);
        } catch (SQLException e) {
            Assertions.fail("testValidateMethod");
        }

        try {//2: fail test
            properties.enableExceptionOnMethod("setAutoCommit");
            properties.setMockException1(new SQLException("Failed to setAutoCommit"));
            FastConnectionPool pool2 = new FastConnectionPool();
            pool2.init(config);
            Assertions.fail("testValidateMethod");
        } catch (SQLException e) {
            Assertions.assertEquals("Failed to setAutoCommit(false)", e.getMessage());
        }
        try {//3: not execute setAutoCommit(false)
            FastConnectionPool pool3 = new FastConnectionPool();
            config.setDefaultAutoCommit(false);
            pool3.init(config);
        } catch (SQLException e) {
            Assertions.fail("testValidateMethod");
        }


        //3: fail test
        config.setDefaultAutoCommit(true);
        properties.disableExceptionOnMethod("setAutoCommit");
        properties.enableExceptionOnMethod("setQueryTimeout");
        FastConnectionPool pool3 = new FastConnectionPool();
        LogCollector logCollector = LogCollector.startLogCollector();
        config.setPrintRuntimeLog(true);
        pool3.init(config);
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("driver not support 'queryTimeout'"));

        try {//4: fail test
            properties.disableExceptionOnMethod("setAutoCommit,setQueryTimeout");
            properties.enableExceptionOnMethod("execute");
            FastConnectionPool pool4 = new FastConnectionPool();
            pool4.init(config);
            Assertions.fail("testValidateMethod");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("Invalid test sql"));
        }
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

