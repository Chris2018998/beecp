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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProxyResultSetGetTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setInitialSize(0);
        config.setConnectionTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con = null;
        PreparedStatement ps=null;
        ResultSet rs=null;
        ResultSet rs2=null;
        try {
            con=ds.getConnection();
            ps=con.prepareStatement("select * from BEECP_TEST");
            rs=ps.executeQuery();
            rs2=ps.getResultSet();
            if(rs2!=rs) TestUtil.assertError("ps.getResultSet() != ps.executeQuery()");
            if(ps.getResultSet()!=rs2) TestUtil.assertError("ps.getResultSet() != ps.executeQuery()");
        } finally {
            if (rs != null) TestUtil.oclose(rs);
            if (ps != null) TestUtil.oclose(ps);
            if (con != null) TestUtil.oclose(con);
        }
    }
}
