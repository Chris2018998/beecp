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
 * Connection factory interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionFactory {

    /**
     * Creates a connection object to be pooled
     *
     * @return created connection
     * @throws SQLException when creates failed.
     */
    Connection create() throws SQLException;
}
