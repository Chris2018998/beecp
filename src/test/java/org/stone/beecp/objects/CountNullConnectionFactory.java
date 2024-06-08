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

public class CountNullConnectionFactory implements RawConnectionFactory {
    private final int maxCount;
    private final MockDriver driver = new MockDriver();
    private int createdCount;

    public CountNullConnectionFactory(int maxCount) {
        this.maxCount = maxCount;
    }

    //create connection instance
    public Connection create() throws SQLException {
        if (createdCount >= maxCount) return null;
        Connection con = driver.connect("testdb", null);
        createdCount++;
        return con;
    }
}
