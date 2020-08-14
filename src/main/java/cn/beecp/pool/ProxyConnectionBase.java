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
package cn.beecp.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static cn.beecp.pool.PoolExceptionList.*;
import static cn.beecp.util.BeecpUtil.equalsText;
import static java.lang.System.currentTimeMillis;

/**
 * raw connection wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
abstract class ProxyConnectionBase implements Connection{
    protected Connection delegate;
    protected PooledConnection pConn;//called by subclass to update time
    private boolean closedInd;

    private final static int Pos_AutoCommitInd=0;
    private final static int Pos_TransactionIsolationInd=1;
    private final static int Pos_ReadOnlyInd=2;
    private final static int Pos_CatalogInd=3;
    private final static int Pos_SchemaInd=4;
    private final static int Pos_NetworkTimeoutInd=5;

    public ProxyConnectionBase(PooledConnection pConn) {
        this.pConn=pConn;
        pConn.proxyConn=this;
        delegate=pConn.rawConn;
    }
    public boolean isClosed()throws SQLException{
        return closedInd;
    }
    protected void checkClosed() throws SQLException {
        if(closedInd)throw ConnectionClosedException;
    }
    synchronized boolean setAsClosed(){
        return closedInd?false:(closedInd=true);
    }
    public void close() throws SQLException {
        if(setAsClosed()){
            pConn.returnToPoolBySelf();
        }else{
            throw ConnectionClosedException;
        }
    }
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        if(!pConn.curAutoCommit && pConn.commitDirtyInd)
            throw AutoCommitChangeForbiddenException;

        delegate.setAutoCommit(autoCommit);
        pConn.curAutoCommit = autoCommit;
        if(autoCommit)pConn.commitDirtyInd=false;
        pConn.setChangedInd(Pos_AutoCommitInd,autoCommit!=pConn.defaultAutoCommit);
        pConn.lastAccessTime=currentTimeMillis();
    }
    public void setTransactionIsolation(int level) throws SQLException {
        checkClosed();
        delegate.setTransactionIsolation(level);
        pConn.setChangedInd(Pos_TransactionIsolationInd,level!=pConn.defaultTransactionIsolationCode);
        pConn.lastAccessTime=currentTimeMillis();
    }
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkClosed();
        delegate.setReadOnly(readOnly);
        pConn.setChangedInd(Pos_ReadOnlyInd,readOnly!=pConn.defaultReadOnly);
    }
    public void setCatalog(String catalog) throws SQLException {
        checkClosed();
        delegate.setCatalog(catalog);
        pConn.setChangedInd(Pos_CatalogInd,!equalsText(catalog, pConn.defaultCatalog));
    }
    public boolean isValid(int timeout) throws SQLException {
        checkClosed();
        return delegate.isValid(timeout);
    }
    //for JDK1.7 begin
    public void setSchema(String schema) throws SQLException {
        checkClosed();
        delegate.setSchema(schema);
        pConn.setChangedInd(Pos_SchemaInd, !equalsText(schema, pConn.defaultSchema));
    }
    public void abort(Executor executor) throws SQLException{
        checkClosed();
        if(executor == null) throw new SQLException("executor can't be null");
        executor.execute(new Runnable() {
            public void run() {
                try {
                    ProxyConnectionBase.this.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    public int getNetworkTimeout() throws SQLException{
        checkClosed();
        return delegate.getNetworkTimeout();
    }
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        checkClosed();
        if(pConn.isSupportNetworkTimeout()) {
            delegate.setNetworkTimeout(executor, milliseconds);
            pConn.setChangedInd(Pos_NetworkTimeoutInd, milliseconds != pConn.defaultNetworkTimeout);
        }else{
            throw DriverNotSupportNetworkTimeoutException;
        }
    }
    //for JDK1.7 end

    public void commit() throws SQLException{
        checkClosed();
        delegate.commit();
        pConn.lastAccessTime=currentTimeMillis();
        pConn.commitDirtyInd=false;
    }
    public void rollback() throws SQLException{
        checkClosed();
        delegate.rollback();
        pConn.lastAccessTime=currentTimeMillis();
        pConn.commitDirtyInd=false;
    }
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        checkClosed();
        return iface.isInstance(this);
    }
    public <T> T unwrap(Class<T> iface) throws SQLException{
        checkClosed();
        if(iface.isInstance(this))
            return (T)this;
        else
            throw new SQLException("Wrapped object is not an instance of "+iface);
    }
}
