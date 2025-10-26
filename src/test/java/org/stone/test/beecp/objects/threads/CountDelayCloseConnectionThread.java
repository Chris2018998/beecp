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
import java.util.concurrent.CountDownLatch;

/**
 * @author Chris Liao
 */
public class CountDelayCloseConnectionThread extends Thread {
    private final Connection con;
    private final CountDownLatch latch;

    public CountDelayCloseConnectionThread(Connection con, CountDownLatch latch) {
        this.con = con;
        this.latch = latch;
    }

    public void run() {
        try {
            con.close();
            latch.countDown();
        } catch (SQLException e) {
            //do nothing
        }
    }
}