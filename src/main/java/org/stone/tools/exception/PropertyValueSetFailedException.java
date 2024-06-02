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
 * Bean property value set failed exception
 *
 * @author Chris Liao
 */
public class PropertyValueSetFailedException extends PropertyException {

    public PropertyValueSetFailedException(String s) {
        super(s);
    }

    public PropertyValueSetFailedException(Throwable cause) {
        super(cause);
    }

    public PropertyValueSetFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
