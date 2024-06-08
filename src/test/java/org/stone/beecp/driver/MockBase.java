/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.driver;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockBase implements Wrapper, AutoCloseable {
    private boolean isClosed = false;

    public void close() throws SQLException {
        if (isClosed) throw new SQLException("has been closed");
        isClosed = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isWrapperFor(Class<?> face) {
        return face.isInstance(this);
    }

    public <T> T unwrap(Class<T> face) throws SQLException {
        if (face.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + face);
    }
}
