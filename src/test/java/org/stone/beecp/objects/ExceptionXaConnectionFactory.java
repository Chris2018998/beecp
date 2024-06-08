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

import org.stone.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.SQLException;

public class ExceptionXaConnectionFactory implements RawXaConnectionFactory {

    public XAConnection create() throws SQLException {
        throw new SQLException("Unknown error");
    }
}
