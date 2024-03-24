package org.stone.beecp.pool2;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.ConfigFactory;
import org.stone.beecp.dataSource.BlockingConnectionFactory;
import org.stone.beecp.dataSource.MockThreadToInterruptCreateLock;
import org.stone.beecp.factory.*;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionCreateTest extends TestCase {

    public void testCreationFailureInFactory_Null() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setRawConnectionFactory(new NullConnectionFactory());
        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
            throw new TestException();
        } catch (ConnectionCreateException e) {
            //do nothing
        }
    }

    public void testCreationFailureInXaFactory_Null() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setRawXaConnectionFactory(new NullXaConnectionFactory());

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        XAConnection con = null;
        try {
            con = pool.getXAConnection();
            throw new TestException();
        } catch (ConnectionCreateException e) {
            //do nothing
        } finally {
            if (con != null) ConnectionPoolStatics.oclose(con);
        }
    }

    public void testCreationFailureInFactory_Exception() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setRawConnectionFactory(new ExceptionConnectionFactory());

        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
            throw new TestException();
        } catch (SQLException e) {
            //do noting
        }
    }

    public void testCreationFailureInXaFactory_Exception() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setRawXaConnectionFactory(new ExceptionXaConnectionFactory());

        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
            throw new TestException();
        } catch (SQLException e) {
            //do noting
        }
    }


    public void testInitialFailedConnectionASync() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setAsyncCreateInitConnection(true);
        config.setRawConnectionFactory(new NullConnectionFactory());
        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
        } catch (ConnectionCreateException e) {
            //do noting
        }
    }

    public void testInitialFailedConnection2() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
        config.setInitialSize(4);
        config.setRawConnectionFactory(new CountNullConnectionFactory(3));

        try {
            FastConnectionPool pool = new FastConnectionPool();
            pool.init(config);
        } catch (ConnectionCreateException e) {
            //do noting
        }
    }

    public void testNullConnection() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
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


    public void testInterruptCreateLock() throws Exception {
        BeeDataSourceConfig config = ConfigFactory.createDefault();
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
        BeeDataSourceConfig config = ConfigFactory.createDefault();
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
