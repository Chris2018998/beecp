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

import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.test.beecp.driver.MockConnection;
import org.stone.test.beecp.driver.MockXaConnection;
import org.stone.test.beecp.driver.MockXaConnectionProperties;
import org.stone.test.beecp.driver.MockXaResource;

import javax.sql.XAConnection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class MockXaConnectionFactory extends BaseConnectionFactory implements BeeXaConnectionFactory {

    private final MockXaConnectionProperties xaConnectionProperties;

    public MockXaConnectionFactory() {
        this(new MockXaConnectionProperties());
    }

    public MockXaConnectionFactory(MockXaConnectionProperties xaConnectionProperties) {
        this.xaConnectionProperties = xaConnectionProperties;
    }

    public XAConnection create() throws SQLException {
        if (needPark) {
            if (sleepMillis > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    throw new SQLException(e);
                }
            } else if (this.parkNanos > 0L) {
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

        MockConnection con = new MockConnection(xaConnectionProperties);
        return new MockXaConnection(xaConnectionProperties, con, new MockXaResource(con));
    }
}



