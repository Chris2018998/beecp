/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockResultSetMetaData extends MockBase implements ResultSetMetaData {
    public int getColumnCount() throws SQLException {
        return 1;
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        return true;
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        return true;
    }

    public boolean isSearchable(int column) throws SQLException {
        return true;
    }

    public boolean isCurrency(int column) throws SQLException {
        return true;
    }

    public int isNullable(int column) throws SQLException {
        return 1;
    }

    public boolean isSigned(int column) throws SQLException {
        return true;
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        return 1;
    }

    public String getColumnLabel(int column) throws SQLException {
        return "";
    }

    public String getColumnName(int column) throws SQLException {
        return "";
    }

    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    public int getPrecision(int column) throws SQLException {
        return 1;
    }

    public int getScale(int column) throws SQLException {
        return 1;
    }

    public String getTableName(int column) throws SQLException {
        return "";
    }

    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    public int getColumnType(int column) throws SQLException {
        return 1;
    }

    public String getColumnTypeName(int column) throws SQLException {
        return "";
    }

    public boolean isReadOnly(int column) throws SQLException {
        return true;
    }

    public boolean isWritable(int column) throws SQLException {
        return true;
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        return true;
    }

    public String getColumnClassName(int column) throws SQLException {
        return "";
    }
}
