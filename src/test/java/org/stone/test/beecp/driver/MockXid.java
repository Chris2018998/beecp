/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.driver;

import javax.transaction.xa.Xid;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class MockXid implements Xid {
    private final int formatId;
    private final byte[] globalTransactionId;
    private final byte[] branchQualifier;

    public MockXid(byte[] globalTransactionId, byte[] branchQualifier, int formatId) {
        this.branchQualifier = branchQualifier;
        this.globalTransactionId = globalTransactionId;
        this.formatId = formatId;
    }

    public int getFormatId() {
        return formatId;
    }

    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    public byte[] getBranchQualifier() {
        return branchQualifier;
    }
}
