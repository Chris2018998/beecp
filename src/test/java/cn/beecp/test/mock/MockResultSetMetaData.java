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
