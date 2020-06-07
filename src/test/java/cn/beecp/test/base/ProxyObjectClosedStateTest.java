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
import cn.beecp.util.BeecpUtil;

import java.sql.*;

public class ProxyObjectClosedStateTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setInitialSize(5);
        config.setConnectionTestSQL("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        config.setIdleCheckTimeInitDelay(10);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con = null;
        Statement st = null;
        PreparedStatement ps = null;
        CallableStatement cs = null;
        try {
            con = ds.getConnection();
            st = con.createStatement();
            st.close();
            if (!st.isClosed())
                TestUtil.assertError("Statement is not closed");
            st = null;

            ps = con.prepareStatement("select 1 from dual");
            ps.close();
            if (!ps.isClosed())
                TestUtil.assertError("PreparedStatement is not closed");
            ps = null;

            cs = con.prepareCall("?={call test(}");
            cs.close();
            if (!cs.isClosed())
                TestUtil.assertError("CallableStatement is not closed");
            cs = null;

            con.close();
            if (!con.isClosed())
                TestUtil.assertError("Connection is not closed");
            con = null;
        } finally {
            if (st != null)
                BeecpUtil.oclose(st);
            if (cs != null)
                BeecpUtil.oclose(cs);
            if (ps != null)
                BeecpUtil.oclose(ps);
            if (con != null)
                BeecpUtil.oclose(con);
        }
    }
}
