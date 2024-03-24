package org.stone.beecp.factory;

import org.stone.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;

public class NullXaConnectionFactory extends SimpleConnectionFactory implements RawXaConnectionFactory {

    public XAConnection create() {
        return null;
    }
}