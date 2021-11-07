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
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

public class PoolInitializeFailedTest extends TestCase {
    private int initSize = 5;

    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    public void testPoolInit() throws InterruptedException, Exception {
        try {
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setJdbcUrl("jdbc:beecp://localhost/testdb2");//give valid URL
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.setUsername(JdbcConfig.JDBC_USER);
            config.setPassword(JdbcConfig.JDBC_PASSWORD);
            config.setInitialSize(initSize);
            new BeeDataSource(config);
            TestUtil.assertError("A initializerError need be thrown,but not");
        } catch (RuntimeException e) {
            System.out.println(e.getCause());
        }
    }
}
