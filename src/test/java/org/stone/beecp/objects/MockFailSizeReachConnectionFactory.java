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

import org.stone.beecp.RawConnectionFactory;
import org.stone.beecp.driver.MockDriver;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockFailSizeReachConnectionFactory implements RawConnectionFactory {
    private final int maxSize;
    private final boolean thrownExceptionInd;
    private final MockDriver driver = new MockDriver();
    private int createdCount;

    public MockFailSizeReachConnectionFactory(int maxSize) {
        this(maxSize, false);
    }

    public MockFailSizeReachConnectionFactory(int maxSize, boolean thrownExceptionInd) {
        this.maxSize = maxSize;
        this.thrownExceptionInd = thrownExceptionInd;
    }

    //create connection instance
    public Connection create() throws SQLException {
        if (createdCount >= maxSize) {
            if (thrownExceptionInd) throw new SQLException("The count of creation has reach max size");
            return null;
        }
        Connection con = driver.connect("testdb", null);
        createdCount++;
        return con;
    }
}
