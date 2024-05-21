package org.stone.beecp.config;

import junit.framework.TestCase;
import org.apache.maven.surefire.booter.ForkedBooter;

public class Tc0000CopyRightTest extends TestCase {

    public void testOnPrintRightInfo() {
        String buf = "********************************************************************************\n" +
                "*                                                                               *\n" +
                "*                             Test case(beecp)                                  *\n" +
                "*                                                                               *\n" +
                "*                                                     Author:Chris2018998       *\n" +
                "*                                                     All rights reserved       *\n" +
                "********************************************************************************\n";
        System.out.print(buf);
    }
}
