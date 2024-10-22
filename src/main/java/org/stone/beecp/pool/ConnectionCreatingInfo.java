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
 * Connection creator info
 *
 * @author Chris liao
 * @version 1.0
 */
final class ConnectionCreatingInfo {
    final Thread creatingThread;
    final long creatingStartTime;

    ConnectionCreatingInfo() {
        this.creatingThread = Thread.currentThread();
        this.creatingStartTime = System.nanoTime();
    }
}