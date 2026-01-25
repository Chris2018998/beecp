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
import org.stone.beecp.BeeConnectionPool;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.exception.BeeDataSourceCreatedException;
import org.stone.beecp.exception.BeeDataSourcePoolInstantiatedException;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.pool.PoolImpl_NoDefaultConstructor;
import org.stone.tools.exception.BeanException;

/**
 * @author Chris Liao
 */
public class Tc0032DsPoolCreateFailTest {

    @Test
    public void testFailException() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MockConnectionFactory());

        //no default constructor
        String poolClassName = PoolImpl_NoDefaultConstructor.class.getName();
        config.setPoolImplementClassName(poolClassName);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testFailException]test failed");
        } catch (BeeDataSourceCreatedException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolInstantiatedException.class, e.getCause());
            BeeDataSourcePoolInstantiatedException poolCreateFailedException = (BeeDataSourcePoolInstantiatedException) e.getCause();
            Assertions.assertInstanceOf(BeanException.class, poolCreateFailedException.getCause());
            BeanException exception = (BeanException) poolCreateFailedException.getCause();
            Assertions.assertEquals("Failed to create instance on class[" + poolClassName + "]", exception.getMessage());
        }

        //error type test
        String poolClassName2 = "java.lang.String";//error type
        config.setPoolImplementClassName(poolClassName2);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testFailException]test failed");
        } catch (BeeDataSourceCreatedException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolInstantiatedException.class, e.getCause());
            BeeDataSourcePoolInstantiatedException poolCreateFailedException = (BeeDataSourcePoolInstantiatedException) e.getCause();
            Assertions.assertInstanceOf(BeanException.class, poolCreateFailedException.getCause());
            BeanException exception = (BeanException) poolCreateFailedException.getCause();
            String errorMsg = "Can‘t create instance on class[" + poolClassName2 + "]which must extend from one of type[" + BeeConnectionPool.class.getName() + "]at least,creation category[pool]";
            Assertions.assertEquals(errorMsg, exception.getMessage());
        }

        //class not found
        String poolClassName3 = poolClassName + "_ClassNotFound";
        config.setPoolImplementClassName(poolClassName3);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testFailException]test failed");
        } catch (BeeDataSourceCreatedException e) {
            Assertions.assertInstanceOf(BeeDataSourcePoolInstantiatedException.class, e.getCause());
            BeeDataSourcePoolInstantiatedException poolCreateFailedException = (BeeDataSourcePoolInstantiatedException) e.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCreateFailedException.getCause());
            String errorMsg = "Failed to create a pool with class:" + poolClassName3;
            Assertions.assertEquals(errorMsg, poolCreateFailedException.getMessage());
        }
    }
}
