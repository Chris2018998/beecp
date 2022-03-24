/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockConnection extends MockBase implements Connection {
    private boolean readOnly;
    private String catalog;
    private int transactionIsolation;
    private boolean autoCommit;
    private int holdability;
    private String schema;

    public MockConnection() {
    }

    public Statement createStatement() throws SQLException {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return new MockCallableStatement(this);
    }

    public String nativeSQL(String sql) throws SQLException {
        return null;
    }

    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    public void commit() throws SQLException {
    }

    public void rollback() throws SQLException {
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return new MockDatabaseMetaData(this);
    }

    public boolean isReadOnly() throws SQLException {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    public String getCatalog() throws SQLException {
        return catalog;
    }

    public void setCatalog(String catalog) throws SQLException {
        this.catalog = catalog;
    }

    public int getTransactionIsolation() throws SQLException {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int level) throws SQLException {
        transactionIsolation = level;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
        //do nothing
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MockCallableStatement(this);
    }

    public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
        return null;
    }

    public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
        //do nothing
    }

    public int getHoldability() throws SQLException {
        return holdability;
    }

    public void setHoldability(int holdability) throws SQLException {
        this.holdability = holdability;
    }

    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        //do nothing
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        //do nothing
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MockCallableStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new MockPreparedStatement(this);
    }

    public Clob createClob() throws SQLException {
        return null;
    }

    public Blob createBlob() throws SQLException {
        return null;
    }

    public NClob createNClob() throws SQLException {
        return null;
    }

    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    public boolean isValid(int timeout) throws SQLException {
        return true;
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
    }

    public String getClientInfo(String name) throws SQLException {
        return null;
    }

    public Properties getClientInfo() throws SQLException {
        return null;
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    public String getSchema() throws SQLException {
        return schema;
    }

    public void setSchema(String schema) throws SQLException {
        this.schema = schema;
    }

    public void abort(Executor executor) throws SQLException {
        //do nothing
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        //do nothing
    }

    public int getNetworkTimeout() throws SQLException {
        return 0;
    }
}
