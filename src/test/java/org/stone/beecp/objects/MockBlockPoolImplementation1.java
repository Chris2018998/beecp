/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class MockBlockPoolImplementation1 extends MockBlockPoolImplementation {

    public MockBlockPoolImplementation1() {
        LockSupport.park();
    }

}