/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection factory
 *
 * @author Chris
 * @version 1.0
 */
public interface RawConnectionFactory {
    //create connection instance
    Connection create() throws SQLException;
}
