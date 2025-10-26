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

import java.sql.*;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0068ConnectionWrapperTest {
    @Test
    public void testConnectionWrapper() throws Exception {
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                DatabaseMetaData dbs = con.getMetaData();
                Statement st = con.createStatement();
                ResultSet rs1 = st.executeQuery("select * from user");
                ResultSetMetaData rs1Meta = rs1.getMetaData();

                Assertions.assertTrue(rs1Meta.isWrapperFor(ResultSetMetaData.class));
                Assertions.assertEquals(rs1Meta, rs1Meta.unwrap(ResultSetMetaData.class));

                Assertions.assertTrue(rs1.isWrapperFor(ResultSet.class));
                Assertions.assertEquals(rs1, rs1.unwrap(ResultSet.class));

                Assertions.assertTrue(st.isWrapperFor(Statement.class));
                Assertions.assertEquals(st, st.unwrap(Statement.class));

                Assertions.assertTrue(dbs.isWrapperFor(DatabaseMetaData.class));
                Assertions.assertEquals(dbs, dbs.unwrap(DatabaseMetaData.class));

                Assertions.assertTrue(con.isWrapperFor(Connection.class));
                Assertions.assertEquals(con, con.unwrap(Connection.class));
            }
        }
    }
}
