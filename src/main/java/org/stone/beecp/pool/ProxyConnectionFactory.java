package org.stone.beecp.pool;

import java.sql.SQLException;

/**
 * proxy factory
 *
 * @author Chris Liao
 * @version 1.0
 */

class ProxyConnectionFactory {

    public ProxyConnectionBase createProxyConnection(PooledConnection p) throws SQLException {
        throw new SQLException("Proxy classes not be generated,please execute 'ProxyClassGenerator' after compile");
    }
}
