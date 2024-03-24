package org.stone.beecp.factory;

import javax.sql.XAConnection;
import java.util.concurrent.locks.LockSupport;

public class BlockingNullXaConnectionFactory extends NullXaConnectionFactory {

    public XAConnection create() {
        LockSupport.park();
        return null;
    }
}
