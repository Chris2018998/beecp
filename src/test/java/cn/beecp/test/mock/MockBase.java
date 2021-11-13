/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockBase implements Wrapper, AutoCloseable {
    private boolean isClosed = false;

    public void close() throws SQLException {
        isClosed = true;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("Wrapped object is not an instance of " + iface);
    }
}
