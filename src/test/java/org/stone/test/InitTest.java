/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First Test Case
 *
 * @author chris liao
 */
public class InitTest {
    public static PrintStream systemOut;
    public static PrintStream systemErr;

    public static PrintStream systemTestOut;
    public static PrintStream systemTestErr;

    public static void switchToSystemOut() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    public static void switchToTestStreamOut() {
        System.setOut(systemTestOut);
        System.setErr(systemTestErr);
    }

    @Test
    public void testPreparation() {
        systemOut = System.out;
        systemErr = System.err;

        systemTestOut = new PrintStream(new ByteArrayOutputStream());
        systemTestErr = new PrintStream(new ByteArrayOutputStream());
        assertTrue(true);
    }
}



