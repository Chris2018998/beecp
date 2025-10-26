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
import org.stone.test.beecp.driver.MockConnection;
import org.stone.test.beecp.driver.MockConnectionProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class MockConnectionFactory extends BaseConnectionFactory implements BeeConnectionFactory {

    private final MockConnectionProperties connectionProperties;

    public MockConnectionFactory() {
        this(new MockConnectionProperties());
    }

    public MockConnectionFactory(MockConnectionProperties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public Connection create() throws SQLException {
        if (needPark) {
            if (this.parkNanos > 0L) {
                LockSupport.parkNanos(parkNanos);
            } else {
                LockSupport.park();
            }
            if (Thread.interrupted()) {
                return null;
            }
        }

        if (failCause1 != null) throw failCause1;
        if (failCause2 != null) throw failCause2;
        if (failCause3 != null) throw failCause3;
        return new MockConnection(connectionProperties);
    }
}
