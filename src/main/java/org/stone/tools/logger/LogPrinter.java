/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.logger;

import org.slf4j.Logger;

/**
 * A logger wrapper on SLF logger for stone project.
 *
 * @author Chris Liao
 */
public class LogPrinter {
    protected final Logger logger;

    LogPrinter() {
        this(null);
    }

    public LogPrinter(Logger logger) {
        this.logger = logger;
    }

    public boolean isOutputLogs() {
        return true;
    }

    public void setOutputLogs(boolean outputLogs) {

    }

    //****************************************************************************************************************//
    //                                     1: debug                                                                   //
    //****************************************************************************************************************//
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

    //****************************************************************************************************************//
    //                                     2: info                                                                    //
    //****************************************************************************************************************//
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

    //****************************************************************************************************************//
    //                                     3: warn                                                                    //
    //****************************************************************************************************************//
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

    //****************************************************************************************************************//
    //                                     4: error                                                                   //
    //****************************************************************************************************************//
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

final class LogPrinter2 extends LogPrinter {

    public boolean isOutputLogs() {
        return false;
    }

    //****************************************************************************************************************//
    //                                     1: debug                                                                   //
    //****************************************************************************************************************//

    public void debug(String s) {
    }

    public void debug(String s, Object... var2) {
    }

    public void debug(String s, Throwable e) {
    }

    public void debug(String s, Throwable e, Object... var2) {
    }

    //****************************************************************************************************************//
    //                                     2: info                                                                    //
    //****************************************************************************************************************//

    public void info(String s) {
    }

    public void info(String s, Object... var2) {
    }

    public void info(String s, Throwable e) {
    }

    public void info(String s, Throwable e, Object... var2) {
    }

    //****************************************************************************************************************//
    //                                     3: warn                                                                    //
    //****************************************************************************************************************//

    public void warn(String s) {
    }

    public void warn(String s, Object... var2) {
    }

    public void warn(String s, Throwable e) {
    }

    public void warn(String s, Throwable e, Object... var2) {
    }

    //****************************************************************************************************************//
    //                                     4: error                                                                   //
    //****************************************************************************************************************//
    public void error(String s) {
    }

    public void error(String s, Object... var2) {
    }

    public void error(String s, Throwable e) {
    }

    public void error(String s, Throwable e, Object... var2) {
    }
}

final class LogPrinter3 extends LogPrinter {
    private boolean outputLogs = true;

    public LogPrinter3(Logger logger) {
        super(logger);
    }

    public boolean isOutputLogs() {
        return outputLogs;
    }

    public void setOutputLogs(boolean outputLogs) {
        this.outputLogs = outputLogs;
    }

    //****************************************************************************************************************//
    //                                     1: debug                                                                   //
    //****************************************************************************************************************//
    public void debug(String s) {
        if (outputLogs) logger.debug(s);
    }

    public void debug(String s, Object... var2) {
        if (outputLogs) logger.debug(s, var2);
    }

    public void debug(String s, Throwable e) {
        if (outputLogs) logger.debug(s, e);
    }

    public void debug(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.debug(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     2: info                                                                    //
    //****************************************************************************************************************//
    public void info(String s) {
        if (outputLogs) logger.info(s);
    }

    public void info(String s, Object... var2) {
        if (outputLogs) logger.info(s, var2);
    }

    public void info(String s, Throwable e) {
        if (outputLogs) logger.info(s, e);
    }

    public void info(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.info(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     3: warn                                                                    //
    //****************************************************************************************************************//
    public void warn(String s) {
        if (outputLogs) logger.warn(s);
    }

    public void warn(String s, Object... var2) {
        if (outputLogs) logger.warn(s, var2);
    }

    public void warn(String s, Throwable e) {
        if (outputLogs) logger.warn(s, e);
    }

    public void warn(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.warn(s, e, var2);
    }

    //****************************************************************************************************************//
    //                                     4: error                                                                   //
    //****************************************************************************************************************//
    public void error(String s) {
        if (outputLogs) logger.error(s);
    }

    public void error(String s, Object... var2) {
        if (outputLogs) logger.error(s, var2);
    }

    public void error(String s, Throwable e) {
        if (outputLogs) logger.error(s, e);
    }

    public void error(String s, Throwable e, Object... var2) {
        if (outputLogs) logger.error(s, e, var2);
    }
}

