/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.base;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.tools.CommonUtil;

import javax.sql.XAConnection;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.tools.BeanUtil.setAccessible;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class TestUtil {
    public static final long Wait_Time = 100L;
    public static final TimeUnit Wait_TimeUnit = TimeUnit.MILLISECONDS;
    private static final Logger log = LoggerFactory.getLogger(TestUtil.class);
    private static StoneLogAppender logAppender = null;

    public static StoneLogAppender getStoneLogAppender() {
        if (logAppender != null) return logAppender;
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger loggerImpl : loggerContext.getLoggerList()) {
            Appender appender = loggerImpl.getAppender("console");
            if (appender instanceof StoneLogAppender) {
                logAppender = (StoneLogAppender) appender;
                break;
            }
        }
        return logAppender;
    }

    public static File getClassPathFileAbsolutePath(String classFileName) throws Exception {
        Class<?> selfClass = CommonUtil.class;
        URL fileUrl = selfClass.getClassLoader().getResource(classFileName);
        return fileUrl != null ? new File(fileUrl.toURI()) : null;
    }

    public static Object getFieldValue(final Object ob, final String fieldName) throws Exception {
        return getFieldValue(ob, ob.getClass(), fieldName);
    }

    public static Object getFieldValue(final Object ob, final Class clazz, final String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        setAccessible(field);
        return field.get(ob);
    }

    public static Object invokeMethod2(final Object ob, final String methodName) throws Exception {
        return invokeMethod2(ob, ob.getClass(), methodName);
    }

    public static Object invokeMethod2(final Object ob, final Class clazz, final String methodName) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName);
        setAccessible(method);
        return method.invoke(ob);
    }

    public static Object invokeMethod(Object ob, String methodName) {
        try {
            Method method = ob.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(ob);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void assertError(String message) {
        throw new AssertionError(message);
    }

    public static void assertError(String message, Object expect, Object current) {
        if (!objectEquals(expect, current))
            throw new AssertionError(String.format(message, expect, current));
    }

    public static boolean containsMessage(Throwable e, String msg) {
        String errorInfo = e.getMessage();
        return errorInfo != null && errorInfo.contains(msg);
    }

    public static void setFieldValue(Object ob, String fieldName, Object value) throws Exception {
        Field field = ob.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(ob, value);
    }

//    public static Object getFieldValue(Object ob, String fieldName) throws Exception {
//        Field field = ob.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return field.get(ob);
//    }
//
//    public static Object invokeMethod(Object ob, String methodName) {
//        try {
//            Method method = ob.getClass().getMethod(methodName);
//            method.setAccessible(true);
//            return method.invoke(ob);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing resultSet:", e);
        }
    }

    public static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing statement:", e);
        }
    }

    public static void oclose(Connection c) {
        try {
            c.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing connection:", e);
        }
    }

    public static void oclose(XAConnection c) {
        try {
            c.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing resultSet:", e);
        }
    }

    public static boolean joinUtilTerminated(Thread thread) {
        for (; ; ) {
            Thread.State curState = thread.getState();
            if (curState == Thread.State.TERMINATED) {
                return true;
            } else {
                LockSupport.parkNanos(5L);
            }
        }
    }

    public static boolean joinUtilWaiting(Thread thread) {
        for (; ; ) {
            Thread.State curState = thread.getState();
            if (curState == Thread.State.WAITING || curState == Thread.State.TIMED_WAITING) {
                return true;
            } else if (curState == Thread.State.TERMINATED) {
                return false;
            } else {
                LockSupport.parkNanos(5L);
            }
        }
    }
}
