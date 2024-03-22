package org.stone.beecp.config;

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
