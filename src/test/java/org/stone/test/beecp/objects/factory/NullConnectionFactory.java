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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * test null result
 *
 * @author Chris Liao
 */
public class NullConnectionFactory extends BaseConnectionFactory implements BeeConnectionFactory {
    private boolean interruptableFlag;

    public NullConnectionFactory() {
    }

    public NullConnectionFactory(boolean interruptableFlag) {
        this.interruptableFlag = interruptableFlag;
    }

    public Connection create() throws SQLException {
        if (interruptableFlag) Thread.currentThread().interrupt();
        return null;
    }
}
