/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logger wrapper on SLF logger for stone project.
 *
 * @author Chris Liao
 */
public class LogPrinter {
    public static final LogPrinter DefaultLogPrinter = new LogPrinter(LoggerFactory.getLogger(LogPrinter.class));

    protected final Logger logger;
    private boolean isEnabled = true;

    public LogPrinter(Logger logger) {
        if (logger == null) throw new IllegalArgumentException("Logger cannot be null");
        this.logger = logger;
    }

    /*********************************************Static methods ******************************************************/
    public static LogPrinter getLogPrinter(Class<?> targetClass, boolean outputLogs) {
        if (targetClass == null) return NotOutputLogPrinter.SingleInstance;
        return outputLogs ? new UnSwitchableLogPrinter(LoggerFactory.getLogger(targetClass)) : NotOutputLogPrinter.SingleInstance;
    }

    public static LogPrinter getSwitchableLogPrinter(Class<?> targetClass) {
        return new LogPrinter(LoggerFactory.getLogger(targetClass));
    }

    /*****************************************************************************************************************/
    public boolean isEnableLogOutput() {
        return isEnabled;
    }

    public void enableLogOutput(boolean outputLogs) {
        this.isEnabled = outputLogs;
    }

    public void debug(String s) {
        if (isEnabled) logger.debug(s);
    }

    public void debug(String s, Object... var2) {
        if (isEnabled) logger.debug(s, var2);
    }

    public void debug(String s, Throwable e) {
        if (isEnabled) logger.debug(s, e);
    }

    public void debug(String s, Throwable e, Object... var2) {
        if (isEnabled) logger.debug(s, e, var2);
    }

    public void info(String s) {
        if (isEnabled) logger.info(s);
    }

    public void info(String s, Object... var2) {
        if (isEnabled) logger.info(s, var2);
    }

    public void info(String s, Throwable e) {
        if (isEnabled) logger.info(s, e);
    }

    public void info(String s, Throwable e, Object... var2) {
        if (isEnabled) logger.info(s, e, var2);
    }

    public void warn(String s) {
        if (isEnabled) logger.warn(s);
    }

    public void warn(String s, Object... var2) {
        if (isEnabled) logger.warn(s, var2);
    }

    public void warn(String s, Throwable e) {
        if (isEnabled) logger.warn(s, e);
    }

    public void warn(String s, Throwable e, Object... var2) {
        if (isEnabled) logger.warn(s, e, var2);
    }

    public void error(String s) {
        if (isEnabled) logger.error(s);
    }

    public void error(String s, Object... var2) {
        if (isEnabled) logger.error(s, var2);
    }

    public void error(String s, Throwable e) {
        if (isEnabled) logger.error(s, e);
    }

    public void error(String s, Throwable e, Object... var2) {
        if (isEnabled) logger.error(s, e, var2);
    }


    //2: Switch not supported
    static final class UnSwitchableLogPrinter extends LogPrinter {
        public UnSwitchableLogPrinter(Logger logger) {
            super(logger);
        }

        public boolean isEnableLogOutput() {
            return true;
        }

        public void debug(String s) {
            logger.debug(s);
        }

        public void debug(String s, Object... var2) {
            logger.debug(s, var2);
        }

        public void debug(String s, Throwable e) {
            logger.debug(s, e);
        }

        public void debug(String s, Throwable e, Object... var2) {
            logger.debug(s, e, var2);
        }

        public void info(String s) {
            logger.info(s);
        }

        public void info(String s, Object... var2) {
            logger.info(s, var2);
        }

        public void info(String s, Throwable e) {
            logger.info(s, e);
        }

        public void info(String s, Throwable e, Object... var2) {
            logger.info(s, e, var2);
        }

        public void warn(String s) {
            logger.warn(s);
        }

        public void warn(String s, Object... var2) {
            logger.warn(s, var2);
        }

        public void warn(String s, Throwable e) {
            logger.warn(s, e);
        }

        public void warn(String s, Throwable e, Object... var2) {
            logger.warn(s, e, var2);
        }

        public void error(String s) {
            logger.error(s);
        }

        public void error(String s, Object... var2) {
            logger.error(s, var2);
        }

        public void error(String s, Throwable e) {
            logger.error(s, e);
        }

        public void error(String s, Throwable e, Object... var2) {
            logger.error(s, e, var2);
        }
    }

    //3: No logs output
    static final class NotOutputLogPrinter extends LogPrinter {
        static final NotOutputLogPrinter SingleInstance = new NotOutputLogPrinter();

        NotOutputLogPrinter() {
            super(LoggerFactory.getLogger(LogPrinter.class));
        }

        public boolean isEnableLogOutput() {
            return false;
        }

        public void debug(String s) {
        }

        public void debug(String s, Object... var2) {
        }

        public void debug(String s, Throwable e) {
        }

        public void debug(String s, Throwable e, Object... var2) {
        }

        public void info(String s) {
        }

        public void info(String s, Object... var2) {
        }

        public void info(String s, Throwable e) {
        }

        public void info(String s, Throwable e, Object... var2) {
        }

        public void warn(String s) {
        }

        public void warn(String s, Object... var2) {
        }

        public void warn(String s, Throwable e) {
        }

        public void warn(String s, Throwable e, Object... var2) {
        }

        public void error(String s) {
        }

        public void error(String s, Object... var2) {
        }

        public void error(String s, Throwable e) {
        }

        public void error(String s, Throwable e, Object... var2) {
        }
    }
}

