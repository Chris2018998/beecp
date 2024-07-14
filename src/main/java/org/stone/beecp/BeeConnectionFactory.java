/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A connection factory interface,whose implementation are used to create connections for pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionFactory {

    /**
     * Creates a connection
     *
     * @return a created connection
     * @throws SQLException while create failed.
     */
    Connection create() throws SQLException;
}
