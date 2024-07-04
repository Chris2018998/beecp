/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0500DataSourceConnectionFactoryTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
        config.addConnectProperty("url", JDBC_URL);
        config.setConnectionFactoryClassName("org.stone.beecp.driver.MockDataSource");

        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testDataSourceConnectionFactory() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            Assert.assertNotNull(con);
        } finally {
            if (con != null) oclose(con);
        }
    }
}
