/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * print rights info
 *
 * @author Chris Liao
 */

public class Tc0000CopyRightTest extends TestCase {

    public void testOnPrintRightInfo() {
        String buf = "*********************************************************************************\n" +
                "*                                                                               *\n" +
                "*                            BeeCP Test                                         *\n" +
                "*                                                                               *\n" +
                "*                                                     Author:Chris2018998       *\n" +
                "*                                                     All rights reserved       *\n" +
                "********************************************************************************\n";
        System.out.print(buf);
        Assert.assertTrue(true);
    }
}
