/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.mock;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockPreparedStatement extends MockStatement implements PreparedStatement {
    MockPreparedStatement(MockConnection connection) {
        super(connection);
    }

    public ResultSet executeQuery() {
        resultSet = new MockResultSet(this);
        return resultSet;
    }

    public ResultSetMetaData getMetaData() {
        return new MockResultSetMetaData();
    }

    public int executeUpdate() {
        return 1;
    }

    public void setNull(int parameterIndex, int sqlType) {
        //do nothing
    }

    public void setBoolean(int parameterIndex, boolean x) {
        //do nothing
    }

    public void setByte(int parameterIndex, byte x) {
        //do nothing
    }

    public void setShort(int parameterIndex, short x) {
        //do nothing
    }

    public void setInt(int parameterIndex, int x) {
        //do nothing
    }

    public void setLong(int parameterIndex, long x) {
        //do nothing
    }

    public void setFloat(int parameterIndex, float x) {
        //do nothing
    }

    public void setDouble(int parameterIndex, double x) {
        //do nothing
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) {
        //do nothing
    }

    public void setString(int parameterIndex, String x) {
        //do nothing
    }

    public void setBytes(int parameterIndex, byte[] x) {
        //do nothing
    }

    public void setDate(int parameterIndex, Date x) {
        //do nothing
    }

    public void setTime(int parameterIndex, Time x) {
        //do nothing
    }

    public void setTimestamp(int parameterIndex, Timestamp x) {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) {
        //do nothing
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) {
        //do nothing
    }

    public void clearParameters() {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x) {
        //do nothing
    }

    public boolean execute() {
        return true;
    }

    public void addBatch() {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) {
        //do nothing
    }

    public void setRef(int parameterIndex, Ref x) {
        //do nothing
    }

    public void setBlob(int parameterIndex, Blob x) {
        //do nothing
    }

    public void setClob(int parameterIndex, Clob x) {
        //do nothing
    }

    public void setArray(int parameterIndex, Array x) {
        //do nothing
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) {
        //do nothing
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) {
        //do nothing
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {
        //do nothing
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) {
        //do nothing
    }

    public void setURL(int parameterIndex, java.net.URL x) {
        //do nothing
    }

    public ParameterMetaData getParameterMetaData() {
        return null;
    }

    public void setRowId(int parameterIndex, RowId x) {
        //do nothing
    }

    public void setNString(int parameterIndex, String value) {
        //do nothing
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) {
        //do nothing
    }

    public void setNClob(int parameterIndex, NClob value) {
        //do nothing
    }

    public void setClob(int parameterIndex, Reader reader, long length) {
        //do nothing
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) {
        //do nothing
    }

    public void setNClob(int parameterIndex, Reader reader, long length) {
        //do nothing
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x) {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x) {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader) {
        //do nothing
    }

    public void setNCharacterStream(int parameterIndex, Reader value) {
        //do nothing
    }

    public void setClob(int parameterIndex, Reader reader) {
        //do nothing
    }

    public void setBlob(int parameterIndex, InputStream inputStream) {
        //do nothing
    }

    public void setNClob(int parameterIndex, Reader reader) {
        //do nothing
    }
}
