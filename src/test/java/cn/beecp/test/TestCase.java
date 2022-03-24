/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
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
        //do nothing
    }

    public void tearDown() throws Throwable {
        //do nothing
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

    private void runTest() throws Exception {
        int successCount = 0;
        int failedCount = 0;
        long beginTime = System.currentTimeMillis();
        Method[] methods = this.getClass().getMethods();
        System.out.println("Case[" + this.getClass().getName() + "]begin");
        Object[] emptyParam = new Object[0];

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
        if (failedCount > 0) throw new TestException("Failed in Case[" + this.getClass().getName() + "]");
    }
}
