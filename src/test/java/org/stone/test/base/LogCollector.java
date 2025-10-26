/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.base;

import org.stone.test.InitTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Log LogCollector
 *
 * @author chris liao
 */
public class LogCollector {
    private final ByteArrayOutputStream byteStream;

    LogCollector() {
        this.byteStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(byteStream));
    }

    public static LogCollector startLogCollector() {
        return new LogCollector();
    }

    public String endLogCollector() {
        InitTest.switchToTestStreamOut();
        return byteStream.toString();
    }
}
