package org.stone.beecp.factory;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;

public class NullConnectionFactory extends SimpleConnectionFactory implements RawConnectionFactory {

    public Connection create() {
        return null;
    }
}
