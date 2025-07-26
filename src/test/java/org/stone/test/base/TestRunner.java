/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.test.base;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.stone.tools.BeanUtil.loadClass;

public class TestRunner {

    public static void main(String[] ags) throws Throwable {
        if (ags != null && ags.length == 2) {
            printTestInfo(ags[0]);
            TestRunner.run(getTestCaseClasses(ags[1]));
        }
    }

    private static Class[] getTestCaseClasses(String caseFile) throws Exception {
        List classList = new ArrayList(10);
        InputStream propertiesStream = null;

        try {
            SortedProperties properties = new SortedProperties();
            propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
            propertiesStream = TestRunner.class.getClassLoader().getResourceAsStream(caseFile);
            if (propertiesStream == null) propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
            if (propertiesStream == null) throw new IOException("Can't find file:'testCase.properties' in classpath");

            String pass1 = "true";
            String pass2 = "Y";
            properties.load(propertiesStream);

            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = properties.getProperty(key);
                if (pass1.equalsIgnoreCase(value) || pass2.equalsIgnoreCase(value)) {
                    Class clazz = loadClass(key);
                    classList.add(clazz);
                }
            }
            return (Class[]) classList.toArray(new Class[0]);
        } finally {
            if (propertiesStream != null)
                try {
                    propertiesStream.close();
                } catch (IOException e) {
                }
        }
    }

    public static void run(Class testClass) throws Throwable {
        if (testClass != null) {
            ((TestCase) testClass.newInstance()).run();
        }
    }

    private static void printTestInfo(String moduleName) {
        String buf = "********************************************************************************\n" +
                "*                                                                              *\n" +
                "*                             Test case(" + moduleName + ")                                 *\n" +
                "*                                                                              *\n" +
                "*                                                     Author:Chris2018998      *\n" +
                "*                                                     All rights reserved      *\n" +
                "*******************************************************************************\n";
        System.out.print(buf);
    }

    public static void run(Class[] testClass) throws Throwable {
        if (testClass != null) {
            for (int i = 0; i < testClass.length; i++)
                run(testClass[i]);
        }
    }
}