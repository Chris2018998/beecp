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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */

public class Tc0030DataSourceCreateTest {

    @Test
    public void testCreationWithDefaultConstructor() {//success test
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreationWithDefaultConstructor]threw exception when create datasource object by default constructor");
        }
    }

    @Test
    public void testCreationWithConfiguration() {//success test
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(createDefault());
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreationWithConfiguration]threw exception when create datasource with configuration object");
        } finally {
            if (ds != null) ds.close();
        }
    }

    @Test
    public void testCreationWithJdbcInfo() {//success test
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Assertions.assertNotNull(ds);
        } catch (Exception e) {
            fail("[testCreationWithJdbcInfo]threw exception when create datasource with jdbc info");
        } finally {
            if (ds != null) ds.close();
        }
    }


    @Test
    public void testInvalidPoolClass() {//fail test
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds = new BeeDataSource(config);
            fail("[testDataSourceCreateFailed]not threw exception when Data source created with an invalid pool class");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolCreateFailedException.class, cause);

            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        } finally {
            if (ds != null) ds.close();
        }
    }
}
