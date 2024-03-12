/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.creationStuck;

import org.stone.beecp.BeeDataSource;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * A mock thread to interrupt blocking from connection creation
 */
public class InterruptionMockThread extends Thread {

    private BeeDataSource dataSource;

    public InterruptionMockThread(BeeDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void run() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));//delay three seconds to execute interruption

        try {
            dataSource.interruptThreadsOnCreationLock();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
