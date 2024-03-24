package org.stone.beecp.pool2;

import junit.framework.TestCase;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.dataSource.BlockingConnectionFactory;
import org.stone.beecp.dataSource.MockThreadToInterruptCreateLock;
import org.stone.beecp.factory.BlockingNullXaConnectionFactory;
import org.stone.beecp.factory.CountNullConnectionFactory;
import org.stone.beecp.factory.NullConnectionFactory;
import org.stone.beecp.factory.NullXaConnectionFactory;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionCreateTest extends TestCase {


    public void testInitialFailedConnectionSync() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setInitialSize(1);
        config.setRawConnectionFactory(new NullConnectionFactory());
        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
        }catch(ConnectionCreateException e){
            //do noting
        }
    }

    public void testInitialFailedConnectionASync() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setInitialSize(1);
        config.setAsyncCreateInitConnection(true);
        config.setRawConnectionFactory(new NullConnectionFactory());
        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
        }catch(ConnectionCreateException e){
            //do noting
        }
    }

    public void testInitialFailedConnection2() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setInitialSize(4);
        config.setRawConnectionFactory(new CountNullConnectionFactory(3));

        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
        }catch(ConnectionCreateException e){
            //do noting
        }
    }

    public void testNullConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawConnectionFactory(new NullConnectionFactory());

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con = null;
        try {
            con = pool.getConnection();
        } catch (SQLException e) {
            if (!(e instanceof ConnectionCreateException))
                throw e;
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }

    public void testNullXaConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawXaConnectionFactory(new NullXaConnectionFactory());

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        XAConnection con = null;
        try {
            con = pool.getXAConnection();
        } catch (SQLException e) {
            if (!(e instanceof ConnectionCreateException))
                throw e;
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }


    public void testInterruptCreateLock() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawConnectionFactory(new BlockingConnectionFactory());

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        new MockThreadToInterruptCreateLock(pool).start();

        Connection con = null;
        try {
            con = pool.getConnection();
        } catch (SQLException e) {
            if (!(e instanceof ConnectionGetInterruptedException))
                throw e;
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }

    public void testInterruptCreateLockX() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setRawXaConnectionFactory(new BlockingNullXaConnectionFactory());

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        new MockThreadToInterruptCreateLock(pool).start();

        XAConnection con = null;
        try {
            con = pool.getXAConnection();
        } catch (SQLException e) {
            if (!(e instanceof ConnectionGetInterruptedException))
                throw e;
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }






}
