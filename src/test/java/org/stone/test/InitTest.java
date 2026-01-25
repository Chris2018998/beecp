/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.stone.tools.CommonUtil;

import java.io.*;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.stone.test.base.TestUtil.getClassVersion;

/**
 * First Test Case
 *
 * @author chris liao
 */
public class InitTest {
    public static PrintStream systemOut;
    public static PrintStream systemErr;
    public static PrintStream systemTestOut;
    public static PrintStream systemTestErr;

    public static void switchToSystemOut() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    public static void switchToTestStreamOut() {
        System.setOut(systemTestOut);
        System.setErr(systemTestErr);
    }

    @Test
    public void testPreparation() throws Exception {
        String init_file = "/InitTest.properties";
        try (InputStream fileStream = this.getClass().getResourceAsStream(init_file)) {
            if (fileStream == null) throw new IOException("Can't find file:'" + init_file + "' in classpath");
            Properties prop = new Properties();
            prop.load(fileStream);
            String targetVersion = prop.getProperty("classes.major");

            if (CommonUtil.isNotBlank(targetVersion)) {
                Integer version = Integer.valueOf(targetVersion);
                Class<?> currentClass = this.getClass();
                String currentClassName = currentClass.getName();
                String currentClassPathName = currentClassName.replaceAll("\\.", "/") + ".class";
                URL classFileUrl = currentClass.getClassLoader().getResource(currentClassPathName);

                assert classFileUrl != null;
                File classFile = new File(classFileUrl.getFile());
                int[] fileJavaVersion = getClassVersion(classFile);
                try {
                    Assertions.assertEquals(version, fileJavaVersion[0]);
                } catch (AssertionError e) {
                    LoggerFactory.getLogger(this.getClass()).error("Compiled class major version error,expect:{},actual:{}", version, fileJavaVersion[0]);
                    System.exit(-1);
                }
            }
        }

        systemOut = System.out;
        systemErr = System.err;

        systemTestOut = new PrintStream(new ByteArrayOutputStream());
        systemTestErr = new PrintStream(new ByteArrayOutputStream());
        assertTrue(true);
    }
}