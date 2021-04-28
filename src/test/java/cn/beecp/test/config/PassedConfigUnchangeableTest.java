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
package cn.beecp.test.config;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

public class PassedConfigUnchangeableTest extends TestCase {
    BeeDataSourceConfig testConfig;
    private BeeDataSource ds;
    private int initSize = 5;
    private int maxSize = 20;

    public void setUp() throws Throwable {
        testConfig = new BeeDataSourceConfig();
        String url = Config.JDBC_URL;
        testConfig.setJdbcUrl(url);
        testConfig.setDriverClassName(Config.JDBC_DRIVER);
        testConfig.setUsername(Config.JDBC_USER);
        testConfig.setPassword(Config.JDBC_PASSWORD);
        testConfig.setInitialSize(initSize);
        testConfig.setMaxActive(maxSize);
        testConfig.setConnectionTestSql("SELECT 1 from dual");
        testConfig.setIdleTimeout(3000);
        ds = new BeeDataSource(testConfig);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        testConfig.setInitialSize(10);
        testConfig.setMaxActive(50);

        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds,"pool");
        BeeDataSourceConfig tempConfig = (BeeDataSourceConfig)TestUtil.getFieldValue(pool,"poolConfig");
        if (tempConfig.getInitialSize() != initSize) TestUtil.assertError("initSize has changed,expected:" + initSize);
        if (tempConfig.getMaxActive() != maxSize) TestUtil.assertError("maxActive has changed,expected" + maxSize);
    }
}
