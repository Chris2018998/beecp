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
public class MockCallableStatement extends MockPreparedStatement implements CallableStatement {

    public MockCallableStatement(MockConnection connection) {
        super(connection);
    }

    public void registerOutParameter(int parameterIndex, int sqlType) {
        //do nothing
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale) {
        //do nothing
    }

    public boolean wasNull() {
        return false;
    }

    public String getString(int parameterIndex) {
        return null;
    }

    public boolean getBoolean(int parameterIndex) {
        return false;
    }

    public byte getByte(int parameterIndex) {
        return 1;
    }

    public short getShort(int parameterIndex) {
        return 1;
    }

    public int getInt(int parameterIndex) {
        return 1;
    }

    public long getLong(int parameterIndex) {
        return 1;
    }

    public float getFloat(int parameterIndex) {
        return 1;
    }

    public double getDouble(int parameterIndex) {
        return 1;
    }

    public BigDecimal getBigDecimal(int parameterIndex, int scale) {
        return new BigDecimal(1);
    }

    public byte[] getBytes(int parameterIndex) {
        return null;
    }

    public Date getDate(int parameterIndex) {
        return null;
    }

    public Time getTime(int parameterIndex) {
        return null;
    }

    public Timestamp getTimestamp(int parameterIndex) {
        return null;
    }

    public Object getObject(int parameterIndex) {
        return null;
    }

    public BigDecimal getBigDecimal(int parameterIndex) {
        return null;
    }

    public Object getObject(int parameterIndex, java.util.Map<String, Class<?>> map) {
        return null;
    }

    public Ref getRef(int parameterIndex) {
        return null;
    }

    public Blob getBlob(int parameterIndex) {
        return null;
    }

    public Clob getClob(int parameterIndex) {
        return null;
    }

    public Array getArray(int parameterIndex) {
        return null;
    }

    public Date getDate(int parameterIndex, Calendar cal) {
        return null;
    }

    public Time getTime(int parameterIndex, Calendar cal) {
        return null;
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal) {
        return null;
    }

    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) {
        //do nothing
    }

    public void registerOutParameter(String parameterName, int sqlType) {
        //do nothing
    }


    public void registerOutParameter(String parameterName, int sqlType, int scale) {
        //do nothing
    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName) {
        //do nothing
    }

    public java.net.URL getURL(int parameterIndex) {
        return null;
    }

    public void setURL(String parameterName, java.net.URL val) {
        //do nothing
    }

    public void setNull(String parameterName, int sqlType) {
        //do nothing
    }

    public void setBoolean(String parameterName, boolean x) {
        //do nothing
    }

    public void setByte(String parameterName, byte x) {
        //do nothing
    }

    public void setShort(String parameterName, short x) {
        //do nothing
    }

    public void setInt(String parameterName, int x) {
        //do nothing
    }

    public void setLong(String parameterName, long x) {
        //do nothing
    }

    public void setFloat(String parameterName, float x) {
        //do nothing
    }

    public void setDouble(String parameterName, double x) {
        //do nothing
    }

    public void setBigDecimal(String parameterName, BigDecimal x) {
        //do nothing
    }

    public void setString(String parameterName, String x) {
        //do nothing
    }

    public void setBytes(String parameterName, byte[] x) {
        //do nothing
    }

    public void setDate(String parameterName, Date x) {
        //do nothing
    }

    public void setTime(String parameterName, Time x) {
        //do nothing
    }

    public void setTimestamp(String parameterName, Timestamp x) {
        //do nothing
    }

    public void setAsciiStream(String parameterName, InputStream x, int length) {
        //do nothing
    }

    public void setBinaryStream(String parameterName, InputStream x, int length) {
        //do nothing
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale) {
        //do nothing
    }

    public void setObject(String parameterName, Object x, int targetSqlType) {
        //do nothing
    }

    public void setObject(String parameterName, Object x) {
        //do nothing
    }

    public void setCharacterStream(String parameterName, Reader reader, int length) {
        //do nothing
    }

    public void setDate(String parameterName, Date x, Calendar cal) {
        //do nothing
    }

    public void setTime(String parameterName, Time x, Calendar cal) {
        //do nothing
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) {
        //do nothing
    }

    public void setNull(String parameterName, int sqlType, String typeName) {
        //do nothing
    }

    public String getString(String parameterName) {
        return null;
    }

    public boolean getBoolean(String parameterName) {
        return true;
    }

    public byte getByte(String parameterName) {
        return 1;
    }

    public short getShort(String parameterName) {
        return 1;
    }

    public int getInt(String parameterName) {
        return 1;
    }

    public long getLong(String parameterName) {
        return 1;
    }

    public float getFloat(String parameterName) {
        return 1;
    }

    public double getDouble(String parameterName) {
        return 1;
    }

    public byte[] getBytes(String parameterName) {
        return null;
    }

    public Date getDate(String parameterName) {
        return null;
    }

    public Time getTime(String parameterName) {
        return null;
    }

    public Timestamp getTimestamp(String parameterName) {
        return null;
    }

    public Object getObject(String parameterName) {
        return null;
    }

    public BigDecimal getBigDecimal(String parameterName) {
        return null;
    }

    public Object getObject(String parameterName, java.util.Map<String, Class<?>> map) {
        return null;
    }

    public Ref getRef(String parameterName) {
        return null;
    }

    public Blob getBlob(String parameterName) {
        return null;
    }

    public Clob getClob(String parameterName) {
        return null;
    }

    public Array getArray(String parameterName) {
        return null;
    }

    public Date getDate(String parameterName, Calendar cal) {
        return null;
    }

    public Time getTime(String parameterName, Calendar cal) {
        return null;
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal) {
        return null;
    }

    public java.net.URL getURL(String parameterName) {
        return null;
    }


    public RowId getRowId(int parameterIndex) {
        return null;
    }

    public RowId getRowId(String parameterName) {
        return null;
    }

    public void setRowId(String parameterName, RowId x) {
        //do nothing
    }

    public void setNString(String parameterName, String value) {
        //do nothing
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) {
        //do nothing
    }

    public void setNClob(String parameterName, NClob value) {
        //do nothing
    }

    public void setClob(String parameterName, Reader reader, long length) {
        //do nothing
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) {
        //do nothing
    }

    public void setNClob(String parameterName, Reader reader, long length) {
        //do nothing
    }

    public NClob getNClob(int parameterIndex) {
        return null;
    }

    public NClob getNClob(String parameterName) {
        return null;
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) {
        //do nothing
    }

    public SQLXML getSQLXML(int parameterIndex) {
        return null;
    }

    public SQLXML getSQLXML(String parameterName) {
        return null;
    }

    public String getNString(int parameterIndex) {
        return null;
    }

    public String getNString(String parameterName) {
        return null;
    }

    public Reader getNCharacterStream(int parameterIndex) {
        return null;
    }

    public Reader getNCharacterStream(String parameterName) {
        return null;
    }

    public Reader getCharacterStream(int parameterIndex) {
        return null;
    }

    public Reader getCharacterStream(String parameterName) {
        return null;
    }

    public void setBlob(String parameterName, Blob x) {
        //do nothing
    }

    public void setClob(String parameterName, Clob x) {
        //do nothing
    }


    public void setAsciiStream(String parameterName, InputStream x, long length) {
        //do nothing
    }

    public void setBinaryStream(String parameterName, InputStream x, long length) {
        //do nothing
    }

    public void setCharacterStream(String parameterName, Reader reader, long length) {
        //do nothing
    }

    public void setAsciiStream(String parameterName, InputStream x) {
        //do nothing
    }

    public void setBinaryStream(String parameterName, InputStream x) {
        //do nothing
    }

    public void setCharacterStream(String parameterName, Reader reader) {
        //do nothing
    }

    public void setNCharacterStream(String parameterName, Reader value) {
        //do nothing
    }

    public void setClob(String parameterName, Reader reader) {
        //do nothing
    }

    public void setBlob(String parameterName, InputStream inputStream) {
        //do nothing
    }

    public void setNClob(String parameterName, Reader reader) {
        //do nothing
    }

    public <T> T getObject(int parameterIndex, Class<T> type) {
        return null;
    }

    public <T> T getObject(String parameterName, Class<T> type) {
        return null;
    }
}
