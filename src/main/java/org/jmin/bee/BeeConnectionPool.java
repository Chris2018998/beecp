/*
 * Copyright (C) Chris Liao
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
package org.jmin.bee;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * BeeCP interface
 * 
 * @author Chris Liao
 */
public interface BeeConnectionPool {

	/**
	 * initialize pool by configuration
	 */
	public void init(BeeDataSourceConfig info) throws SQLException;

	/**
	 * borrow an connection from pool
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * borrow an connection from pool with specified milliseconds
	 */
	public Connection getConnection(long maxWaitTime) throws SQLException;

	/**
	 * close pool
	 */
	public void destroy();

}
