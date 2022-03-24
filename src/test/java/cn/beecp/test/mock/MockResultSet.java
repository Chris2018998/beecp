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
public class MockResultSet extends MockBase implements ResultSet {
    private MockStatement statement;

    MockResultSet() {
    }

    MockResultSet(MockStatement statement) {
        this.statement = statement;
    }

    public void close() throws SQLException {
        super.close();
        if (statement != null) statement.resultSet = null;
    }

    public boolean next() throws SQLException {
        return false;
    }

    public Statement getStatement() throws SQLException {
        return statement;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MockResultSetMetaData();
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(int columnIndex) throws SQLException {
        return "";
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        return true;
    }

    public byte getByte(int columnIndex) throws SQLException {
        return 1;
    }

    public short getShort(int columnIndex) throws SQLException {
        return 1;
    }

    public int getInt(int columnIndex) throws SQLException {
        return 1;
    }

    public long getLong(int columnIndex) throws SQLException {
        return 1;
    }

    public float getFloat(int columnIndex) throws SQLException {
        return 1;
    }

    public double getDouble(int columnIndex) throws SQLException {
        return 1;
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return new BigDecimal(1);
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        return null;
    }

    public Date getDate(int columnIndex) throws SQLException {
        return null;
    }

    public Time getTime(int columnIndex) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return null;
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    public String getString(String columnLabel) throws SQLException {
        return null;
    }

    public boolean getBoolean(String columnLabel) throws SQLException {
        return true;
    }

    public byte getByte(String columnLabel) throws SQLException {
        return 1;
    }

    public short getShort(String columnLabel) throws SQLException {
        return 1;
    }

    public int getInt(String columnLabel) throws SQLException {
        return 1;
    }

    public long getLong(String columnLabel) throws SQLException {
        return 1;
    }

    public float getFloat(String columnLabel) throws SQLException {
        return 1;
    }

    public double getDouble(String columnLabel) throws SQLException {
        return 1;
    }

    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return new BigDecimal(1);
    }

    public byte[] getBytes(String columnLabel) throws SQLException {
        return null;
    }

    public Date getDate(String columnLabel) throws SQLException {
        return null;
    }

    public Time getTime(String columnLabel) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return null;
    }

    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return null;
    }

    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return null;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
        //do nothing
    }

    public String getCursorName() throws SQLException {
        return null;
    }

    public Object getObject(int columnIndex) throws SQLException {
        return null;
    }

    public Object getObject(String columnLabel) throws SQLException {
        return null;
    }

    public int findColumn(String columnLabel) throws SQLException {
        return 1;
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return null;
    }

    public boolean isBeforeFirst() throws SQLException {
        return false;
    }

    public boolean isAfterLast() throws SQLException {
        return false;
    }

    public boolean isFirst() throws SQLException {
        return false;
    }

    public boolean isLast() throws SQLException {
        return false;
    }

    public void beforeFirst() throws SQLException {
        //do nothing
    }

    public void afterLast() throws SQLException {
        //do nothing
    }

    public boolean first() throws SQLException {
        return false;
    }

    public boolean last() throws SQLException {
        return false;
    }

    public int getRow() throws SQLException {
        return 1;
    }

    public boolean absolute(int row) throws SQLException {
        return true;
    }

    public boolean relative(int rows) throws SQLException {
        return true;
    }

    public boolean previous() throws SQLException {
        return true;
    }

    public int getFetchDirection() throws SQLException {
        return 1;
    }

    public void setFetchDirection(int direction) throws SQLException {
        //do nothing
    }

    public int getFetchSize() throws SQLException {
        return 1;
    }

    public void setFetchSize(int rows) throws SQLException {
        //do nothing
    }

    public int getType() throws SQLException {
        return 1;
    }

    public int getConcurrency() throws SQLException {
        return 1;
    }

    public boolean rowUpdated() throws SQLException {
        return true;
    }

    public boolean rowInserted() throws SQLException {
        return true;
    }

    public boolean rowDeleted() throws SQLException {
        return true;
    }

    public void updateNull(int columnIndex) throws SQLException {
        //do nothing
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        //do nothing
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        //do nothing
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        //do nothing
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        //do nothing
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        //do nothing
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        //do nothing
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        //do nothing
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        //do nothing
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        //do nothing
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        //do nothing
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        //do nothing
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        //do nothing
    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        //do nothing
    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        //do nothing
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        //do nothing
    }

    public void updateNull(String columnLabel) throws SQLException {
        //do nothing
    }

    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        //do nothing
    }

    public void updateByte(String columnLabel, byte x) throws SQLException {
        //do nothing
    }

    public void updateShort(String columnLabel, short x) throws SQLException {
        //do nothing
    }

    public void updateInt(String columnLabel, int x) throws SQLException {
        //do nothing
    }

    public void updateLong(String columnLabel, long x) throws SQLException {
        //do nothing
    }

    public void updateFloat(String columnLabel, float x) throws SQLException {
        //do nothing
    }

    public void updateDouble(String columnLabel, double x) throws SQLException {
        //do nothing
    }

    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        //do nothing
    }

    public void updateString(String columnLabel, String x) throws SQLException {
        //do nothing
    }

    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        //do nothing
    }

    public void updateDate(String columnLabel, Date x) throws SQLException {
        //do nothing
    }

    public void updateTime(String columnLabel, Time x) throws SQLException {
        //do nothing
    }

    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        //do nothing
    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        //do nothing
    }

    public void updateObject(String columnLabel, Object x) throws SQLException {
        //do nothing
    }

    public void insertRow() throws SQLException {
        //do nothing
    }

    public void updateRow() throws SQLException {
        //do nothing
    }

    public void deleteRow() throws SQLException {
        //do nothing
    }

    public void refreshRow() throws SQLException {
        //do nothing
    }

    public void cancelRowUpdates() throws SQLException {
        //do nothing
    }

    public void moveToInsertRow() throws SQLException {
        //do nothing
    }

    public void moveToCurrentRow() throws SQLException {
        //do nothing
    }

    public Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(int columnIndex) throws SQLException {
        return null;
    }

    public Blob getBlob(int columnIndex) throws SQLException {
        return null;
    }

    public Clob getClob(int columnIndex) throws SQLException {
        return null;
    }

    public Array getArray(int columnIndex) throws SQLException {
        return null;
    }

    public Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(String columnLabel) throws SQLException {
        return null;
    }

    public Blob getBlob(String columnLabel) throws SQLException {
        return null;
    }

    public Clob getClob(String columnLabel) throws SQLException {
        return null;
    }

    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    public java.net.URL getURL(int columnIndex) throws SQLException {
        return null;
    }

    public java.net.URL getURL(String columnLabel) throws SQLException {
        return null;
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        //do nothing
    }

    public void updateRef(String columnLabel, Ref x) throws SQLException {
        //do nothing
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        //do nothing
    }

    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        //do nothing
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        //do nothing
    }

    public void updateClob(String columnLabel, Clob x) throws SQLException {
        //do nothing
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        //do nothing
    }

    public void updateArray(String columnLabel, Array x) throws SQLException {
        //do nothing
    }

    public RowId getRowId(int columnIndex) throws SQLException {
        return null;
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        return null;
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        //do nothing
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        //do nothing
    }

    public int getHoldability() throws SQLException {
        return 1;
    }

    public void updateNString(int columnIndex, String nString) throws SQLException {
        //do nothing
    }

    public void updateNString(String columnLabel, String nString) throws SQLException {
        //do nothing
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        //do nothing
    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        //do nothing
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        return null;
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        return null;
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return null;
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return null;
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        //do nothing
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        //do nothing
    }

    public String getNString(int columnIndex) throws SQLException {
        return null;
    }

    public String getNString(String columnLabel) throws SQLException {
        return null;
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        //do nothing
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        //do nothing
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        //do nothing
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        //do nothing
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        //do nothing
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        //do nothing
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        //do nothing
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        //do nothing
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        //do nothing
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        //do nothing
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        //do nothing
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        //do nothing
    }

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return null;
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return null;
    }

}
