/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

import org.stone.beecp.BeeXaConnectionFactory;
import org.stone.test.beecp.driver.MockXaDataSource;

import javax.sql.XAConnection;

/**
 * @author Chris Liao
 */
public class MockDriverXaConnectionFactory extends DatabaseLinkInfo implements BeeXaConnectionFactory {

    private final MockXaDataSource xaDataSource = new MockXaDataSource();

    public XAConnection create() {
        return xaDataSource.getXAConnection();
    }

    public XAConnection create(String username, String password) {
        return xaDataSource.getXAConnection(username, password);
    }
}
