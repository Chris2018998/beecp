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

import org.stone.beecp.BeeConnectionFactory;
import org.stone.test.beecp.driver.MockConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 */
public class MaxSizeMockConnectionFactory extends BaseConnectionFactory implements BeeConnectionFactory {

    public MaxSizeMockConnectionFactory(int maxSize) {
        this.maxSize = maxSize;
        this.createdCount = new AtomicInteger();
    }

    public Connection create() throws SQLException {
        this.increaseCreationCount();
        return new MockConnection();
    }
}
