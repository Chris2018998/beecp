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

import org.stone.base.TestUtil;
import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeDataSource;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

public final class BorrowThread extends Thread {
    private final boolean xa;
    private final BeeDataSource ds;
    private final BeeConnectionPool pool;
    private Connection connection;
    private XAConnection xaConnection;
    private SQLException failureCause;

    public BorrowThread(BeeDataSource ds) {
        this(ds, null, false);
    }

    public BorrowThread(BeeConnectionPool pool) {
        this(null, pool, false);
    }

    public BorrowThread(BeeDataSource ds, BeeConnectionPool pool, boolean xa) {
        this.xa = xa;
        this.ds = ds;
        this.pool = pool;
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
            if (ds != null) {
                if (xa) {
                    xaConnection = ds.getXAConnection();
                } else {
                    connection = ds.getConnection();
                }
            } else {
                if (xa) {
                    xaConnection = pool.getXAConnection();
                } else {
                    connection = pool.getConnection();
                }
            }
        } catch (SQLException e) {
            this.failureCause = e;
        } finally {
            if (connection != null) TestUtil.oclose(connection);
            if (xaConnection != null) TestUtil.oclose(xaConnection);
        }
    }
}
