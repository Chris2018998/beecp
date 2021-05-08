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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test case
 *
 * @author chris.liao
 */
public class TestCase {
    public void setUp() throws Throwable {
    }

    public void tearDown() throws Throwable {
    }

    void run() throws Throwable {
        try {
            setUp();
            runTest();
        } finally {
            try {
                tearDown();
            } catch (Throwable e) {
            }
        }
    }

    private void runTest() throws Throwable {
        int successCount = 0, failedCount = 0;
        long beginTime = System.currentTimeMillis();
        Method[] methods = this.getClass().getMethods();
        System.out.println("Case[" + this.getClass().getName() + "]begin");
        Object[]emptyParam= new Object[0];

        for (Method method : methods) {
            if (method.getName().startsWith("test") && method.getParameterTypes().length == 0) {
                try {
                    method.invoke(this, emptyParam);
                    successCount++;
                } catch (Throwable e) {
                    failedCount++;
                    System.out.println("Failed to run test method:" + method.getName() + " in Class[" + this.getClass().getName() + "]");
                    if (e instanceof InvocationTargetException) {
                        ((InvocationTargetException) e).getTargetException().printStackTrace();
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("took time:" + (endTime - beginTime) + "ms,success(" + successCount + "),failed(" + failedCount + ")");
        if (failedCount > 0) throw new Exception("Failed in Case[" + this.getClass().getName() + "]");
    }
}
