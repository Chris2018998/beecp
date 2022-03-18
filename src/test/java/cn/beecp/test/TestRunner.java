/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TestRunner {
    private static final String defaultFilename = "beecp/testCase.properties";

    @SuppressWarnings("rawtypes")
    private static Class[] getTestCaseClasses() throws Exception {
        return getTestCaseClasses(defaultFilename);
    }

    public static void main(String[] ags) throws Throwable {
        TestRunner.run(getTestCaseClasses());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Class[] getTestCaseClasses(String caseFile) throws Exception {
        List classList = new ArrayList(10);
        InputStream propertiesStream = null;

        try {
            SortedProperties properties = new SortedProperties();
            propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
            propertiesStream = TestRunner.class.getClassLoader().getResourceAsStream(defaultFilename);
            if (propertiesStream == null) propertiesStream = TestRunner.class.getResourceAsStream(defaultFilename);
            if (propertiesStream == null) throw new IOException("Can't find file:'testCase.properties' in classpath");

            String pass1 = "true", pass2 = "Y";
            properties.load(propertiesStream);

            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = properties.getProperty(key);
                if (pass1.equalsIgnoreCase(value) || pass2.equalsIgnoreCase(value)) {
                    Class clazz = Class.forName(key);
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

    @SuppressWarnings("rawtypes")
    public static void run(Class testClass) throws Throwable {
        if (testClass != null) {
            ((TestCase) testClass.newInstance()).run();
        }
    }

    @SuppressWarnings("rawtypes")
    public static void run(Class[] testClass) throws Throwable {
        if (testClass != null) {
            for (int i = 0; i < testClass.length; i++)
                run(testClass[i]);
        }
    }

    public void testRun() throws Throwable {
        long begtinTime = System.currentTimeMillis();
        TestRunner.run(getTestCaseClasses());
        System.out.println("Took time:(" + (System.currentTimeMillis() - begtinTime) + ")ms");
    }
}