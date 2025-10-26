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
import org.stone.test.beecp.driver.MockXaConnectionProperties;
import org.stone.test.beecp.driver.MockXaResource;

/**
 * @author Chris Liao
 */
public class SimpleMockXaConnectionFactory implements BeeXaConnectionFactory {
    public MockXaConnection create() {
        MockXaConnectionProperties xaConnectionProperties = new MockXaConnectionProperties();
        MockConnection con = new MockConnection(xaConnectionProperties);
        return new MockXaConnection(xaConnectionProperties, con, new MockXaResource(con));
    }
}