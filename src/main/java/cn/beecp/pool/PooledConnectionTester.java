/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

/**
 * Pooled Connection Tester
 *
 * @author Chris.Liao
 * @version 1.0
 */
interface PooledConnectionTester {
    boolean isAlive(PooledConnection pCon);
}
