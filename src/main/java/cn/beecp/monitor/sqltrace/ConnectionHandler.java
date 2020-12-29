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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

/**
 * @author Chris.Liao
 */
public class ConnectionHandler implements InvocationHandler {
    private static final String Type_Statement = "Statement";
    private static final String Type_PreparedStatement = "PreparedStatement";
    private static final String Type_CallableStatement = "CallableStatement";
    private String poolName;
    private Connection connection;

    public ConnectionHandler(Connection connection, String poolName) {
        this.poolName = poolName;
        this.connection = connection;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Object re = method.invoke(connection, args);
        if (name == "createStatement") {
            return ProxyFactory.createStatementProxy((Statement) re, Type_Statement, poolName, null);
        } else if (name == "prepareStatement") {
            return ProxyFactory.createStatementProxy((Statement) re, Type_PreparedStatement, poolName, (String) args[0]);
        } else if (name == "prepareCall") {
            return ProxyFactory.createStatementProxy((Statement) re, Type_CallableStatement, poolName, (String) args[0]);
        } else {
            return re;
        }
    }
}
