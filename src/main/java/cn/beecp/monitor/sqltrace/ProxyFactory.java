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

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;

/**
 * @author Chris.Liao
 */
public class ProxyFactory {
    private static final ClassLoader classLoader = ProxyFactory.class.getClassLoader();
    private static final Class[] INTF_Connection = new Class[]{Connection.class};
    private static final Class[] INTF_CallableStatement = new Class[]{CallableStatement.class};

    public static final Connection createConnection(Connection delegete, String poolName) {
        return (Connection) Proxy.newProxyInstance(
                classLoader,
                INTF_Connection,
                new ConnectionHandler(delegete, poolName)
        );
    }

    public static final Statement createStatementProxy(Statement delegete, String statementType, String poolName) {
        return createStatementProxy(delegete, statementType, poolName, null);
    }

    public static final Statement createStatementProxy(Statement delegete, String statementType, String poolName, String SQL) {
        return (Statement) Proxy.newProxyInstance(
                classLoader,
                INTF_CallableStatement,
                new StatementHandler(delegete, statementType, poolName, SQL)
        );
    }
}
