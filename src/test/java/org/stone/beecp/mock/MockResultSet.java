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
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 * @author Chris Liao
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
        if (statement != returnNull()) statement.resultSet = null;
    }

    public boolean next() {
        return returnFalse();
    }

    public Statement getStatement() {
        return statement;
    }

    public ResultSetMetaData getMetaData() {
        return new MockResultSetMetaData();
    }

    public boolean wasNull() {
        return returnFalse();
    }

    public String getString(int columnIndex) {
        return "";
    }

    public boolean getBoolean(int columnIndex) {
        return true;
    }

    public byte getByte(int columnIndex) {
        return (byte) returnNumberOne();
    }

    public short getShort(int columnIndex) {
        return (short) returnNumberOne();
    }

    public int getInt(int columnIndex) {
        return returnNumberOne();
    }

    public long getLong(int columnIndex) {
        return returnNumberOne();
    }

    public float getFloat(int columnIndex) {
        return (float) returnNumberOne();
    }

    public double getDouble(int columnIndex) {
        return returnNumberOne();
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) {
        return new BigDecimal(1);
    }

    public byte[] getBytes(int columnIndex) {
        return (byte[]) returnNull();
    }

    public Date getDate(int columnIndex) {
        return (Date) returnNull();
    }

    public Time getTime(int columnIndex) {
        return (Time) returnNull();
    }

    public Timestamp getTimestamp(int columnIndex) {
        return (Timestamp) returnNull();
    }

    public InputStream getAsciiStream(int columnIndex) {
        return (InputStream) returnNull();
    }

    public InputStream getUnicodeStream(int columnIndex) {
        return (InputStream) returnNull();
    }

    public InputStream getBinaryStream(int columnIndex) {
        return (InputStream) returnNull();
    }

    public String getString(String columnLabel) {
        return (String) returnNull();
    }

    public boolean getBoolean(String columnLabel) {
        return true;
    }

    public byte getByte(String columnLabel) {
        return (byte) returnNumberOne();
    }

    public short getShort(String columnLabel) {
        return (short) returnNumberOne();
    }

    public int getInt(String columnLabel) {
        return returnNumberOne();
    }

    public long getLong(String columnLabel) {
        return returnNumberOne();
    }

    public float getFloat(String columnLabel) {
        return returnNumberOne();
    }

    public double getDouble(String columnLabel) {
        return returnNumberOne();
    }

    public BigDecimal getBigDecimal(String columnLabel, int scale) {
        return new BigDecimal(1);
    }

    public byte[] getBytes(String columnLabel) {
        return (byte[]) returnNull();
    }

    public Date getDate(String columnLabel) {
        return (Date) returnNull();
    }

    public Time getTime(String columnLabel) {
        return (Time) returnNull();
    }

    public Timestamp getTimestamp(String columnLabel) {
        return (Timestamp) returnNull();
    }

    public InputStream getAsciiStream(String columnLabel) {
        return (InputStream) returnNull();
    }

    public InputStream getUnicodeStream(String columnLabel) {
        return (InputStream) returnNull();
    }

    public InputStream getBinaryStream(String columnLabel) {
        return (InputStream) returnNull();
    }

    public SQLWarning getWarnings() {
        return (SQLWarning) returnNull();
    }

    public void clearWarnings() {
        //do nothing
    }

    public String getCursorName() {
        return (String) returnNull();
    }

    public Object getObject(int columnIndex) {
        return returnNull();
    }

    public Object getObject(String columnLabel) {
        return returnNull();
    }

    public int findColumn(String columnLabel) {
        return returnNumberOne();
    }

    public Reader getCharacterStream(int columnIndex) {
        return (Reader) returnNull();
    }

    public Reader getCharacterStream(String columnLabel) {
        return (Reader) returnNull();
    }

    public BigDecimal getBigDecimal(int columnIndex) {
        return (BigDecimal) returnNull();
    }

    public BigDecimal getBigDecimal(String columnLabel) {
        return (BigDecimal) returnNull();
    }

    public boolean isBeforeFirst() {
        return returnFalse();
    }

    public boolean isAfterLast() {
        return returnFalse();
    }

    public boolean isFirst() {
        return returnFalse();
    }

    public boolean isLast() {
        return returnFalse();
    }

    public void beforeFirst() {
        //do nothing
    }

    public void afterLast() {
        //do nothing
    }

    public boolean first() {
        return returnFalse();
    }

    public boolean last() {
        return returnFalse();
    }

    public int getRow() {
        return returnNumberOne();
    }

    public boolean absolute(int row) {
        return true;
    }

    public boolean relative(int rows) {
        return true;
    }

    public boolean previous() {
        return true;
    }

    public int getFetchDirection() {
        return returnNumberOne();
    }

    public void setFetchDirection(int direction) {
        //do nothing
    }

    public int getFetchSize() {
        return returnNumberOne();
    }

    public void setFetchSize(int rows) {
        //do nothing
    }

    public int getType() {
        return returnNumberOne();
    }

    public int getConcurrency() {
        return returnNumberOne();
    }

    public boolean rowUpdated() {
        return true;
    }

    public boolean rowInserted() {
        return true;
    }

    public boolean rowDeleted() {
        return true;
    }

    public void updateNull(int columnIndex) {
        //do nothing
    }

    public void updateBoolean(int columnIndex, boolean x) {
        //do nothing
    }

    public void updateByte(int columnIndex, byte x) {
        //do nothing
    }

    public void updateShort(int columnIndex, short x) {
        //do nothing
    }

    public void updateInt(int columnIndex, int x) {
        //do nothing
    }

    public void updateLong(int columnIndex, long x) {
        //do nothing
    }

    public void updateFloat(int columnIndex, float x) {
        //do nothing
    }

    public void updateDouble(int columnIndex, double x) {
        //do nothing
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) {
        //do nothing
    }

    public void updateString(int columnIndex, String x) {
        //do nothing
    }

    public void updateBytes(int columnIndex, byte[] x) {
        //do nothing
    }

    public void updateDate(int columnIndex, Date x) {
        //do nothing
    }

    public void updateTime(int columnIndex, Time x) {
        //do nothing
    }

    public void updateTimestamp(int columnIndex, Timestamp x) {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) {
        //do nothing
    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength) {
        //do nothing
    }

    public void updateObject(int columnIndex, Object x) {
        //do nothing
    }

    public void updateNull(String columnLabel) {
        //do nothing
    }

    public void updateBoolean(String columnLabel, boolean x) {
        //do nothing
    }

    public void updateByte(String columnLabel, byte x) {
        //do nothing
    }

    public void updateShort(String columnLabel, short x) {
        //do nothing
    }

    public void updateInt(String columnLabel, int x) {
        //do nothing
    }

    public void updateLong(String columnLabel, long x) {
        //do nothing
    }

    public void updateFloat(String columnLabel, float x) {
        //do nothing
    }

    public void updateDouble(String columnLabel, double x) {
        //do nothing
    }

    public void updateBigDecimal(String columnLabel, BigDecimal x) {
        //do nothing
    }

    public void updateString(String columnLabel, String x) {
        //do nothing
    }

    public void updateBytes(String columnLabel, byte[] x) {
        //do nothing
    }

    public void updateDate(String columnLabel, Date x) {
        //do nothing
    }

    public void updateTime(String columnLabel, Time x) {
        //do nothing
    }

    public void updateTimestamp(String columnLabel, Timestamp x) {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length) {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length) {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length) {
        //do nothing
    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength) {
        //do nothing
    }

    public void updateObject(String columnLabel, Object x) {
        //do nothing
    }

    public void insertRow() {
        //do nothing
    }

    public void updateRow() {
        //do nothing
    }

    public void deleteRow() {
        //do nothing
    }

    public void refreshRow() {
        //do nothing
    }

    public void cancelRowUpdates() {
        //do nothing
    }

    public void moveToInsertRow() {
        //do nothing
    }

    public void moveToCurrentRow() {
        //do nothing
    }

    public Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) {
        return returnNull();
    }

    public Ref getRef(int columnIndex) {
        return (Ref) returnNull();
    }

    public Blob getBlob(int columnIndex) {
        return (Blob) returnNull();
    }

    public Clob getClob(int columnIndex) {
        return (Clob) returnNull();
    }

    public Array getArray(int columnIndex) {
        return (Array) returnNull();
    }

    public Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) {
        return returnNull();
    }

    public Ref getRef(String columnLabel) {
        return (Ref) returnNull();
    }

    public Blob getBlob(String columnLabel) {
        return (Blob) returnNull();
    }

    public Clob getClob(String columnLabel) {
        return (Clob) returnNull();
    }

    public Array getArray(String columnLabel) {
        return (Array) returnNull();
    }

    public Date getDate(int columnIndex, Calendar cal) {
        return (Date) returnNull();
    }

    public Date getDate(String columnLabel, Calendar cal) {
        return (Date) returnNull();
    }

    public Time getTime(int columnIndex, Calendar cal) {
        return (Time) returnNull();
    }

    public Time getTime(String columnLabel, Calendar cal) {
        return (Time) returnNull();
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) {
        return (Timestamp) returnNull();
    }

    public Timestamp getTimestamp(String columnLabel, Calendar cal) {
        return (Timestamp) returnNull();
    }

    public URL getURL(int columnIndex) {
        return (URL) returnNull();
    }

    public URL getURL(String columnLabel) {
        return (URL) returnNull();
    }

    public void updateRef(int columnIndex, Ref x) {
        //do nothing
    }

    public void updateRef(String columnLabel, Ref x) {
        //do nothing
    }

    public void updateBlob(int columnIndex, Blob x) {
        //do nothing
    }

    public void updateBlob(String columnLabel, Blob x) {
        //do nothing
    }

    public void updateClob(int columnIndex, Clob x) {
        //do nothing
    }

    public void updateClob(String columnLabel, Clob x) {
        //do nothing
    }

    public void updateArray(int columnIndex, Array x) {
        //do nothing
    }

    public void updateArray(String columnLabel, Array x) {
        //do nothing
    }

    public RowId getRowId(int columnIndex) {
        return (RowId) returnNull();
    }

    public RowId getRowId(String columnLabel) {
        return (RowId) returnNull();
    }

    public void updateRowId(int columnIndex, RowId x) {
        //do nothing
    }

    public void updateRowId(String columnLabel, RowId x) {
        //do nothing
    }

    public int getHoldability() {
        return returnNumberOne();
    }

    public void updateNString(int columnIndex, String nString) {
        //do nothing
    }

    public void updateNString(String columnLabel, String nString) {
        //do nothing
    }

    public void updateNClob(int columnIndex, NClob nClob) {
        //do nothing
    }

    public void updateNClob(String columnLabel, NClob nClob) {
        //do nothing
    }

    public NClob getNClob(int columnIndex) {
        return (NClob) returnNull();
    }

    public NClob getNClob(String columnLabel) {
        return (NClob) returnNull();
    }

    public SQLXML getSQLXML(int columnIndex) {
        return (SQLXML) returnNull();
    }

    public SQLXML getSQLXML(String columnLabel) {
        return (SQLXML) returnNull();
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) {
        //do nothing
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) {
        //do nothing
    }

    public String getNString(int columnIndex) {
        return (String) returnNull();
    }

    public String getNString(String columnLabel) {
        return (String) returnNull();
    }

    public Reader getNCharacterStream(int columnIndex) {
        return (Reader) returnNull();
    }

    public Reader getNCharacterStream(String columnLabel) {
        return (Reader) returnNull();
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) {
        //do nothing
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) {
        //do nothing
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) {
        //do nothing
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) {
        //do nothing
    }

    public void updateClob(int columnIndex, Reader reader, long length) {
        //do nothing
    }

    public void updateClob(String columnLabel, Reader reader, long length) {
        //do nothing
    }

    public void updateNClob(int columnIndex, Reader reader, long length) {
        //do nothing
    }

    public void updateNClob(String columnLabel, Reader reader, long length) {
        //do nothing
    }

    public void updateNCharacterStream(int columnIndex, Reader x) {
        //do nothing
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) {
        //do nothing
    }

    public void updateAsciiStream(int columnIndex, InputStream x) {
        //do nothing
    }

    public void updateBinaryStream(int columnIndex, InputStream x) {
        //do nothing
    }

    public void updateCharacterStream(int columnIndex, Reader x) {
        //do nothing
    }

    public void updateAsciiStream(String columnLabel, InputStream x) {
        //do nothing
    }

    public void updateBinaryStream(String columnLabel, InputStream x) {
        //do nothing
    }

    public void updateCharacterStream(String columnLabel, Reader reader) {
        //do nothing
    }

    public void updateBlob(int columnIndex, InputStream inputStream) {
        //do nothing
    }

    public void updateBlob(String columnLabel, InputStream inputStream) {
        //do nothing
    }

    public void updateClob(int columnIndex, Reader reader) {
        //do nothing
    }

    public void updateClob(String columnLabel, Reader reader) {
        //do nothing
    }

    public void updateNClob(int columnIndex, Reader reader) {
        //do nothing
    }

    public void updateNClob(String columnLabel, Reader reader) {
        //do nothing
    }

    public <T> T getObject(int columnIndex, Class<T> type) {
        return (T) returnNull();
    }

    public <T> T getObject(String columnLabel, Class<T> type) {
        return (T) returnNull();
    }

    private Object returnNull() {
        return null;
    }

    private int returnNumberOne() {
        return returnNumberOne();
    }

    private boolean returnFalse() {
        return false;
    }
}
