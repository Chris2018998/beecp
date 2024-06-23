/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.exception;

/**
 * Reflection Operation Exception
 *
 * @author Chris Liao
 */
public class ReflectionOperationException extends RuntimeException {

    public ReflectionOperationException(String s) {
        super(s);
    }

    public ReflectionOperationException(Throwable cause) {
        super(cause);
    }

    public ReflectionOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
