/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.base;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.textui.ResultPrinter;

public class TestResultPrinter extends ResultPrinter {
    TestResultPrinter() {
        super(System.out);
    }

    public void startTest(Test test) {
        //do nothing
    }

    protected void printHeader(long runTime) {
        //do nothing
    }

    protected void printErrors(TestResult result) {
        //do nothing
    }

    protected void printFailures(TestResult result) {
        //do nothing
    }

    protected void printFooter(TestResult result) {
        //do nothing
    }
}
