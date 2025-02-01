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

/**
 * @author Chris Liao
 */
public class BlockingState {

    //0:Normal,1:interrupt,2:continue to create
    private volatile int state = 0;

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
