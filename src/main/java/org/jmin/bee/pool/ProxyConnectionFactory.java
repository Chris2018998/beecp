/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.sql.SQLException;

/**
 * Connection proxy factory
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class ProxyConnectionFactory {
	public static ProxyConnection createProxyConnection(PooledConnection pooledConnection)throws SQLException{
		 throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassUtil' after project compile");
	}
}
