/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

/**
 * @author Chris Liao
 */
public class BeeCPHello implements BeeCPHelloMBean {
    public String getName() {
        return "hello world";
    }
}
