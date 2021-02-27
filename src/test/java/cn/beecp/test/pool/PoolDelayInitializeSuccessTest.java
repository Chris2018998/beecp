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
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;

public class PoolDelayInitializeSuccessTest extends TestCase {
    private int initSize = 5;

    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    public void testPoolInit() throws InterruptedException, Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setJdbcUrl(Config.JDBC_URL);
        ds.setDriverClassName(Config.JDBC_DRIVER);
        ds.setUsername(Config.JDBC_USER);
        ds.setPassword(Config.JDBC_PASSWORD);
        ds.setInitialSize(initSize);

        Connection con = null;
        try {
            con = ds.getConnection();
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getPool(ds);
            if (pool.getConnTotalSize() != initSize)
                TestUtil.assertError("Total connections expected:%s,current is s%", initSize, pool.getConnTotalSize());
        } catch (ExceptionInInitializerError e) {
            e.getCause().printStackTrace();
        } finally {
            if (con != null)
                TestUtil.oclose(con);
            if (ds != null) ds.close();
        }
    }
}
