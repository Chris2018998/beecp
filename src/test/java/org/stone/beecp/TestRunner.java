/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp;

import junit.framework.TestCase;
import org.stone.base.StoneTestRunner;

public class TestRunner extends TestCase {
    private static final String defaultFilename = "beecp/testCase.properties";

    public static void main(String[] ags) throws Throwable {
        StoneTestRunner.main(new String[]{"beecp", defaultFilename});
    }

    public void testRun() throws Throwable {
        StoneTestRunner.main(new String[]{"beecp", defaultFilename});
    }
}