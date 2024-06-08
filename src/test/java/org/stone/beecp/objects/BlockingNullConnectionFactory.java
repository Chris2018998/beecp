package org.stone.beecp.objects;

import java.sql.Connection;
import java.util.concurrent.locks.LockSupport;

public class BlockingNullConnectionFactory extends NullConnectionFactory {

    public Connection create() {
        LockSupport.park();
        return null;
    }
}
