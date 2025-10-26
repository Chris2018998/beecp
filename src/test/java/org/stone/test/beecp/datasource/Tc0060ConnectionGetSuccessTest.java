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
import org.stone.test.beecp.driver.MockDataSource;
import org.stone.test.beecp.driver.MockXaDataSource;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.factory.MockXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.Connection;

/**
 * @author Chris Liao
 */
public class Tc0060ConnectionGetSuccessTest {

    @Test
    public void testConnectionFactory() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockConnectionFactory.class.getName());
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            try (Connection con = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon = ds.getXAConnection();
            try (Connection con = xaCon.getConnection()) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon2 = ds.getXAConnection("root", "root");
            try (Connection con = xaCon2.getConnection()) {
                Assertions.assertNotNull(con);
            }
        }
    }

    @Test
    public void testXAConnectionFactory() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockXaConnectionFactory.class.getName());
            XAConnection xaCon = ds.getXAConnection();
            try (Connection con = xaCon.getConnection()) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon2 = ds.getXAConnection("root", "root");
            try (Connection con = xaCon2.getConnection()) {
                Assertions.assertNotNull(con);
            }

            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            try (Connection con = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(con);
            }
        }
    }

    @Test
    public void testDriverDataSource() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockDataSource.class.getName());
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            try (Connection con = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon = ds.getXAConnection();
            try (Connection con = xaCon.getConnection()) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon2 = ds.getXAConnection("root", "root");
            try (Connection con = xaCon2.getConnection()) {
                Assertions.assertNotNull(con);
            }
        }
    }

    @Test
    public void testDriverXaDataSource() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(MockXaDataSource.class.getName());

            XAConnection xaCon = ds.getXAConnection();
            try (Connection con = xaCon.getConnection()) {
                Assertions.assertNotNull(con);
            }

            XAConnection xaCon2 = ds.getXAConnection("root", "root");
            try (Connection con = xaCon2.getConnection()) {
                Assertions.assertNotNull(con);
            }

            ds.setConnectionFactoryClassName(MockXaDataSource.class.getName());
            try (Connection con = ds.getConnection()) {
                Assertions.assertNotNull(con);
            }
            try (Connection con = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(con);
            }
        }
    }
}