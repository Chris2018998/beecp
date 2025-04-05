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

import org.stone.beecp.BeeConnectionValidator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */

public class MockConnectionValidator implements BeeConnectionValidator {

    private boolean isAlive = true;

    public MockConnectionValidator() {
    }

    public MockConnectionValidator(boolean isAlive) {
        this.isAlive = isAlive;
    }

    public void attach(BeeConnectionValidator baseValidator) {

    }

    public boolean isAlive(Connection con, int timeout) throws SQLException {
        return this.isAlive;
    }
}
