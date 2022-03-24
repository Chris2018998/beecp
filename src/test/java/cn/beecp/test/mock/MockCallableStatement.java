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
public class MockCallableStatement extends MockPreparedStatement implements CallableStatement {

    public MockCallableStatement(MockConnection connection) {
        super(connection);
    }

    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        //do nothing
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        //do nothing
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(int parameterIndex) throws SQLException {
        return null;
    }

    public boolean getBoolean(int parameterIndex) throws SQLException {
        return false;
    }

    public byte getByte(int parameterIndex) throws SQLException {
        return 1;
    }

    public short getShort(int parameterIndex) throws SQLException {
        return 1;
    }

    public int getInt(int parameterIndex) throws SQLException {
        return 1;
    }

    public long getLong(int parameterIndex) throws SQLException {
        return 1;
    }

    public float getFloat(int parameterIndex) throws SQLException {
        return 1;
    }

    public double getDouble(int parameterIndex) throws SQLException {
        return 1;
    }

    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return new BigDecimal(1);
    }


    public byte[] getBytes(int parameterIndex) throws SQLException {
        return null;
    }

    public Date getDate(int parameterIndex) throws SQLException {
        return null;
    }

    public Time getTime(int parameterIndex) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return null;
    }

    public Object getObject(int parameterIndex) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return null;
    }

    public Object getObject(int parameterIndex, java.util.Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(int parameterIndex) throws SQLException {
        return null;
    }

    public Blob getBlob(int parameterIndex) throws SQLException {
        return null;
    }

    public Clob getClob(int parameterIndex) throws SQLException {
        return null;
    }

    public Array getArray(int parameterIndex) throws SQLException {
        return null;
    }

    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return null;
    }

    public void registerOutParameter(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
    }

    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        //do nothing
    }


    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        //do nothing
    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        //do nothing
    }

    public java.net.URL getURL(int parameterIndex) throws SQLException {
        return null;
    }

    public void setURL(String parameterName, java.net.URL val) throws SQLException {
        //do nothing
    }

    public void setNull(String parameterName, int sqlType) throws SQLException {
        //do nothing
    }

    public void setBoolean(String parameterName, boolean x) throws SQLException {
        //do nothing
    }

    public void setByte(String parameterName, byte x) throws SQLException {
        //do nothing
    }

    public void setShort(String parameterName, short x) throws SQLException {
        //do nothing
    }

    public void setInt(String parameterName, int x) throws SQLException {
        //do nothing
    }

    public void setLong(String parameterName, long x) throws SQLException {
        //do nothing
    }

    public void setFloat(String parameterName, float x) throws SQLException {
        //do nothing
    }

    public void setDouble(String parameterName, double x) throws SQLException {
        //do nothing
    }

    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        //do nothing
    }

    public void setString(String parameterName, String x) throws SQLException {
        //do nothing
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException {
        //do nothing
    }

    public void setDate(String parameterName, Date x) throws SQLException {
        //do nothing
    }

    public void setTime(String parameterName, Time x) throws SQLException {
        //do nothing
    }

    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        //do nothing
    }

    public void setAsciiStream(String parameterName, java.io.InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(String parameterName, java.io.InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        //do nothing
    }

    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        //do nothing
    }

    public void setObject(String parameterName, Object x) throws SQLException {
        //do nothing
    }

    public void setCharacterStream(String parameterName, java.io.Reader reader, int length) throws SQLException {
        //do nothing
    }

    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        //do nothing
    }

    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        //do nothing
    }

    public String getString(String parameterName) throws SQLException {
        return null;
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        return true;
    }

    public byte getByte(String parameterName) throws SQLException {
        return 1;
    }

    public short getShort(String parameterName) throws SQLException {
        return 1;
    }

    public int getInt(String parameterName) throws SQLException {
        return 1;
    }

    public long getLong(String parameterName) throws SQLException {
        return 1;
    }

    public float getFloat(String parameterName) throws SQLException {
        return 1;
    }

    public double getDouble(String parameterName) throws SQLException {
        return 1;
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        return null;
    }

    public Date getDate(String parameterName) throws SQLException {
        return null;
    }

    public Time getTime(String parameterName) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return null;
    }

    public Object getObject(String parameterName) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return null;
    }

    public Object getObject(String parameterName, java.util.Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(String parameterName) throws SQLException {
        return null;
    }

    public Blob getBlob(String parameterName) throws SQLException {
        return null;
    }

    public Clob getClob(String parameterName) throws SQLException {
        return null;
    }

    public Array getArray(String parameterName) throws SQLException {
        return null;
    }

    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    public java.net.URL getURL(String parameterName) throws SQLException {
        return null;
    }


    public RowId getRowId(int parameterIndex) throws SQLException {
        return null;
    }

    public RowId getRowId(String parameterName) throws SQLException {
        return null;
    }

    public void setRowId(String parameterName, RowId x) throws SQLException {
        //do nothing
    }

    public void setNString(String parameterName, String value) throws SQLException {
        //do nothing
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        //do nothing
    }

    public void setNClob(String parameterName, NClob value) throws SQLException {
        //do nothing
    }

    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        //do nothing
    }

    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public NClob getNClob(int parameterIndex) throws SQLException {
        return null;
    }

    public NClob getNClob(String parameterName) throws SQLException {
        return null;
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        //do nothing
    }

    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return null;
    }

    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return null;
    }

    public String getNString(int parameterIndex) throws SQLException {
        return null;
    }

    public String getNString(String parameterName) throws SQLException {
        return null;
    }

    public java.io.Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return null;
    }

    public java.io.Reader getNCharacterStream(String parameterName) throws SQLException {
        return null;
    }

    public java.io.Reader getCharacterStream(int parameterIndex) throws SQLException {
        return null;
    }

    public java.io.Reader getCharacterStream(String parameterName) throws SQLException {
        return null;
    }

    public void setBlob(String parameterName, Blob x) throws SQLException {
        //do nothing
    }

    public void setClob(String parameterName, Clob x) throws SQLException {
        //do nothing
    }


    public void setAsciiStream(String parameterName, java.io.InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(String parameterName, java.io.InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void setCharacterStream(String parameterName, java.io.Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void setAsciiStream(String parameterName, java.io.InputStream x) throws SQLException {
        //do nothing
    }

    public void setBinaryStream(String parameterName, java.io.InputStream x) throws SQLException {
        //do nothing
    }

    public void setCharacterStream(String parameterName, java.io.Reader reader) throws SQLException {
        //do nothing
    }

    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        //do nothing
    }

    public void setClob(String parameterName, Reader reader) throws SQLException {
        //do nothing
    }

    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        //do nothing
    }

    public void setNClob(String parameterName, Reader reader) throws SQLException {
        //do nothing
    }

    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return null;
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return null;
    }
}
