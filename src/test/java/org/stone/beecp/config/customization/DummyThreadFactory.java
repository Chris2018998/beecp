/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config.customization;

import org.stone.beecp.BeeConnectionPoolThreadFactory;

public class DummyThreadFactory implements BeeConnectionPoolThreadFactory {
    public Thread createIdleScanThread(Runnable runnable) {
        return new Thread(runnable);
    }

    public Thread createServantThread(Runnable runnable) {
        return new Thread(runnable);
    }

    public Thread createNetworkTimeoutThread(Runnable runnable) {
        return new Thread(runnable);
    }
}
