package org.stone.beecp.dataSource;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.util.concurrent.locks.LockSupport;

public class BlockingConnectionFactory implements RawConnectionFactory {
    public Connection create() {
        LockSupport.park();
        return null;
    }
}