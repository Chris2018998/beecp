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
 * Connection factory interface whose impl instance build connections to pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface RawConnectionFactory {

    /**
     * Creates a jdbc connection.
     *
     * @return a connection
     * @throws SQLException when creates failed(maybe invalid url,error username and password and so on)
     */
    Connection create() throws SQLException;
}
