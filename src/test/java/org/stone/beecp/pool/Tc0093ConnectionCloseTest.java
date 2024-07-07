/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;

import static org.stone.beecp.config.DsConfigFactory.TEST_PROCEDURE;
import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0093ConnectionCloseTest extends TestCase {

    public void testCloseResultSet() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create connection
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        ResultSet rs1 = st.executeQuery("select * from user");
        ResultSetMetaData rs1Meta = rs1.getMetaData();

        rs1.close();
        Assert.assertTrue(rs1.isClosed());
        try {
            rs1Meta.getColumnCount();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        con.close();
        pool.close();
    }

    public void testCloseStatement() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create connection
        Connection con = pool.getConnection();
        Statement st = con.createStatement();
        ResultSet rs1 = st.executeQuery("select * from user");
        ResultSetMetaData rs1Meta = rs1.getMetaData();
        st.close();

        Assert.assertTrue(rs1.isClosed());
        Assert.assertTrue(st.isClosed());

        try {
            rs1Meta.getColumnCount();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }

        try {
            st.getConnection();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after statement closed", e.getMessage());
        }
        con.close();
        pool.close();
    }

    public void testCloseConnection() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        //1: create connection
        Connection con = pool.getConnection();

        //2；create statement
        DatabaseMetaData dbs = con.getMetaData();
        Statement st = con.createStatement();
        PreparedStatement ps = con.prepareStatement("select * from dual");
        CallableStatement cs = con.prepareCall("?={call " + TEST_PROCEDURE + "}");

        //owner check
        Assert.assertEquals(con, dbs.getConnection());
        Assert.assertEquals(con, st.getConnection());
        Assert.assertEquals(con, ps.getConnection());
        Assert.assertEquals(con, cs.getConnection());

        //3: create ResultSet
        ResultSet rs1 = st.executeQuery("select * from user");
        Assert.assertEquals(rs1, st.getResultSet());
        ResultSet rs2 = ps.executeQuery();
        cs.executeQuery();
        ResultSet rs3 = cs.getResultSet();
        Assert.assertNotNull(rs1);
        Assert.assertNotNull(rs2);
        Assert.assertNotNull(rs3);

        //owner check
        Assert.assertEquals(st, rs1.getStatement());
        Assert.assertEquals(ps, rs2.getStatement());
        Assert.assertEquals(cs, rs3.getStatement());

        //4:create result MetaData
        ResultSetMetaData rs1Meta = rs1.getMetaData();
        ResultSetMetaData rs2Meta = rs2.getMetaData();
        ResultSetMetaData rs3Meta = rs3.getMetaData();

        con.close();

        Assert.assertTrue(con.isClosed());
        Assert.assertTrue(rs1.isClosed());
        Assert.assertTrue(rs2.isClosed());
        Assert.assertTrue(rs3.isClosed());
        Assert.assertTrue(st.isClosed());
        Assert.assertTrue(ps.isClosed());
        Assert.assertTrue(cs.isClosed());

        try {
            rs1Meta.getColumnCount();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs2Meta.getColumnCount();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs3Meta.getColumnCount();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }

        try {
            rs1.getStatement();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs2.getStatement();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs3.getStatement();
        } catch (SQLException e) {
            Assert.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }


        try {
            st.getConnection();
            fail("statement operation after connection close(st)");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        try {
            st.getConnection();
            fail("statement operation after connection close(st)");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        try {
            ps.getConnection();
            fail("preparedStatement operation after connection close(ps)");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }
        try {
            cs.getConnection();
            fail("callableStatement operation after connection close(cs)");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }
        try {
            dbs.getConnection();
            fail("DatabaseMetaData operation after connection close(dbs)");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }

        try {
            con.getMetaData();
            fail("DatabaseMetaData operation after connection close");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }

        pool.close();
    }
}
