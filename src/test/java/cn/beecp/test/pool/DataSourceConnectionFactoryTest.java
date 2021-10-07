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

public class DataSourceConnectionFactoryTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.addConnectProperty("url", Config.JDBC_URL);
        config.setConnectionFactoryClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.setInitialSize(5);
        config.setValidTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            if (con == null) TestUtil.assertError("DataSourceConnectionFactoryTest failed");
            System.out.println(con);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
