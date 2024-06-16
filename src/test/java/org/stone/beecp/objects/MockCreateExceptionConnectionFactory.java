/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockCreateExceptionConnectionFactory extends MockCreateExceptionFactory implements RawConnectionFactory {

    public MockCreateExceptionConnectionFactory() {
    }

    public MockCreateExceptionConnectionFactory(SQLException cause1) {
        super(cause1);
    }

    public MockCreateExceptionConnectionFactory(RuntimeException cause2) {
        super(cause2);
    }

    public Connection create() throws SQLException {
        throwsException();
        return null;
    }
}
