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

/**
 * @author Chris Liao
 */
public class SimpleMockConnectionFactory implements BeeConnectionFactory {
    public MockConnection create() {
        return new MockConnection();
    }
}
