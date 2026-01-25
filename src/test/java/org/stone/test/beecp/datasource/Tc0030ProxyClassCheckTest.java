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
import org.stone.beecp.exception.BeeDataSourceCreatedException;
import org.stone.beecp.exception.BeeDataSourcePoolStartedFailureException;
import org.stone.test.base.TestUtil;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0030ProxyClassCheckTest {

    @Test
    public void testJdbcProxyClassMissedCheck() throws Exception {
        String proxyClassName = "org/stone/beecp/pool/ProxyConnection.class";
        File proxyClassFile = TestUtil.getClassPathFileAbsolutePath(proxyClassName);
        assert proxyClassFile != null;

        String proxyClassFileFullName = proxyClassFile.toString();
        int classFileFolderPos = proxyClassFileFullName.lastIndexOf(File.separator);
        String classFolderName = proxyClassFileFullName.substring(0, classFileFolderPos);
        File proxyClassFile2 = new File(classFolderName + File.separator + "ProxyConnection2.class");

        try {
            assertTrue(proxyClassFile.renameTo(proxyClassFile2));
            try (BeeDataSource ignored = new BeeDataSource(createDefault())) {
                Assertions.fail("[testJdbcProxyClassMissedCheck]Test failed");
            }
        } catch (BeeDataSourceCreatedException e) {
            assertInstanceOf(BeeDataSourcePoolStartedFailureException.class, e.getCause());
            BeeDataSourcePoolStartedFailureException failedException = (BeeDataSourcePoolStartedFailureException) e.getCause();
            assertInstanceOf(ClassNotFoundException.class, failedException.getCause());
        } finally {
            assertTrue(proxyClassFile2.renameTo(proxyClassFile));
        }
    }
}
