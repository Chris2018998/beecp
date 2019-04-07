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

import java.sql.Statement;

/**
 * Statement proxy class
 * 
 * @author Chris.Liao
 * @version 1.0
 */

public abstract class ProxyStatement extends ProxyStatementWrapper implements Statement {
	public ProxyStatement(Statement delegate,ProxyConnection proxyConnection) {
		super(delegate,proxyConnection,false);
	}
}
