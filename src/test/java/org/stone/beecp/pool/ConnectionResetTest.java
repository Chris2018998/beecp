/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.TransactionIsolation;

import java.sql.Connection;

public class ConnectionResetTest extends TestCase {
    String catlog = "mysql";
    String schema = "mysql";
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setDefaultAutoCommit(false);
        config.setDefaultTransactionIsolationName(TransactionIsolation.LEVEL_READ_COMMITTED);
        config.setDefaultReadOnly(true);
        config.setDefaultCatalog(catlog);
        config.setDefaultSchema(schema);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            con.setAutoCommit(true);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setReadOnly(false);
            con.setCatalog("test");
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
            if (pool.getTotalSize() != 1) TestUtil.assertError("Total connections not as expected:" + 1);

            con = ds.getConnection();
            //if(con.getAutoCommit()!=false)TestUtil.assertError("autoCommit reset fail");
            if (con.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED)
                TestUtil.assertError("TransactionIsolation reset fail");
            if (!con.isReadOnly()) TestUtil.assertError("readonly reset fail");

            TestUtil.assertError("catalog reset fail,expect:s%,current is s%", catlog, con.getCatalog());
        } finally {
            if (con != null)
                TestUtil.oclose(con);
        }
    }
}
