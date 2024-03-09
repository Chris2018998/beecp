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

import java.sql.Connection;
import java.sql.Savepoint;

//test case for issue #2142 of HikariCP
public class SavePointRollbackTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setDefaultAutoCommit(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        ds = new BeeDataSource(config);
    }

    public void test() throws Exception {

        Connection conn1 = null;
        try {//
            conn1 = ds.getConnection();
            conn1.setAutoCommit(false);//
            conn1.createStatement().execute(
                    "INSERT INTO data (type, payload) VALUES ('a', '{}'::jsonb)");

            Savepoint point1 = conn1.setSavepoint();
            conn1.rollback(point1);

            /*
             * Key info
             * 1: rollback to the point1,it is no effect on the previous executing statement.
             * 2: terminate a traction, need call <method>commit</method> or <method>rollback</method>
             */
        } finally {
            if (conn1 != null) conn1.close();//I think that bee connection will reset to default and rollback
        }

        Connection conn2 = null;
        try {
            conn2 = ds.getConnection();
            if (!conn2.getAutoCommit())
                TestUtil.assertError("A dirty connection has in pool");
        } finally {
            if (conn2 != null) conn2.close();
        }
    }

    public void tearDown() throws Throwable {
        ds.close();
    }
}
