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
import org.slf4j.LoggerFactory;
import org.stone.tools.CommonUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.locks.LockSupport;

import static org.stone.tools.BeanUtil.setAccessible;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class TestUtil {
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

    public static Object invokeMethod(final Object ob, final String methodName) throws Exception {
        return invokeMethod(ob, ob.getClass(), methodName);
    }

    public static Object invokeMethod(final Object ob, final Class clazz, final String methodName) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName);
        setAccessible(method);
        return method.invoke(ob);
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
