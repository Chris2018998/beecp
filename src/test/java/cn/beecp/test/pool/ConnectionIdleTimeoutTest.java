/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConnectionIdleTimeoutTest extends TestCase {
    private BeeDataSource ds;
    private int initSize = 5;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setInitialSize(initSize);
        config.setMaxActive(initSize);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(1000);
        config.setTimerCheckInterval(1000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        //CountDownLatch poolThreadLatch = (CountDownLatch) TestUtil.getFieldValue(pool, "poolThreadLatch");
        //if (poolThreadLatch.getCount() > 0) poolThreadLatch.await();

        if (pool.getConnTotalSize() != initSize) TestUtil.assertError("Total connections not as expected:" + initSize);
        if (pool.getConnIdleSize() != initSize) TestUtil.assertError("Idle connections not as expected:" + initSize);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

        if (pool.getConnTotalSize() != 0) TestUtil.assertError("Total connections not as expected:" + 0);
        if (pool.getConnIdleSize() != 0) TestUtil.assertError("Idle connections not a sexpected:" + 0);
    }
}
