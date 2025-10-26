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

import org.stone.beecp.pool.exception.ConnectionCreateException;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 */
public class BaseConnectionFactory {
    protected long parkNanos;
    protected boolean needPark;
    protected SQLException failCause1;
    protected RuntimeException failCause2;
    protected Error failCause3;

    protected int maxSize;
    protected AtomicInteger createdCount;
    private String JdbcUrl;
    private String username;
    private String password;

    public long getParkNanos() {
        return parkNanos;
    }

    public void setParkNanos(long parkNanos) {
        this.parkNanos = parkNanos;
    }

    public boolean isNeedPark() {
        return needPark;
    }

    public void setNeedPark(boolean needPark) {
        this.needPark = needPark;
    }

    public String getJdbcUrl() {
        return JdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.JdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFailCause(Throwable failCause) {
        failCause1 = null;
        failCause2 = null;
        failCause3 = null;
        if (failCause instanceof SQLException) {
            failCause1 = (SQLException) failCause;
        } else if (failCause instanceof RuntimeException) {
            failCause2 = (RuntimeException) failCause;
        } else if (failCause instanceof Error) {
            failCause3 = (Error) failCause;
        } else {
            failCause1 = new SQLException(failCause);
        }
    }

    public void increaseCreationCount() throws SQLException {
        int count;
        do {
            count = createdCount.get();
            if (count >= maxSize)
                throw new ConnectionCreateException("the count of created connections has reached max");
        } while (!createdCount.compareAndSet(count, count + 1));
    }
}
