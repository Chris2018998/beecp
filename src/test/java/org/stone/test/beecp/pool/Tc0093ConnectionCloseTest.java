/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.TEST_PROCEDURE;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0093ConnectionCloseTest {

    @Test
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
        Assertions.assertTrue(rs1.isClosed());
        try {
            rs1Meta.getColumnCount();
            fail("testCloseResultSet");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        con.close();
        pool.close();
    }

    @Test
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

        Assertions.assertTrue(rs1.isClosed());
        Assertions.assertTrue(st.isClosed());

        try {
            rs1Meta.getColumnCount();
            fail("testCloseStatement");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }

        try {
            st.getConnection();
            fail("testCloseStatement");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after statement closed", e.getMessage());
        }
        con.close();
        pool.close();
    }

    @Test
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
        Assertions.assertEquals(con, dbs.getConnection());
        Assertions.assertEquals(con, st.getConnection());
        Assertions.assertEquals(con, ps.getConnection());
        Assertions.assertEquals(con, cs.getConnection());

        //3: create ResultSet
        ResultSet rs1 = st.executeQuery("select * from user");
        Assertions.assertEquals(rs1, st.getResultSet());
        ResultSet rs2 = ps.executeQuery();
        cs.executeQuery();
        ResultSet rs3 = cs.getResultSet();
        Assertions.assertNotNull(rs1);
        Assertions.assertNotNull(rs2);
        Assertions.assertNotNull(rs3);

        //owner check
        Assertions.assertEquals(st, rs1.getStatement());
        Assertions.assertEquals(ps, rs2.getStatement());
        Assertions.assertEquals(cs, rs3.getStatement());

        //4:create result MetaData
        ResultSetMetaData rs1Meta = rs1.getMetaData();
        ResultSetMetaData rs2Meta = rs2.getMetaData();
        ResultSetMetaData rs3Meta = rs3.getMetaData();

        con.close();

        Assertions.assertTrue(con.isClosed());
        Assertions.assertTrue(rs1.isClosed());
        Assertions.assertTrue(rs2.isClosed());
        Assertions.assertTrue(rs3.isClosed());
        Assertions.assertTrue(st.isClosed());
        Assertions.assertTrue(ps.isClosed());
        Assertions.assertTrue(cs.isClosed());

        try {
            rs1Meta.getColumnCount();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs2Meta.getColumnCount();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs3Meta.getColumnCount();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }

        try {
            rs1.getStatement();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs2.getStatement();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }
        try {
            rs3.getStatement();
            fail("testCloseConnection");
        } catch (SQLException e) {
            Assertions.assertEquals("No operations allowed after resultSet closed", e.getMessage());
        }


        try {
            st.getConnection();
            fail("statement operation after connection close(st)");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        try {
            st.getConnection();
            fail("statement operation after connection close(st)");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }

        try {
            ps.getConnection();
            fail("preparedStatement operation after connection close(ps)");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }
        try {
            cs.getConnection();
            fail("callableStatement operation after connection close(cs)");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after statement closed"));
        }
        try {
            dbs.getConnection();
            fail("DatabaseMetaData operation after connection close(dbs)");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }

        try {
            con.getMetaData();
            fail("DatabaseMetaData operation after connection close");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }

        pool.close();
    }
}
