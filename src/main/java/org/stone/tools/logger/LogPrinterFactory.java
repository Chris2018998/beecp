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

import org.slf4j.LoggerFactory;

/**
 * LogPrinter Factory
 *
 * @author Chris Liao
 */
public class LogPrinterFactory {
    //an empty printer
    public static final LogPrinter BlankLogPrinter = new LogPrinter2();
    //a switchable printer
    public static final LogPrinter CommonLogPrinter = LogPrinterFactory.getLogPrinter(LogPrinterFactory.class);

    public static LogPrinter getLogPrinter(Class<?> targetClass) {
        return getLogPrinter(targetClass, false);
    }

    public static LogPrinter getLogPrinter(Class<?> targetClass, boolean needSwitch) {
        if (targetClass == null) return BlankLogPrinter;
        return needSwitch ? new LogPrinter3(LoggerFactory.getLogger(targetClass)) : new LogPrinter(LoggerFactory.getLogger(targetClass));
    }
}
