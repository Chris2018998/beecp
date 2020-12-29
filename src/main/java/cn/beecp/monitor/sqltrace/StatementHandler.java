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
package cn.beecp.boot.monitor.sqltrace;

import cn.beecp.boot.SystemUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

/**
 * @author Chris.Liao
 */
public class StatementHandler implements InvocationHandler {
    private static final String Execute = "execute";
    private String poolName;
    private Statement statement;
    private String statementType;
    private SqlTraceEntry traceEntry;

    public StatementHandler(Statement statement, String statementType, String poolName) {
        this(statement, statementType, poolName, null);
    }

    public StatementHandler(Statement statement, String statementType, String poolName, String sql) {
        this.poolName = poolName;
        this.statement = statement;
        this.statementType = statementType;
        if (!SystemUtil.isBlank(sql)) {
            traceEntry = new SqlTraceEntry(sql, poolName, statementType);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().startsWith(Execute)) {//execute method
            if (args == null || args.length == 0) {
                if (traceEntry != null) {
                    return SqlTracePool.getInstance().executeStatement(traceEntry, statement, method, args, poolName);
                } else
                    return method.invoke(statement, args);
            } else {
                SqlTraceEntry sqlVo = new SqlTraceEntry(poolName, (String) args[0], statementType);
                return SqlTracePool.getInstance().executeStatement(sqlVo, statement, method, args, poolName);
            }
        } else {
            return method.invoke(statement, args);
        }
    }
}
