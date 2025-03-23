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
 * Connection valid test
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionValidator {

    /**
     * attach a base validator
     *
     * @param baseValidator base validator
     */
    void attach(BeeConnectionValidator baseValidator);

    /**
     * Validates a connection at moment on successful borrowed.
     *
     * @param con     to be tested
     * @param timeout is seconds time wait for test result
     * @return true if alive,otherwise return false
     * @throws SQLException if error occur during testing
     */
    boolean isAlive(Connection con, int timeout) throws SQLException;
}
