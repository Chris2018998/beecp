/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0032DataSourceWrapperTest {

    @Test
    public void testOnWrapper() {
        BeeDataSource ds = new BeeDataSource();
        Assertions.assertFalse(ds.isWrapperFor(null));
        Assertions.assertTrue(ds.isWrapperFor(DataSource.class));
        Assertions.assertTrue(ds.isWrapperFor(BeeDataSource.class));
        Assertions.assertFalse(ds.isWrapperFor(Connection.class));

        try {
            ds.unwrap(null);
            fail("testOnWrapper");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }

        try {
            ds.unwrap(Connection.class);
            fail("testOnWrapper");
        } catch (SQLException e) {
            Assertions.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }

        try {//correct
            ds.unwrap(BeeDataSource.class);
        } catch (SQLException e) {
            fail("testOnWrapper");
            Assertions.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }
    }
}
