package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.MockDbCrashConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0058PoolRecoveryTest extends TestCase {

    public void testRecoveryAfterDBRestoreTest() throws SQLException {
        MockDbCrashConnectionFactory factory = new MockDbCrashConnectionFactory();

        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(3);
        config.setMaxActive(3);
        config.setPrintRuntimeLog(true);
        config.setRawConnectionFactory(factory);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertEquals(3, pool.getTotalSize());
        Assert.assertEquals(3, pool.getIdleSize());

        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();

        try {
            LockSupport.parkNanos(MILLISECONDS.toNanos(600));
            factory.setErrorCode(0b010000);
            factory.setErrorState("57P02");
            factory.setCrashed(true);//<----db crash here,all pooled connections dead
            pool.getConnection();
        } catch (SQLException e) {
            Assert.assertEquals("Unlucky,your db has crashed", e.getMessage());//message from mock factory
            Assert.assertEquals(0, pool.getTotalSize());//
            String logs = logAppender.endCollectedStoneLog();
            Assert.assertTrue(logs.contains("failed to test connection with 'isAlive' method"));
            Assert.assertTrue(logs.contains("for cause:bad"));
        }

        //2: db restore here
        factory.setCrashed(false);
        factory.setErrorCode(0);
        factory.setErrorState(null);
        Connection con = pool.getConnection();
        Assert.assertNotNull(con);
        con.close();
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }
}
