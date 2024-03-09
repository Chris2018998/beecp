package org.stone.beecp.issue.HikariCP.issue2169;

import org.stone.beecp.BeeDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class MyDataSource implements DataSource {

    private BeeDataSource ds;//you can replace it with other dataSource(for example: HikariCP)

    private ConnectionCloseHook closeHook;//set your implementation on ConnectionCloseHook

    MyDataSource(BeeDataSource ds, ConnectionCloseHook closeHook) {
        this.ds = ds;
        this.closeHook = closeHook;
    }

    public Connection getConnection() throws SQLException {
        Connection con = ds.getConnection();
        ConnectionHandler handler = new ConnectionHandler(con, closeHook);
        return (Connection) Proxy.newProxyInstance(MyDataSource.class.getClassLoader(),
                new Class[]{Connection.class},
                handler);
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

