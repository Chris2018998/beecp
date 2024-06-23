package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.BorrowThread;
import org.stone.beecp.objects.MockNetBlockConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionCreateException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.tools.BeanUtil.setAccessible;

public class Tc0053GetTimeoutTest extends TestCase {
    public void testGetTimeoutOnSemaphore() throws SQLException {
        //1:timeout on pool semaphore
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setMaxActive(2);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        config.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        BorrowThread first = new BorrowThread(pool);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);
        try {
            pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool semaphore acquisition"));
            first.interrupt();
        }
        pool.close();
    }

    public void testGetTimeoutOnPoolLock() throws SQLException {
        //2:timeout on pool lock
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(0);
        config2.setMaxActive(2);
        config2.setBorrowSemaphoreSize(2);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        config2.setRawConnectionFactory(new MockNetBlockConnectionFactory());
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);

        BorrowThread first = new BorrowThread(pool2);//mock stuck in driver.getConnection()
        first.start();
        TestUtil.joinUtilWaiting(first);
        try {
            pool2.getConnection();
        } catch (ConnectionCreateException e) {
            Assert.assertTrue(e.getMessage().contains("Wait timeout on pool lock acquisition"));
            first.interrupt();
        }
        pool2.close();
    }

    public void testGetTimeoutOnWaitQueue() throws Exception {
        //3: timeout in wait queue
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(0);
        config3.setMaxActive(1);
        config3.setForceCloseUsingOnClear(true);
        config3.setBorrowSemaphoreSize(2);
        config3.setMaxWait(TimeUnit.SECONDS.toMillis(2));
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.init(config3);
        BorrowThread first = new BorrowThread(pool3);
        first.start();
        first.join();

        try {
            pool3.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertTrue(e.getMessage().contains("Wait timeout for a released connection"));
        }

        //mock concurrent to create connection
        Method createMethod = FastConnectionPool.class.getDeclaredMethod("createPooledConn", Integer.TYPE);
        setAccessible(createMethod);
        Assert.assertNull(createMethod.invoke(pool3, 1));
        oclose(first.getConnection());
        pool3.close();
    }
}
