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

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * A XAConnection factory interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeXaConnectionFactory {

    /**
     * Creates a xa connection.
     *
     * @return a created xa-connection
     * @throws SQLException when creates fail
     */
    XAConnection create() throws SQLException;
}
