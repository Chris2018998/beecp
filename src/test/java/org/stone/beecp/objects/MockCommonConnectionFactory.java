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

import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.driver.MockConnection;
import org.stone.beecp.driver.MockConnectionProperties;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A connection factory impl for mock test
 *
 * @author Chris Liao
 */
public class MockCommonConnectionFactory extends MockCommonBaseFactory implements BeeConnectionFactory {

    public MockCommonConnectionFactory() {
    }

    public MockCommonConnectionFactory(MockConnectionProperties properties) {
        super(properties);
    }

    public Connection create() throws SQLException {
        this.throwCreationException();
        if (this.returnNullOnCreate) return null;

        this.checkCurCreatedCount();
        MockConnection con = new MockConnection(properties);
        this.increaseCurCreatedCount();
        return con;
    }
}
