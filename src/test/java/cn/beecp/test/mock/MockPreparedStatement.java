/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockPreparedStatement extends MockStatement implements PreparedStatement {
    public MockPreparedStatement(MockConnection connection) {
        super(connection);
    }

    public ResultSet executeQuery() throws SQLException {
        resultSet = new MockResultSet(this);
        return resultSet;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MockResultSetMetaData();
    }

    public int executeUpdate() throws SQLException {
        return 1;
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        //do nothing
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        //do nothing
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        //do nothing
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        //do nothing
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        //do nothing
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        //do nothing
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        //do nothing
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        //do nothing
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        //do nothing
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        //do nothing
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        //do nothing
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        //do nothing
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        //do nothing
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void clearParameters() throws SQLException {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        //do nothing
    }

    public boolean execute() throws SQLException {
        return true;
    }

    public void addBatch() throws SQLException {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        //do nothing
    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {
        //do nothing
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        //do nothing
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        //do nothing
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
        //do nothing
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        //do nothing
    }

    public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
        //do nothing
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        //do nothing
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        //do nothing
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        //do nothing
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        //do nothing
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        //do nothing
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        //do nothing
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        //do nothing
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        //do nothing
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        //do nothing
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        //do nothing
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        //do nothing
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        //do nothing
    }

}
