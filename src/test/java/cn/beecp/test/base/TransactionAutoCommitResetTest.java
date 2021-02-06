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
package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class TransactionAutoCommitResetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setDefaultAutoCommit(false);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con1 = null;
        PreparedStatement ps1 = null;
        ResultSet re1 = null;
        try {
            con1 = ds.getConnection();
            String userId = String.valueOf(new Random(Long.MAX_VALUE).nextLong());
            ps1 = con1
                    .prepareStatement("select count(*) from " + Config.TEST_TABLE + " where TEST_ID='" + userId + "'");
            re1 = ps1.executeQuery();
            try {
                con1.setAutoCommit(true);
                TestUtil.assertError("AutoCommit reset false before rollback or commit");
            } catch (SQLException e) {
            }
        } finally {
            if (re1 != null)
                TestUtil.oclose(re1);
            if (ps1 != null)
                TestUtil.oclose(ps1);
            if (con1 != null)
                TestUtil.oclose(con1);
        }
    }
}