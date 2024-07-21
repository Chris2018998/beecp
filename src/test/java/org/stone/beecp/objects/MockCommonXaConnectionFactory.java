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

import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.beecp.driver.MockConnection;
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.driver.MockXaConnection;
import org.stone.beecp.driver.MockXaResource;

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * A xa connection factory impl for mock test
 *
 * @author Chris Liao
 */
public final class MockCommonXaConnectionFactory extends MockCommonBaseFactory implements BeeXaConnectionFactory {

    public MockCommonXaConnectionFactory() {
    }

    public MockCommonXaConnectionFactory(MockConnectionProperties properties) {
        super(properties);
    }

    public XAConnection create() throws SQLException {
        this.throwCreationException();
        if (this.returnNullOnCreate) return null;

        this.checkCurCreatedCount();
        MockXaConnection con = new MockXaConnection(new MockConnection(properties), new MockXaResource());
        this.increaseCurCreatedCount();
        return con;
    }
}
