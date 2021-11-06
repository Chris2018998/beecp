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
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class PoolDelayInitializeFailedTest extends TestCase {
    private int initSize = 5;

    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    public void testPoolInit() throws InterruptedException, Exception {
        Connection con = null;
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            ds.setJdbcUrl("jdbc:bee://localhost/test/mockdb2");//give valid URL
            ds.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            ds.setUsername(JdbcConfig.JDBC_USER);
            ds.setPassword(JdbcConfig.JDBC_PASSWORD);
            ds.setInitialSize(initSize);
            con = ds.getConnection();
            TestUtil.assertError("A pool fail to init e need be thrown,but not");
        } catch (SQLException e) {
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
