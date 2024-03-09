/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.mock;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockBase implements Wrapper, AutoCloseable {
    private boolean isClosed = false;

    public void close() throws SQLException {
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
