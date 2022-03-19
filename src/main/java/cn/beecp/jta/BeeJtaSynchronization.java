/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.jta;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jta DataSource Synchronization
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class BeeJtaSynchronization implements Synchronization {
    private final Transaction transaction;
    private final ConcurrentHashMap<Transaction, Connection> transactionMap;

    BeeJtaSynchronization(Transaction transaction, ConcurrentHashMap<Transaction, Connection> transactionMap) {
        this.transaction = transaction;
        this.transactionMap = transactionMap;
    }

    public void beforeCompletion() {
    }

    public void afterCompletion(int status) {
        this.transactionMap.remove(this.transaction);
    }
}
