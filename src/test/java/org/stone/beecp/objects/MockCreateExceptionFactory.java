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

import java.sql.SQLException;

class MockCreateExceptionFactory {

    protected SQLException cause1;

    protected RuntimeException cause2;

    public MockCreateExceptionFactory() {
        cause1 = new SQLException("Network communications error");
    }

    public MockCreateExceptionFactory(SQLException cause1) {
        this.cause1 = cause1;
    }

    public MockCreateExceptionFactory(RuntimeException cause2) {
        this.cause2 = cause2;
    }

    public void throwsException() throws SQLException {
        if (cause1 != null) throw cause1;
        throw cause2;
    }
}
