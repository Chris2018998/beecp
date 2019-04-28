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

import java.sql.CallableStatement;

/**
 * CallableStatement proxy super class
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyCsStatement extends ProxyStatementWrapper implements CallableStatement {
	public ProxyCsStatement(CallableStatement delegate, ProxyConnection proxyConnection,boolean cacheAble) {
		super(delegate,proxyConnection,cacheAble);
	}
}
