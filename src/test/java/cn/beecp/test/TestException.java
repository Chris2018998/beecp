/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test;

/**
 * Test Exception
 *
 * @author chris.liao
 */
public class TestException extends Exception {

    public TestException(String s) {
        super(s);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }

}
