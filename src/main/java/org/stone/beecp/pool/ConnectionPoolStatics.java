/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.exception.TestSqlExecFailedException;

import javax.sql.CommonDataSource;
import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

/**
 * Pool Static Center
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConnectionPoolStatics {
    public static final Logger CommonLog = LoggerFactory.getLogger(ConnectionPoolStatics.class);
    //properties configuration separator
    public static final String Separator_MiddleLine = "-";
    //properties configuration separator
    public static final String Separator_UnderLine = "_";
    //transaction manager jndi name in configuration
    public static final String CONFIG_TM_JNDI = "transactionManagerName";
    //connect properties for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP = "connectProperties";
    //connect properties count for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP_SIZE = "connectProperties.size";
    //connect properties prefix for driver or driver dataSource
    public static final String CONFIG_CONNECT_PROP_KEY_PREFIX = "connectProperties.";
    //sql exception fatal code
    public static final String CONFIG_SQL_EXCEPTION_CODE = "sqlExceptionCodeList";
    //sql exception fatal state
    public static final String CONFIG_SQL_EXCEPTION_STATE = "sqlExceptionStateList";
    //sql exception fatal state
    public static final String CONFIG_CONFIG_PRINT_EXCLUSION_LIST = "configPrintExclusionList";

    //dummy impl on CommonDataSource
    public static final CommonDataSource Dummy_CommonDataSource = new CommonDataSource() {
        public final PrintWriter getLogWriter() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public final void setLogWriter(PrintWriter out) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public int getLoginTimeout() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public final void setLoginTimeout(int seconds) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }

        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Not supported");
        }
    };

    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_STARTING = 1;
    static final int POOL_READY = 2;
    static final int POOL_CLOSED = 3;
    static final int POOL_CLEARING = 4;
    //connection state
    static final int CON_IDLE = 0;
    static final int CON_USING = 1;
    static final int CON_CLOSED = 2;
    //pool thread state
    static final int THREAD_WORKING = 0;
    static final int THREAD_WAITING = 1;
    static final int THREAD_EXIT = 2;
    //Connection reset pos in array
    static final int PS_AUTO = 0;
    static final int PS_TRANS = 1;
    static final int PS_READONLY = 2;
    static final int PS_CATALOG = 3;
    static final int PS_SCHEMA = 4;
    static final int PS_NETWORK = 5;
    //
    static final String DESC_RM_INIT = "init";
    static final String DESC_RM_BAD = "bad";
    static final String DESC_RM_ABORT = "abort";
    static final String DESC_RM_IDLE = "idle";
    static final String DESC_RM_CLOSED = "closed";
    static final String DESC_RM_CLEAR = "clear";
    static final String DESC_RM_DESTROY = "destroy";
    //***************************************************************************************************************//
    //                                1: jdbc global proxy (3)                                                       //
    //***************************************************************************************************************//
    static final Connection CLOSED_CON = (Connection) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{Connection.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("isClosed".equals(method.getName())) {
                        return Boolean.TRUE;
                    } else {
                        throw new SQLException("No operations allowed after connection closed");
                    }
                }
            }
    );
    static final CallableStatement CLOSED_CSTM = (CallableStatement) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{CallableStatement.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("isClosed".equals(method.getName())) {
                        return Boolean.TRUE;
                    } else {
                        throw new SQLException("No operations allowed after statement closed");
                    }
                }
            }
    );
    static final ResultSet CLOSED_RSLT = (ResultSet) Proxy.newProxyInstance(
            ConnectionPoolStatics.class.getClassLoader(),
            new Class[]{ResultSet.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("isClosed".equals(method.getName())) {
                        return Boolean.TRUE;
                    } else {
                        throw new SQLException("No operations allowed after resultSet closed");
                    }
                }
            }
    );

    //***************************************************************************************************************//
    //                               2: JDBC close methods(4)                                                        //
    //***************************************************************************************************************//
    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing resultSet", e);
        }
    }

    public static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing statement", e);
        }
    }

    public static void oclose(Connection c) {
        try {
            c.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing connection", e);
        }
    }

    public static void oclose(XAConnection c) {
        try {
            c.close();
        } catch (Throwable e) {
            CommonLog.debug("Warning:Error at closing connection", e);
        }
    }

    //***************************************************************************************************************//
    //                               3: JDBC body auto fill by javassist methods(2)                                  //
    //***************************************************************************************************************//
    static ProxyConnectionBase createProxyConnection(PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    static ResultSet createProxyResultSet(ResultSet raw, ProxyStatementBase owner, PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }

    //***************************************************************************************************************//
    //                               4: JDBC other help methods(3)                                                   //
    //***************************************************************************************************************//
    public static Driver loadDriver(String driverClassName) throws BeeDataSourceConfigException {
        try {
            return (Driver) Class.forName(driverClassName).newInstance();
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException("Failed to create jdbc driver by class:" + driverClassName, e);
        }
    }

    static void checkJdbcProxyClass() {
        String[] classNames = {
                "org.stone.beecp.pool.Borrower",
                "org.stone.beecp.pool.PooledConnection",
                "org.stone.beecp.pool.ProxyConnection",
                "org.stone.beecp.pool.ProxyStatement",
                "org.stone.beecp.pool.ProxyPsStatement",
                "org.stone.beecp.pool.ProxyCsStatement",
                "org.stone.beecp.pool.ProxyDatabaseMetaData",
                "org.stone.beecp.pool.ProxyResultSet"};
        try {
            ClassLoader loader = ConnectionPoolStatics.class.getClassLoader();
            for (String className : classNames)
                Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Jdbc proxy classes missed", e);
        }
    }

    static boolean validateTestSql(String poolName, Connection rawCon, String testSql, int validTestTimeout, boolean isDefaultAutoCommit) throws SQLException {
        boolean changed = false;
        Statement st = null;
        try {
            //step1: setAutoCommit to 'false'
            if (isDefaultAutoCommit) {
                try {
                    rawCon.setAutoCommit(false);
                    changed = true;
                } catch (Throwable e) {
                    throw new SQLException("Failed to setAutoCommit(false)", e);
                }
            }

            //step2: create statement and test 'QueryTimeout'
            st = rawCon.createStatement();
            boolean supportQueryTimeout = true;
            try {
                st.setQueryTimeout(validTestTimeout);
            } catch (Throwable e) {
                supportQueryTimeout = false;
                CommonLog.warn("BeeCP({})driver not support 'queryTimeout'", poolName, e);
            }

            //step3: execute test sql
            try {
                st.execute(testSql);
            } catch (Throwable e) {
                throw new TestSqlExecFailedException("Invalid test sql:" + testSql, e);
            } finally {
                rawCon.rollback();//why? maybe store procedure in test sql
            }

            return supportQueryTimeout;
        } finally {
            if (st != null) oclose(st);
            if (changed) rawCon.setAutoCommit(true);//reset to default
        }
    }

    //***************************************************************************************************************//
    //                               6: configuration read methods(5)                                                //
    //***************************************************************************************************************//

    /**
     * find-out all set methods and put to map with method names,for example:
     * method:setMaxActive, map.put('MaxActive',method)
     *
     * @param beanClass set methods owner
     * @return methods map
     */
    public static Map<String, Method> getClassSetMethodMap(Class beanClass) {
        Method[] methods = beanClass.getMethods();
        HashMap<String, Method> methodMap = new LinkedHashMap<String, Method>(methods.length);
        for (Method method : methods) {
            String methodName = method.getName();
            if (method.getParameterTypes().length == 1 && methodName.startsWith("set") && methodName.length() > 3)
                methodMap.put(methodName.substring(3), method);
        }
        return methodMap;
    }

    /**
     * get config item value by property name,which support three format:
     * 1:hump,example:maxActive
     * 2:middle line,example: max-active
     * 3:middle line,example: max_active
     *
     * @param properties   configuration list
     * @param propertyName config item name
     * @return configuration item value
     */
    public static String getPropertyValue(Properties properties, final String propertyName) {
        String value = readPropertyValue(properties, propertyName);
        if (value != null) return value;

        String newPropertyName = propertyName.substring(0, 1).toLowerCase(Locale.US) + propertyName.substring(1);

        value = readPropertyValue(properties, newPropertyName);
        if (value != null) return value;

        value = readPropertyValue(properties, propertyNameToFieldId(newPropertyName, Separator_MiddleLine));
        if (value != null) return value;

        return readPropertyValue(properties, propertyNameToFieldId(newPropertyName, Separator_UnderLine));
    }

    /**
     * get config item value by property name,which support three format:
     * 1:hump,example:maxActive
     * 2:middle line,example: max-active
     * 3:middle line,example: max_active
     *
     * @param valueMap     configuration list
     * @param propertyName config item name
     * @return configuration item value
     */
    private static Object getFieldValue(Map<String, Object> valueMap, final String propertyName) {
        Object value = valueMap.get(propertyName);
        if (value != null) return value;

        String newPropertyName = propertyName.substring(0, 1).toLowerCase(Locale.US) + propertyName.substring(1);
        value = valueMap.get(newPropertyName);
        if (value != null) return value;

        value = valueMap.get(propertyNameToFieldId(newPropertyName, Separator_MiddleLine));
        if (value != null) return value;

        return valueMap.get(propertyNameToFieldId(newPropertyName, Separator_UnderLine));
    }

    public static String propertyNameToFieldId(String property, String separator) {
        char[] chars = property.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                sb.append(separator).append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readPropertyValue(Properties configProperties, String propertyName) {
        String value = configProperties.getProperty(propertyName, null);
        if (value != null) {
            CommonLog.info("beecp.{}={}", propertyName, value);
            return value.trim();
        } else {
            return null;
        }
    }

    //***************************************************************************************************************//
    //                               7: bean property set methods(3)                                                 //
    //***************************************************************************************************************//
    public static void setPropertiesValue(Object bean, Map<String, Object> valueMap) throws BeeDataSourceConfigException {
        if (bean == null) throw new BeeDataSourceConfigException("Bean can't be null");
        setPropertiesValue(bean, getClassSetMethodMap(bean.getClass()), valueMap);
    }

    public static void setPropertiesValue(Object bean, Map<String, Method> setMethodMap, Map<String, Object> valueMap) throws BeeDataSourceConfigException {
        if (bean == null) throw new BeeDataSourceConfigException("Bean can't be null");
        if (setMethodMap == null || setMethodMap.isEmpty() || valueMap == null || valueMap.isEmpty()) return;
        for (Map.Entry<String, Method> entry : setMethodMap.entrySet()) {
            String propertyName = entry.getKey();
            Method setMethod = entry.getValue();

            Object setValue = getFieldValue(valueMap, propertyName);
            if (setValue != null) {
                Class type = setMethod.getParameterTypes()[0];
                try {
                    //1:convert config value to match type of set method
                    setValue = convert(propertyName, setValue, type);
                } catch (BeeDataSourceConfigException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new BeeDataSourceConfigException("Failed to convert config value to property(" + propertyName + ")type:" + type.getName(), e);
                }

                try {//2:inject value by set method
                    setMethod.invoke(bean, setValue);
                } catch (IllegalAccessException e) {
                    throw new BeeDataSourceConfigException("Failed to inject config value to property:" + propertyName, e);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause != null) {
                        throw new BeeDataSourceConfigException("Failed to inject config value to property:" + propertyName, cause);
                    } else {
                        throw new BeeDataSourceConfigException("Failed to inject config value to property:" + propertyName, e);
                    }
                }
            }
        }
    }

    private static Object convert(String propName, Object setValue, Class type) {
        if (type.isInstance(setValue)) {
            return setValue;
        } else if (type == String.class) {
            return setValue.toString();
        }

        String text = setValue.toString();
        text = text.trim();
        if (text.length() == 0) return null;
        if (type == char.class || type == Character.class) {
            return text.toCharArray()[0];
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(text);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(text);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(text);
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(text);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(text);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(text);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(text);
        } else if (type == BigInteger.class) {
            return new BigInteger(text);
        } else if (type == BigDecimal.class) {
            return new BigDecimal(text);
        } else if (type == Class.class) {
            try {
                return Class.forName(text);
            } catch (ClassNotFoundException e) {
                throw new BeeDataSourceConfigException("Not found class:" + text);
            }
        } else if (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {//do nothing
            return null;
        } else {
            try {
                Object objInstance = Class.forName(text).newInstance();
                if (!type.isInstance(objInstance))
                    throw new BeeDataSourceConfigException("Config value can't mach property(" + propName + ")type:" + type.getName());
                return objInstance;
            } catch (BeeDataSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeDataSourceConfigException("Failed to create property(" + propName + ")value by type:" + text, e);
            }
        }
    }

    //***************************************************************************************************************//
    //                               8: class check(3)                                                               //
    //***************************************************************************************************************//
    //check subclass,if failed,then return error message;
    public static Object createClassInstance(Class objectClass, Class parentClass, String objectClassType) throws Exception {
        return createClassInstance(objectClass, parentClass != null ? new Class[]{parentClass} : null, objectClassType);
    }

    //check subclass,if failed,then return error message;
    public static Object createClassInstance(Class objectClass, Class[] parentClasses, String objectClassType) throws Exception {
        //1:check class abstract modifier
        if (Modifier.isAbstract(objectClass.getModifiers()))
            throw new BeeDataSourceConfigException("Cant't create a instance on abstract class[" + objectClass.getName() + "],creation category[" + objectClassType + "]");
        //2:check class public modifier
        if (!Modifier.isPublic(objectClass.getModifiers()))
            throw new BeeDataSourceConfigException("Cant't create a instance on non-public class[" + objectClass.getName() + "],creation category[" + objectClassType + "]");
        //3:check extension
        boolean isSubClass = false;//pass when match one
        if (parentClasses != null && parentClasses.length > 0) {
            for (Class parentClass : parentClasses) {
                if (parentClass != null && parentClass.isAssignableFrom(objectClass)) {
                    isSubClass = true;
                    break;
                }
            }
            if (!isSubClass)
                throw new BeeDataSourceConfigException("Cant't create a instance on class[" + objectClass.getName() + "]which must extend from one of type[" + getClassName(parentClasses) + "]at least,creation category[" + objectClassType + "]");
        }
        //4:check class constructor
        return objectClass.getConstructor().newInstance();
    }

    private static String getClassName(Class[] classes) {
        StringBuilder buf = new StringBuilder(classes.length * 10);
        for (Class clazz : classes) {
            if (buf.length() > 0) buf.append(",");
            buf.append(clazz.getName());
        }
        return buf.toString();
    }
}