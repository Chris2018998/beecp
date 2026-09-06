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
package org.stone.test.beecp.objects.listener;

import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.BeeMethodLogListener;

import java.sql.SQLException;
import java.util.List;

/**
 * SQL
 *
 * @author Chris Liao
 */
public class SQLWallPreparedStatementExecutionListener implements BeeMethodLogListener {
    private final String targetSQL;

    public SQLWallPreparedStatementExecutionListener(String targetSQL) {
        this.targetSQL = targetSQL;
    }

    public void onMethodStart(BeeMethodLog log) throws SQLException {
        if (log.getType() == BeeMethodLog.Type_Statement_Log) {
            if (log.getParameters() == null && log.getMethod().contains("execute")) {
                checkSQL(log.getSql());
            }
        }
    }

    private void checkSQL(String sql) throws SQLException {
        if (sql != null && sql.equals(this.targetSQL))
            throw new SQLException("SQL check failed in Wall");
    }

    public void onMethodEnd(BeeMethodLog log) throws SQLException {

    }

    public List<Boolean> onLongRunningDetected(List<BeeMethodLog> logList) {
        return null;
    }


//
//         else if (log.getType() == BeeMethodLog.Type_Statement_Log) {
//        Object[] parameters = log.getParameters();
//        if (log.getMethod().startsWith("Statement.execute") && parameters != null && parameters.length > 0) {
//            checkSQL((String)parameters[0]);
//        }
//    }
}
