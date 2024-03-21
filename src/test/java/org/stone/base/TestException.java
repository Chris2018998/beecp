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

/**
 * Test Exception
 *
 * @author chris liao
 */
public class TestException extends Exception {
    public TestException() {
    }

    public TestException(String s) {
        super(s);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }

}
