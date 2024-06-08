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
import org.stone.beecp.driver.MockConnection;

import java.sql.Connection;

public class MockConnectionFactory extends BaseConnectionFactory implements RawConnectionFactory {

    public Connection create() {
        return new MockConnection();
    }
}
