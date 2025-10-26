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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class TimeDelayCloseConnectionThread extends Thread {
    private final Connection con;
    private final Long time;

    public TimeDelayCloseConnectionThread(Connection con, Long time) {
        this.con = con;
        this.time = time;
    }

    public void run() {
        try {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(time - System.currentTimeMillis()));
            con.close();
        } catch (SQLException e) {
            //do nothing
        }
    }
}