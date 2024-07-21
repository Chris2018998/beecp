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
    private final MockDatabaseMetaData metaData;
    private final MockConnectionProperties properties;

    public MockConnection() {
        this(new MockConnectionProperties());
    }

    public MockConnection(MockConnectionProperties properties) {
        this.properties = properties;
        this.metaData = new MockDatabaseMetaData(this);
    }

    //***************************************************************************************************************//
    //                                          Properties                                                           //                                                                                  //
    //***************************************************************************************************************//
    public boolean getAutoCommit() throws SQLException {
        properties.mockThrowExceptionOnMethod("getAutoCommit");
        return properties.isAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        properties.mockThrowExceptionOnMethod("setAutoCommit");
        properties.setAutoCommit(autoCommit);
    }

    public int getTransactionIsolation() throws SQLException {
        properties.mockThrowExceptionOnMethod("getTransactionIsolation");
        return properties.getTransactionIsolation();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        properties.mockThrowExceptionOnMethod("setTransactionIsolation");
        properties.setTransactionIsolation(level);
    }

    public boolean isReadOnly() throws SQLException {
        properties.mockThrowExceptionOnMethod("isReadOnly");
        return properties.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        properties.mockThrowExceptionOnMethod("setReadOnly");
        properties.setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException {
        properties.mockThrowExceptionOnMethod("getCatalog");
        return properties.getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        properties.mockThrowExceptionOnMethod("setCatalog");
        this.properties.setCatalog(catalog);
    }

    public String getSchema() throws SQLException {
        properties.mockThrowExceptionOnMethod("getSchema");
        return properties.getSchema();
    }

    public void setSchema(String schema) throws SQLException {
        properties.mockThrowExceptionOnMethod("setSchema");
        properties.setSchema(schema);
    }

    public int getNetworkTimeout() throws SQLException {
        properties.mockThrowExceptionOnMethod("getNetworkTimeout");
        return this.properties.getNetworkTimeout();
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        properties.mockThrowExceptionOnMethod("setNetworkTimeout");
        properties.setNetworkTimeout(milliseconds);
    }

    public boolean isValid(int timeout) throws SQLException {
        properties.mockThrowExceptionOnMethod("isValid");
        return properties.isValid();
    }

    public int getHoldability() {
        return properties.getHoldability();
    }

    public void setHoldability(int holdability) {
        this.properties.setHoldability(holdability);
    }

    //***************************************************************************************************************//
    //                                          Statement                                                            //                                                                                  //
    //***************************************************************************************************************//
    public Statement createStatement() throws SQLException {
        properties.mockThrowExceptionOnMethod("createStatement");
        return new MockStatement(this);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        properties.mockThrowExceptionOnMethod("createStatement");
        return new MockStatement(this);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        properties.mockThrowExceptionOnMethod("createStatement");
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareStatement");
        return new MockPreparedStatement(this);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareCall");
        return new MockCallableStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareCall");
        return new MockCallableStatement(this);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        properties.mockThrowExceptionOnMethod("prepareCall");
        return new MockCallableStatement(this);
    }

    //***************************************************************************************************************//
    //                                          Transaction                                                          //                                                                                  //
    //***************************************************************************************************************//
    public void commit() {
        //do nothing
    }

    public void rollback() {
        //do nothing
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

    //***************************************************************************************************************//
    //                                          Close                                                                //                                                                                  //
    //***************************************************************************************************************//
    public void abort(Executor executor) {
        //do nothing
    }


    //***************************************************************************************************************//
    //                                          Warning                                                              //                                                                                  //
    //***************************************************************************************************************//
    public SQLWarning getWarnings() {
        return null;
    }

    public void clearWarnings() {
        //do nothing
    }

    //***************************************************************************************************************//
    //                                          ClientInfo                                                           //                                                                                  //
    //***************************************************************************************************************//
    public Properties getClientInfo() {
        return null;
    }

    public void setClientInfo(Properties properties) {
        //do nothing
    }

    public String getClientInfo(String name) {
        return null;
    }

    public void setClientInfo(String name, String value) {
        //do nothing
    }

    //***************************************************************************************************************//
    //                                          create jdbc objects                                                  //                                                                                  //
    //***************************************************************************************************************//
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

    public Array createArrayOf(String typeName, Object[] elements) {
        return null;
    }

    public Struct createStruct(String typeName, Object[] attributes) {
        return null;
    }

    public java.util.Map<String, Class<?>> getTypeMap() {
        return null;
    }

    public void setTypeMap(java.util.Map<String, Class<?>> map) {
        //do nothing
    }

    //***************************************************************************************************************//
    //                                          Others                                                               //                                                                                  //
    //***************************************************************************************************************//
    public DatabaseMetaData getMetaData() {
        return metaData;
    }

    public String nativeSQL(String sql) {
        return null;
    }
}
