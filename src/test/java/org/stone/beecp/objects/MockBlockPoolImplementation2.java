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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class MockBlockPoolImplementation2 extends MockBlockPoolImplementation {

    public MockBlockPoolImplementation2() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
    }

}
