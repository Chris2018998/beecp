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

import org.stone.beecp.BeeXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockCreateExceptionXaConnectionFactory extends MockCreateExceptionFactory implements BeeXaConnectionFactory {

    public MockCreateExceptionXaConnectionFactory() {
    }

    public MockCreateExceptionXaConnectionFactory(SQLException cause1) {
        super(cause1);
    }

    public MockCreateExceptionXaConnectionFactory(RuntimeException cause2) {
        super(cause2);
    }

    public XAConnection create() throws SQLException {
        throwsException();
        return null;
    }
}
