/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.other.slowSql;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.pool.FastConnectionPool;
import java.util.concurrent.CyclicBarrier;

/**
 * max active size test,detail,please visit: HikariCP issue #2156 link
 * https://github.com/brettwooldridge/HikariCP/issues/2156
 */

public class SlowSqlConcurrentTest extends TestCase {

    private BeeDataSource ds;

    public static void main(String[] args) throws Throwable {
        SlowSqlConcurrentTest test = new SlowSqlConcurrentTest();
        test.setUp();
        test.test();
        test.tearDown();
    }

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setInitialSize(0);
        config.setMaxActive(30);
        ds = new BeeDataSource(config);
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        if (pool.getTotalSize() != 0) TestUtil.assertError("Total initial connections not as expected 0");

        int count = 30;
        VisitThread[] threads = new VisitThread[count];
        CyclicBarrier barrier = new CyclicBarrier(count);
        for (int i = 0; i < count; i++) {
            threads[i] = new VisitThread(ds, barrier);
            threads[i].start();
        }
        for (int i = 0; i < 30; i++) {
            threads[i].join();
        }

        int total = pool.getTotalSize();
        if (total != 30) TestUtil.assertError("Total connections[" + total + "]not as expected 30");
    }

    public void tearDown() throws Throwable {
        ds.close();
    }
}
