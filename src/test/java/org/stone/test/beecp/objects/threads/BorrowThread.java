/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.threads;

import org.stone.beecp.BeeDataSource;
import org.stone.test.base.TestUtil;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public final class BorrowThread extends Thread {
    private final boolean xa;
    private final BeeDataSource ds;
    private Connection connection;
    private XAConnection xaConnection;
    private SQLException failureCause;

    public BorrowThread(BeeDataSource ds) {
        this(ds, false);
    }

    public BorrowThread(BeeDataSource ds, boolean xa) {
        this.xa = xa;
        this.ds = ds;
        this.setDaemon(true);
    }

    public SQLException getFailureCause() {
        return failureCause;
    }

    public Connection getConnection() {
        return connection;
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

    public void run() {
        try {
            if (xa) {
                xaConnection = ds.getXAConnection();
            } else {
                connection = ds.getConnection();
            }
        } catch (SQLException e) {
            this.failureCause = e;
        } finally {
            if (connection != null) TestUtil.oclose(connection);
            if (xaConnection != null) TestUtil.oclose(xaConnection);
        }
    }
}
