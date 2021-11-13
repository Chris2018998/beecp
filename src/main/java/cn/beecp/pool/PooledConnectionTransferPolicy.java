/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

/**
 * Pooled Connection Transfer Policy
 *
 * @author Chris.Liao
 * @version 1.0
 */
interface PooledConnectionTransferPolicy {
    int getCheckStateCode();

    void beforeTransfer(PooledConnection p);

    boolean tryCatch(PooledConnection p);

    void onTransferFail(PooledConnection p);
}
