/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

import java.sql.Connection;

public class Tc0061DataSourceConnectionFactoryTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.addConnectProperty("url", JdbcConfig.JDBC_URL);
        //config.setConnectionFactoryClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockDataSource");

        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
