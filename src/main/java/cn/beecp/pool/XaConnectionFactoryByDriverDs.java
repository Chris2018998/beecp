package cn.beecp.pool;

import cn.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.SQLException;

import static cn.beecp.pool.PoolStaticCenter.isBlank;

/**
 * XaConnection Factory implementation by XADataSource
 *
 * @author Chris.liao
 * @version 1.0
 */
public class XaConnectionFactoryByDriverDs implements RawXaConnectionFactory {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final XADataSource dataSource;

    //Constructor
    public XaConnectionFactoryByDriverDs(XADataSource dataSource, String username, String password) {
        this.dataSource = dataSource;
        this.username = username;
        this.password = password;
        this.useUsername = !isBlank(username);
    }

    //create one connection
    public XAConnection create() throws SQLException {
        return useUsername ? dataSource.getXAConnection(username, password) : dataSource.getXAConnection();
    }
}