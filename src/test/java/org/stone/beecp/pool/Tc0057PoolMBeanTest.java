package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beecp.BeeDataSourceConfig;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.base.TestUtil.getStoneLogAppender;
import static org.stone.base.TestUtil.oclose;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0057PoolMBeanTest extends TestCase {
    public void testJmxRegister() throws Exception {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setEnableJmx(true);
        config.setPoolName(poolName);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        String name1 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        String name2 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assert.assertTrue(mBeanServer.isRegistered(jmxRegName1));
        Assert.assertTrue(mBeanServer.isRegistered(jmxRegName2));
        pool.close();
        Assert.assertFalse(mBeanServer.isRegistered(jmxRegName1));
        Assert.assertFalse(mBeanServer.isRegistered(jmxRegName2));
    }

    public void testJmxBeanMethods() throws Exception {
        String poolName = "test";
        BeeDataSourceConfig config = createDefault();
        config.setPoolName(poolName);
        config.setEnableJmx(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeDataSourceConfig config2 = createDefault();
        config2.setPoolName(poolName);
        config2.setEnableJmx(true);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertEquals(poolName, pool2.getPoolName());

        String name1 = "BeeDataSourceConfig:type=BeeCP(" + poolName + ")-config";
        String name2 = "FastConnectionPool:type=BeeCP(" + poolName + ")";
        ObjectName jmxRegName1 = new ObjectName(name1);
        ObjectName jmxRegName2 = new ObjectName(name2);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assert.assertTrue(mBeanServer.isRegistered(jmxRegName1));
        Assert.assertTrue(mBeanServer.isRegistered(jmxRegName2));

    }

    public void testOnPrintRuntimeLog() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: not print
        StoneLogAppender logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.setPrintRuntimeLog(false);//not print
        Connection con = null;
        try {
            con = pool.getConnection();
        } finally {
            oclose(con);
        }
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.isEmpty());

        //2: print runtime log
        pool.clear(false);//remove all connection
        logAppender = getStoneLogAppender();
        logAppender.beginCollectStoneLog();
        pool.setPrintRuntimeLog(true);//print
        Connection con2 = null;
        try {
            con2 = pool.getConnection();
        } finally {
            oclose(con2);
        }
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
    }
}


