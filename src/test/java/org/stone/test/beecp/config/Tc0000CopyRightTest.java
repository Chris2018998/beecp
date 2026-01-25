
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Test;
import org.stone.test.InitTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * print copy right info
 *
 * @author Chris Liao
 */

public class Tc0000CopyRightTest {

    @Test
    public void testOnPrintRightInfo() {
        String buf = "*********************************************************************************\n" +
                "*                                                                               *\n" +
                "*                            BeeCP Test                                         *\n" +
                "*                                                                               *\n" +
                "*                                                     Author:Chris2018998       *\n" +
                "*                                                     All rights reserved       *\n" +
                "********************************************************************************\n";

        try {
            InitTest.switchToSystemOut();
            System.out.print(buf);
            assertTrue(true);
        } finally {
            InitTest.switchToTestStreamOut();
        }
    }
}
