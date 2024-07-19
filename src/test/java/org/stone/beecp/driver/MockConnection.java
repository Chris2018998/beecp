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

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockConnection extends MockBase implements Connection {
    public int errorCode;
    public String errorState;
    private boolean isValid = true;
    private boolean readOnly;
    private String catalog;
    private int transactionIsolation;
    private boolean autoCommit;
    private int holdability;
    private String schema;

    private boolean exceptionOnAutoCommit;
    private boolean exceptionOnTransactionIsolation;
    private boolean exceptionOnReadOnly;
    private boolean exceptionOnCatalog;
    private boolean exceptionOnSchema;

    private int networkTimeout;
    private boolean exceptionOnNetworkTimeout;

    public MockConnection() {
    }

    public MockConnection(int errorCode) {
        this.errorCode = errorCode;
    }

    public MockConnection(String errorState) {
        this.errorState = errorState;
    }

    public MockConnection(int errorCode, String errorState) {
        this.errorCode = errorCode;
        this.errorState = errorState;
    }

    public void enableExceptionOnDefault() {
        this.exceptionOnAutoCommit = true;
        this.exceptionOnTransactionIsolation = true;
        this.exceptionOnReadOnly = true;
        this.exceptionOnCatalog = true;
        this.exceptionOnSchema = true;
    }

    public void setExceptionOnNetworkTimeout(boolean exceptionOnNetworkTimeout) {
        this.exceptionOnNetworkTimeout = exceptionOnNetworkTimeout;
    }

    public String getSchema() throws SQLException {
        if (exceptionOnSchema) throw new SQLException();
        if (errorCode != 0 || errorState != null) throw new SQLException(null, errorState, errorCode);
        return schema;
    }

    public void setSchema(String schema) throws SQLException {
        if (exceptionOnSchema) throw new SQLException();
        this.schema = schema;
    }

    public boolean isReadOnly() throws SQLException {
        if (exceptionOnReadOnly) throw new SQLException();
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        if (exceptionOnReadOnly) throw new SQLException();
        this.readOnly = readOnly;
    }

    public String getCatalog() throws SQLException {
        if (exceptionOnCatalog) throw new SQLException();
        return catalog;
    }

    public void setCatalog(String catalog) throws SQLException {
        if (exceptionOnCatalog) throw new SQLException();
        this.catalog = catalog;
    }

    public int getTransactionIsolation() throws SQLException {
        if (exceptionOnTransactionIsolation) throw new SQLException();
        return transactionIsolation;
    }

    public void setTransactionIsolation(int level) throws SQLException {
        if (exceptionOnTransactionIsolation) throw new SQLException();
        transactionIsolation = level;
    }

    //************************************* added for test**************************************************//
    public void setValid(boolean valid) {
        isValid = valid;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorState(String errorState) {
        this.errorState = errorState;
    }

    public boolean isValid(int timeout) throws SQLException {
        if (errorCode != 0 || errorState != null) throw new SQLException(null, errorState, errorCode);
        return isValid;
    }

    public Statement createStatement() {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql) {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql) {
        return new MockCallableStatement(this);
    }

    public String nativeSQL(String sql) {
        return null;
    }

    public boolean getAutoCommit() throws SQLException {
        if (exceptionOnAutoCommit) throw new SQLException();
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (exceptionOnAutoCommit) throw new SQLException();
        this.autoCommit = autoCommit;
    }

    public void commit() {
        //do nothing
    }

    public void rollback() {
        //do nothing
    }

    public DatabaseMetaData getMetaData() {
        return new MockDatabaseMetaData(this);
    }


    public SQLWarning getWarnings() {
        return null;
    }

    public void clearWarnings() {
        //do nothing
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
        return new MockCallableStatement(this);
    }

    public java.util.Map<String, Class<?>> getTypeMap() {
        return null;
    }

    public void setTypeMap(java.util.Map<String, Class<?>> map) {
        //do nothing
    }

    public int getHoldability() {
        return holdability;
    }

    public void setHoldability(int holdability) {
        this.holdability = holdability;
    }

    public Savepoint setSavepoint() {
        return null;
    }

    public Savepoint setSavepoint(String name) {
        return null;
    }

    public void rollback(Savepoint savepoint) {
        //do nothing
    }

    public void releaseSavepoint(Savepoint savepoint) {
        //do nothing
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return new MockCallableStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) {
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) {
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) {
        return new MockPreparedStatement(this);
    }

    public Clob createClob() {
        return null;
    }

    public Blob createBlob() {
        return null;
    }

    public NClob createNClob() {
        return null;
    }

    public SQLXML createSQLXML() {
        return null;
    }


    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        //do nothing
    }

    public String getClientInfo(String name) {
        return null;
    }

    public Properties getClientInfo() {
        return null;
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        //do nothing
    }

    public Array createArrayOf(String typeName, Object[] elements) {
        return null;
    }

    public Struct createStruct(String typeName, Object[] attributes) {
        return null;
    }

    public void abort(Executor executor) {
        //do nothing
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (exceptionOnNetworkTimeout) throw new SQLException();
        this.networkTimeout = milliseconds;
    }

    public int getNetworkTimeout() throws SQLException {
        if (exceptionOnNetworkTimeout) throw new SQLException();
        return this.networkTimeout;
    }

    public void setNetworkTimeout(int networkTimeout) {
        this.networkTimeout = networkTimeout;
    }
}
