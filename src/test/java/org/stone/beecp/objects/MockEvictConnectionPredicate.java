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

import org.stone.beecp.BeeConnectionPredicate;

import java.sql.SQLException;
import java.util.Objects;

/**
 * @author Chris Liao
 */
public class MockEvictConnectionPredicate implements BeeConnectionPredicate {

    private final int errorCode;
    private final String errorState;

    public MockEvictConnectionPredicate() {
        this(0, null);
    }

    public MockEvictConnectionPredicate(int errorCode, String errorState) {
        this.errorCode = errorCode;
        this.errorState = errorState;
    }

    public String evictTest(SQLException e) {
        return errorCode == e.getErrorCode() && Objects.equals(errorState, e.getSQLState()) ? "dead" : null;
    }
}