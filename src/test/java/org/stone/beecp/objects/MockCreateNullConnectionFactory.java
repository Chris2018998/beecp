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

import java.sql.Connection;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockCreateNullConnectionFactory extends DatabaseLinkInfo implements BeeConnectionFactory {

    public Connection create() {
        return null;
    }
}
