/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TestRunner {
    private static String defaultFilename = "testCase.properties";

    @SuppressWarnings("rawtypes")
    private static Class[] getTestCaseClasses() throws Exception {
        return getTestCaseClasses(defaultFilename);
    }

    public static void main(String[] ags) throws Throwable {
        TestRunner.run(getTestCaseClasses());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Class[] getTestCaseClasses(String caseFile) throws Exception {
        List classList = new ArrayList();
        InputStream propertiesStream = null;

        try {
            SortKeyProperties properties = new SortKeyProperties();
            propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
            propertiesStream = TestRunner.class.getClassLoader().getResourceAsStream(defaultFilename);
            if (propertiesStream == null) propertiesStream = TestRunner.class.getResourceAsStream(defaultFilename);
            if (propertiesStream == null) throw new IOException("Can't find file:'testCase.properties' in classpath");

            String pass1 = "true", pass2 = "Y";
            properties.load(propertiesStream);

            Enumeration<Object> keys=properties.keys();
            while(keys.hasMoreElements()){
                String key=(String)keys.nextElement();
                String value=properties.getProperty(key);
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

@SuppressWarnings("serial")
class SortKeyProperties extends Properties {
    private Vector<Object> keyVector = new Vector<Object>();

    public synchronized Enumeration<Object> keys() {
        return keyVector.elements();
    }

    public synchronized Object put(Object key, Object value) {
        Object oldValue = super.put(key, value);
        if (!keyVector.contains(key))
            keyVector.add(key);
        return oldValue;
    }

    public synchronized Object remove(Object key) {
        Object value = super.remove(key);
        if (keyVector.contains(key))
            keyVector.remove(key);
        return value;
    }
}
