/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.stone.base.TestException;
import org.stone.beecp.BeeDataSource;

import java.io.PrintWriter;

public class DsNullCommonDsTest extends TestCase {

    public void testNullCommonDs() throws Exception {
        BeeDataSource ds = new BeeDataSource();
        PrintWriter testWriter = new PrintWriter(System.out);
        ds.setLogWriter(testWriter);

        if (ds.getLogWriter() != null) throw new TestException();
        if (ds.getParentLogger() != null) throw new TestException();
    }
}
