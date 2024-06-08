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
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class TestUtil {
    public static final long Wait_Time = 100L;
    public static final TimeUnit Wait_TimeUnit = TimeUnit.MILLISECONDS;
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

    public static Object getFieldValue(Object ob, String fieldName) throws Exception {
        Field field = ob.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(ob);
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
