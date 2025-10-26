/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

/**
 * A runtime exception thrown when fail to create {@link BeeDataSource}
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeDataSourceCreationException extends RuntimeException {

    public BeeDataSourceCreationException(Throwable cause) {
        super(cause);
    }
}
