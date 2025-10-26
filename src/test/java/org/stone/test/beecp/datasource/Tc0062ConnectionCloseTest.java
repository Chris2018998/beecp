/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.driver.MockXaConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.factory.MockXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.*;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0062ConnectionCloseTest {

    @Test
    public void testOperationOnClosed() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();
            DatabaseMetaData dbMeta = con.getMetaData();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select * from user");
            ResultSet catalogs = dbMeta.getCatalogs();

            Assertions.assertEquals(con, dbMeta.getConnection());
            Assertions.assertEquals(con, st.getConnection());
            Assertions.assertEquals(st, rs.getStatement());
            Assertions.assertNull(catalogs.getStatement());

            con.close();
            Assertions.assertTrue(con.isClosed());
            Assertions.assertTrue(st.isClosed());
            Assertions.assertTrue(rs.isClosed());
            Assertions.assertEquals("Connection has been closed", con.toString());
            Assertions.assertEquals("Statement has been closed", st.toString());
            Assertions.assertEquals("ResultSet has been closed", rs.toString());

            try {
                con.createStatement();
                Assertions.fail("[testOperationOnClosed]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("No operations allowed on closed connection", e.getMessage());
            }

            try {
                st.execute("select * from user");
                Assertions.fail("[testOperationOnClosed]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("No operations allowed on closed statement", e.getMessage());
            }

            try {
                rs.getMetaData();
                Assertions.fail("[testOperationOnClosed]Test failed");
            } catch (SQLException e) {
                Assertions.assertEquals("No operations allowed on closed resultSet", e.getMessage());
            }
        }
    }

    @Test
    public void testExceptionOnClose() throws SQLException {
        MockConnectionProperties conProperties = new MockConnectionProperties();
        conProperties.throwsExceptionWhenCallMethod("close");
        conProperties.setMockException1(new SQLException("unknown error during close"));
        MockConnectionFactory factory = new MockConnectionFactory(conProperties);
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(factory);
        config.setForceRecycleBorrowedOnClose(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select * from user");

            LogCollector logCollector = LogCollector.startLogCollector();
            oclose(rs);
            oclose(st);
            oclose(con);
            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("Warning:Error at closing resultSet"));
            Assertions.assertTrue(logs.contains("Warning:Error at closing statement"));
        }

        //close xa connection
        MockXaConnectionProperties xaConnectionProperties = new MockXaConnectionProperties();
        xaConnectionProperties.throwsExceptionWhenCallMethod("close");
        xaConnectionProperties.setMockException1(new SQLException("unknown error during close"));
        MockXaConnectionFactory XaConnectionFactory = new MockXaConnectionFactory(xaConnectionProperties);
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setXaConnectionFactory(XaConnectionFactory);
        config2.setForceRecycleBorrowedOnClose(true);
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            XAConnection xaCon = ds.getXAConnection();
            oclose(xaCon);
        }
    }
}
