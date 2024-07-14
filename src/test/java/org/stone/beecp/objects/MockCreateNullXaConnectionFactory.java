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

import javax.sql.XAConnection;

/**
 * Mock Impl on ConnectionFactory
 *
 * @author Chris Liao
 */
public class MockCreateNullXaConnectionFactory extends DatabaseLinkInfo implements BeeXaConnectionFactory {

    public XAConnection create() {
        return null;
    }
}