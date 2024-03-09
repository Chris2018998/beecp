package org.stone.beecp.issue.HikariCP.issue2176;

import org.stone.beecp.BeeDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static org.stone.beecp.issue.HikariCP.issue2176.SnowConstants.Snow_Key;

public class SnowDataSource implements DataSource {

    private BeeDataSource ds;

    private SnowflakeKeyStore keys;

    SnowDataSource(SnowflakeKeyStore keys, BeeDataSource ds) {
        this.ds = ds;
        this.keys = keys;
    }

    public Connection getConnection() throws SQLException {
        Connection con = ds.getConnection();
        String key = con.getClientInfo(Snow_Key);//get attached key from connection

        if (keys.existsKey(key)) {//key expired check
            return con;
        } else {
            con.abort(null);//removed connection with expired key
            throw new KeyExpiredException();
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not support");
    }

    public final PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public final void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public final void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported");
    }
}
