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
package cn.beecp.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Pool Constants definition
 *
 * @author Chris.Liao
 * @version 1.0
 */

class PoolConstants {
    //POOL STATE
    static final int POOL_UNINIT = 1;
    static final int POOL_NORMAL = 2;
    static final int POOL_CLOSED = 3;
    static final int POOL_RESTING = 4;

    //POOLED CONNECTION STATE
    static final int CONNECTION_IDLE = 1;
    static final int CONNECTION_USING = 2;
    static final int CONNECTION_CLOSED = 3;

    //ADD CONNECTION THREAD STATE
    static final int THREAD_WORKING = 1;
    static final int THREAD_WAITING = 2;
    static final int THREAD_DEAD = 3;

    //BORROWER STATE
    static final Object BORROWER_NORMAL = new Object();
    static final Object BORROWER_WAITING = new Object();


    static final SQLTimeoutException RequestTimeoutException = new SQLTimeoutException("Request timeout");

    static final SQLException RequestInterruptException = new SQLException("Request interrupt");

    static final SQLException PoolCloseException = new SQLException("Pool has been closed or in resting");

    static final SQLException ConnectionClosedException = new SQLException("No operations allowed after connection closed.");

    static final SQLException StatementClosedException = new SQLException("No operations allowed after statement closed.");

    static final SQLException ResultSetClosedException = new SQLException("No operations allowed after resultSet closed.");

    static final SQLException AutoCommitChangeForbiddenException = new SQLException("Execute 'commit' or 'rollback' before this operation");

    static final SQLException DriverNotSupportNetworkTimeoutException = new SQLException("Driver not support 'networkTimeout'");

    final static Connection DUMMY_CON = (Connection) Proxy.newProxyInstance(
            PoolConstants.class.getClassLoader(),
            new Class[]{Connection.class},
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    if (methodName == "isClosed") {
                        return true;
                    } else {
                        throw ConnectionClosedException;
                    }
                }
            }
    );
}