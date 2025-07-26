/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.beecp.other.slow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.base.TestUtil;

import java.util.concurrent.CyclicBarrier;

import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * max active size test,detail,please visit: HikariCP issue #2156 link
 * <a href="https://github.com/brettwooldridge/HikariCP/issues/2156">...</a>
 */

public class SlowSqlConcurrentTest {

    private BeeDataSource ds;

    public static void main(String[] args) throws Throwable {
        SlowSqlConcurrentTest test = new SlowSqlConcurrentTest();
        test.setUp();
        test.testSqlConcurrent();
        test.tearDown();
    }

    @BeforeEach
    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);// give valid URL
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setInitialSize(0);
        config.setMaxActive(30);
        ds = new BeeDataSource(config);
    }

    @AfterEach
    public void tearDown() {
        ds.close();
    }

    @Test
    public void testSqlConcurrent() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        Assertions.assertEquals(0, pool.getTotalSize());

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
        Assertions.assertEquals(30, total);
    }


}
