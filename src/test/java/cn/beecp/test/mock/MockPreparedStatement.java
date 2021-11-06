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
package cn.beecp.test.mock;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;

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
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    }

    public void setString(int parameterIndex, String x) throws SQLException {
    }

    public void setBytes(int parameterIndex, byte x[]) throws SQLException {
    }

    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
    }

    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException {
    }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
    }

    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
    }

    public void clearParameters() throws SQLException {
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SQLException {
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
    }

    public boolean execute() throws SQLException {
        return true;
    }

    public void addBatch() throws SQLException {
    }

    public void setCharacterStream(int parameterIndex,
                                   java.io.Reader reader,
                                   int length) throws SQLException {
    }


    public void setRef(int parameterIndex, Ref x) throws SQLException {
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
    }

    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException {
    }

    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
            throws SQLException {
    }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
            throws SQLException {
    }

    public void setNull(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
    }


    public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
    }

    public void setClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
    }

    public void setNClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x, long length)
            throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x,
                                long length) throws SQLException {
    }

    public void setCharacterStream(int parameterIndex,
                                   java.io.Reader reader,
                                   long length) throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x)
            throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x)
            throws SQLException {
    }

    public void setCharacterStream(int parameterIndex, java.io.Reader reader) throws SQLException {
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
    }

    public void setBlob(int parameterIndex, InputStream inputStream)
            throws SQLException {
    }

    public void setNClob(int parameterIndex, Reader reader)
            throws SQLException {
    }
}
