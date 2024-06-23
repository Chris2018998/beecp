/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public class Tc0032DataSourceWrapperTest extends TestCase {

    public void testOnWrapper() {
        BeeDataSource ds = new BeeDataSource();
        Assert.assertFalse(ds.isWrapperFor(null));
        Assert.assertTrue(ds.isWrapperFor(DataSource.class));
        Assert.assertTrue(ds.isWrapperFor(BeeDataSource.class));
        Assert.assertFalse(ds.isWrapperFor(Connection.class));

        try {
            ds.unwrap(null);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }

        try {
            ds.unwrap(Connection.class);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }

        try {
            ds.unwrap(BeeDataSource.class);
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("The wrapper object was not an instance of"));
        }
    }
}
