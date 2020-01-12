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

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Define some pool exceptions
 *
 * @author Chris.Liao
 * @version 1.0
 */
class PoolExceptionList {

	static final SQLTimeoutException RequestTimeoutException = new SQLTimeoutException("Connection timeout");

	static final SQLException RequestInterruptException = new SQLException("Request interrupt");
	
	static final SQLException PoolCloseException = new SQLException("Pool has been closed or in resting");
	
	static final SQLException WaitTimeException = new SQLException("Wait time must be greater than zero");

    static final SQLException ConnectionClosedException = new SQLException("Connection has been closed");
	
	static final SQLException StatementClosedException = new SQLException("Statement has been closed");
	
	static final SQLException ResultSetClosedException = new SQLException("ResultSet has been closed");

	static final SQLException FeatureNotSupportedException = new SQLException("Feature not supported");

	static final SQLException AutoCommitChangeForbiddennException = new SQLException("Execute 'commit' or 'rollback' before this operation");

}	
