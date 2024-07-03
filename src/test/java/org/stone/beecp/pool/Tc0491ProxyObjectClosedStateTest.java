package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.stone.beecp.config.DsConfigFactory.*;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0491ProxyObjectClosedStateTest extends TestCase {
    private BeeDataSource ds;

    private static void statementProxy(Connection con) throws Exception {
        Statement st = null;
        try {
            st = con.createStatement();
            st.close();
            Assert.assertTrue(st.isClosed());
        } finally {
            oclose(st);
        }
    }

    private static void preparedStatementProxy(Connection con) throws Exception {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("select 1 from dual");
            ps.close();
            Assert.assertTrue(ps.isClosed());
        } finally {
            oclose(ps);
        }
    }

    private static void callableStatementProxy(Connection con) throws Exception {
        CallableStatement cs = null;
        try {
            cs = con.prepareCall("?={call test(}");
            cs.close();
            Assert.assertTrue(cs.isClosed());
        } finally {
            oclose(cs);
        }
    }

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName(JDBC_DRIVER);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);

        config.setInitialSize(5);
        config.setAliveTestSql("SELECT 1 from dual");
        config.setIdleTimeout(3000);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testProxyObject() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            statementProxy(con);
            preparedStatementProxy(con);
            callableStatementProxy(con);

            con.close();
            Assert.assertTrue(con.isClosed());
        } finally {
            if (con != null) oclose(con);
        }
    }
}
