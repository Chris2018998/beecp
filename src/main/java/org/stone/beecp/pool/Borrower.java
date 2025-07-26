/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

/**
 * Pool Connection borrower
 *
 * @author Chris Liao
 * @version 1.0
 */
final class Borrower {
    final Thread thread;
    volatile Object state;
    PooledConnection lastUsed;

    Borrower(Thread thread) {
        this.thread = thread;
    }

    Borrower(Thread thread, PooledConnection lastUsed) {
        this.thread = thread;
        this.lastUsed = lastUsed;
    }
}