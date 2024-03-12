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

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

/**
 * Blocking Mock in creating connection
 *
 * @author chris liao
 */
public class BlockingConnectionFactory implements RawConnectionFactory {

    public Connection create() throws SQLException {
        LockSupport.park();
        if (Thread.interrupted()) throw new RuntimeException("InterruptedException");
        return null;
    }
}
