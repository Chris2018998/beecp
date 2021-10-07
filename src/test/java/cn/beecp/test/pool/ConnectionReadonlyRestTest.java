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
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;

public class ConnectionReadonlyRestTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con1 = null;
        try {
            con1 = ds.getConnection();
            con1.setReadOnly(true);
            if (!con1.isReadOnly()) TestUtil.assertError("Connection Readonly set error");
//            con1.setReadOnly(false);
//            if (con1.isReadOnly()) TestUtil.assertError("Connection Readonly set error");
        } finally {
            if (con1 != null) con1.close();
        }

        Connection con2 = null;
        try {
            con2 = ds.getConnection();
            if (con2.isReadOnly()) TestUtil.assertError("Connection Readonly reset error");
        } finally {
            if (con2 != null) con2.close();
        }
    }
}
