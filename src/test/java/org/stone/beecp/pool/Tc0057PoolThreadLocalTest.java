package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.lang.ref.WeakReference;
import java.sql.Connection;

import static org.stone.base.TestUtil.getFieldValue;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0057PoolThreadLocalTest extends TestCase {

    public void testThreadLocalCreation() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setEnableThreadLocal(true);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Assert.assertNotNull(getFieldValue(pool, "threadLocal"));
        pool.close();

        BeeDataSourceConfig config2 = createDefault();
        config2.setEnableThreadLocal(false);
        FastConnectionPool pool2 = new FastConnectionPool();
        pool2.init(config2);
        Assert.assertNull(getFieldValue(pool2, "threadLocal"));
        pool2.close();
    }

    public void testGetSameConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();
        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assert.assertEquals(con1Raw, con2Raw);
    }

    public void testGcOnConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();

        Object fieldVal = getFieldValue(pool, "threadLocal");
        Assert.assertTrue(fieldVal instanceof ThreadLocal);
        ThreadLocal threadLocal = (ThreadLocal) fieldVal;
        WeakReference weakRef = (WeakReference) threadLocal.get();
        Assert.assertNotNull(weakRef.get());
        System.gc();
        Assert.assertNull(weakRef.get());

        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assert.assertEquals(con1Raw, con2Raw);
    }

    public void testDisableThreadLocal() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setEnableThreadLocal(false);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con1 = pool.getConnection();
        Connection con1Raw = (Connection) getFieldValue(con1, ProxyConnectionBase.class, "raw");
        con1.close();
        Connection con2 = pool.getConnection();
        Connection con2Raw = (Connection) getFieldValue(con2, ProxyConnectionBase.class, "raw");
        con2.close();
        Assert.assertEquals(con1Raw, con2Raw);
    }
}
