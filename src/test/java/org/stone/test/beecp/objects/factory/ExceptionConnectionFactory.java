/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.factory;

import org.stone.beecp.BeeConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */

public class ExceptionConnectionFactory extends BaseConnectionFactory implements BeeConnectionFactory {

    public ExceptionConnectionFactory() {
        setFailCause(new SQLException("Unknown exception when created connection"));
    }

    public ExceptionConnectionFactory(Throwable failCause) {
        setFailCause(failCause);
    }

    public Connection create() throws SQLException {
        if (failCause1 != null) throw failCause1;
        if (failCause2 != null) throw failCause2;
        if (failCause3 != null) throw failCause3;
        return null;
    }
}
