/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.base;

import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class StoneTestRunner {

    public static void main(String[] ags) throws Throwable {
        if (ags != null && ags.length == 2) {
            long beginTime = System.currentTimeMillis();
            StoneTestRunner.run(getTestCaseClasses(ags[1]));
            System.out.println("Took time:(" + (System.currentTimeMillis() - beginTime) + ")ms");
        }
    }

    private static String[] getTestCaseClasses(String caseFile) throws Exception {
        InputStream propertiesStream = null;

        try {
            SortedProperties properties = new SortedProperties();
            propertiesStream = StoneTestRunner.class.getResourceAsStream(caseFile);
            propertiesStream = StoneTestRunner.class.getClassLoader().getResourceAsStream(caseFile);
            if (propertiesStream == null) propertiesStream = StoneTestRunner.class.getResourceAsStream(caseFile);
            if (propertiesStream == null) throw new IOException("Can't find file:'testCase.properties' in classpath");

            String pass1 = "true";
            String pass2 = "Y";
            properties.load(propertiesStream);
            List<String> classNameList = new ArrayList<>(properties.size());

            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = properties.getProperty(key);
                if (pass1.equalsIgnoreCase(value) || pass2.equalsIgnoreCase(value))
                    classNameList.add(key);
            }
            return classNameList.toArray(new String[0]);
        } finally {
            if (propertiesStream != null)
                try {
                    propertiesStream.close();
                } catch (IOException e) {
                }
        }
    }

    private static void run(String[] classNames) throws Throwable {
        if (classNames != null) {
            TestRunner runner = new TestRunner(new TestResultPrinter());
            for (String className : classNames) {
                System.out.println("Running:" + className);
                long startTime = System.currentTimeMillis();
                Class<?> caseClass = Class.forName(className);

                TestResult result = runner.doRun(new TestSuite(caseClass));
                long endTime = System.currentTimeMillis();
                System.out.println("...Tests run:" + result.runCount() + ", Failures:" + result.failureCount() + ", Errors:" + result.errorCount() + ",Time elapsed:" + (endTime - startTime) + " millis");
                Enumeration<TestFailure> failureEnum = result.failures();
                if (failureEnum != null) {
                    while (failureEnum.hasMoreElements()) {
                        TestFailure failure = failureEnum.nextElement();
                        System.err.println(failure.thrownException());
                    }
                }
                Enumeration<TestFailure> errorEnum = result.errors();
                if (errorEnum != null) {
                    while (errorEnum.hasMoreElements()) {
                        TestFailure error = errorEnum.nextElement();
                        System.err.println(error.thrownException());
                    }
                }
            }
        }
    }
}