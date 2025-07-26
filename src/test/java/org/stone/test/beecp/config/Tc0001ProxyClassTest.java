/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Test;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.test.base.TestUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.stone.test.base.TestUtil.invokeMethod2;

/**
 * Proxy objects test case
 *
 * @author Chris Liao
 */
public class Tc0001ProxyClassTest {

    @Test
    public void testCheckJdbcProxyClasses() throws Exception {
        String className1 = "org/stone/beecp/pool/Borrower.class";
        File classFile1 = TestUtil.getClassPathFileAbsolutePath(className1);

        assert classFile1 != null;
        String classFileName1 = classFile1.toString();
        int pos = classFileName1.lastIndexOf(File.separator);
        String folderName = classFileName1.substring(0, pos);
        File classFile2 = new File(folderName + File.separator + "Borrower2.class");

        try {
            assertTrue(classFile1.renameTo(classFile2));
            invokeMethod2(null, ConnectionPoolStatics.class, "checkJdbcProxyClass");
            fail("[testCheckJdbcProxyClasses]Not thrown exception when proxy classes missed");
        } catch (InvocationTargetException e) {
            assertInstanceOf(ClassNotFoundException.class, e.getCause());
        } finally {
            assertTrue(classFile2.renameTo(classFile1));
        }
    }
}
