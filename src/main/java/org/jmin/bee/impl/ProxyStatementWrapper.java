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
package org.jmin.bee.impl;

import java.sql.SQLException;

/**
 * ProxyBaseStatement
 * 
 * @author Chris
 * @version 1.0
 */
public class ProxyStatementWrapper {
	protected boolean isClosed;
	protected ProxyConnection proxyConnection;

	public ProxyStatementWrapper(ProxyConnection proxyConnection) {
		this.proxyConnection = proxyConnection;
	}

	public boolean isClosed() {
		return isClosed;
	}

	protected void updateLastActivityTime() throws SQLException {
		if (isClosed)throw new SQLException("Statement has been closed,access forbidden");
		this.proxyConnection.updateLastActivityTime();
	}
}
