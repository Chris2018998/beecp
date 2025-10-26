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

import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.test.beecp.driver.MockConnection;
import org.stone.test.beecp.driver.MockXaConnection;
import org.stone.test.beecp.driver.MockXaResource;

import javax.sql.XAConnection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * for count of creation
 *
 * @author Chris Liao
 */
public class MaxSizeMockXaConnectionFactory extends BaseConnectionFactory implements BeeXaConnectionFactory {

    public MaxSizeMockXaConnectionFactory(int maxSize) {
        this.maxSize = maxSize;
        this.createdCount = new AtomicInteger();
    }

    public XAConnection create() throws SQLException {
        this.increaseCreationCount();
        MockConnection mockConnection = new MockConnection();
        return new MockXaConnection(mockConnection, new MockXaResource(mockConnection));
    }
}
