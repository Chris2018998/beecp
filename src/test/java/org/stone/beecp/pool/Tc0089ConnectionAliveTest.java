package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.driver.MockConnectionProperties;
import org.stone.beecp.objects.MockCommonConnectionFactory;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0089ConnectionAliveTest extends TestCase {

    public void testDeadConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLog(true);
        config.setAliveAssumeTime(0L);
        config.setMaxWait(TimeUnit.MILLISECONDS.toNanos(500L));

        MockConnectionProperties propertiesSet = new MockConnectionProperties();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(propertiesSet);
        config.setConnectionFactory(factory);

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(1, pool.getIdleSize());

        Connection con = null;
        try {
            con = pool.getConnection();
            con.close();
            con = null;

            LockSupport.parkNanos(500L);
            con = pool.getConnection();
            con.close();
            con = null;

            LockSupport.parkNanos(500L);
            propertiesSet.setValid(false);
            con = pool.getConnection();
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertEquals("Waited timeout for a released connection", e.getMessage());
        } finally {
            TestUtil.oclose(con);
            pool.close();
        }
    }

    public void testValidException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLog(true);
        config.setAliveAssumeTime(0L);
        config.setMaxWait(TimeUnit.MILLISECONDS.toNanos(500L));

        MockConnectionProperties propertiesSet = new MockConnectionProperties();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory(propertiesSet);
        config.setConnectionFactory(factory);

        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(1, pool.getIdleSize());

        Connection con = null;
        try {
            con = pool.getConnection();
            con.close();
            con = null;

            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
            propertiesSet.setMockException1(new SQLException());
            propertiesSet.enableExceptionOnMethod("isValid");
            StoneLogAppender logAppender = getStoneLogAppender();
            logAppender.beginCollectStoneLog();

            con = pool.getConnection();
            String logs = logAppender.endCollectedStoneLog();
            Assert.assertTrue(logs.contains("alive test failed on a borrowed connection"));
        } catch (ConnectionGetTimeoutException e) {
            Assert.assertEquals("Waited timeout for a released connection", e.getMessage());
        } finally {
            TestUtil.oclose(con);
            pool.close();
        }
    }
}
