/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.pool;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.JdbcConfig;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;

import java.sql.Connection;

public class DataSourceConnectionFactoryTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.addConnectProperty("url", JdbcConfig.JDBC_URL);
        //config.setConnectionFactoryClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.setConnectionFactoryClassName("cn.beecp.test.mock.MockDataSource");

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
