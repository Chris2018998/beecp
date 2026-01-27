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

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * test null result
 *
 * @author Chris Liao
 */

public class NullXaConnectionFactory extends BaseConnectionFactory implements BeeXaConnectionFactory {
    private boolean interruptableFlag;

    public NullXaConnectionFactory() {
    }

    public NullXaConnectionFactory(boolean interruptableFlag) {
        this.interruptableFlag = interruptableFlag;
    }

    public XAConnection create() throws SQLException {
        if (interruptableFlag) Thread.currentThread().interrupt();
        return null;
    }
}
