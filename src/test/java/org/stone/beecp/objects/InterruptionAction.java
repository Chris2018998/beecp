/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

import org.stone.base.TestUtil;

public final class InterruptionAction extends Thread {
    private final Thread blockingThread;

    public InterruptionAction(Thread blockingThread) {
        this.blockingThread = blockingThread;
        this.setDaemon(true);
    }

    public void run() {
        if (TestUtil.joinUtilWaiting(blockingThread))
            blockingThread.interrupt();
    }
}
