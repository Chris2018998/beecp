/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.xa;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.SQLException;

import static cn.beecp.pool.PoolConstants.ConnectionClosedException;
import static cn.beecp.pool.PoolConstants.XaConnectionClosedException;


/**
 * XaConnection Wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class XaConnectionWrapper implements XAConnection {
    private boolean isClosed;
    private Connection proxyConn;
    private XAConnection delegate;
    private XaResourceWrapper xaResource;

    public XaConnectionWrapper(XAConnection delegate, Connection proxyConn) {
        this.proxyConn = proxyConn;
        this.delegate = delegate;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) throw ConnectionClosedException;
    }

    void checkClosedForXa() throws XAException {
        if (isClosed) throw XaConnectionClosedException;
    }

    public void close() throws SQLException {
        isClosed = true;
        proxyConn.close();
    }

    public Connection getConnection() throws SQLException {
        checkClosed();
        return proxyConn;
    }

    public javax.transaction.xa.XAResource getXAResource() throws SQLException {
        checkClosed();
        if (xaResource == null)
            xaResource = new XaResourceWrapper(delegate.getXAResource(), this);
        return xaResource;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    public void addStatementEventListener(StatementEventListener listener) {
    }

    public void removeStatementEventListener(StatementEventListener listener) {
    }
}
