/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.driver;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * A properties holder in mock connection
 */
public final class MockConnectionProperties {
    private final HashMap<String, Boolean> methodExceptionFlagMap;

    //connection properties
    private boolean autoCommit = true;
    private boolean readOnly = false;
    private boolean valid = true;
    private int transactionIsolation;
    private String catalog;
    private String schema;
    private int networkTimeout;
    private int holdability;
    //for eviction test
    private String error;
    private int errorCode;
    private String errorState;
    private SQLException mockException1;
    private RuntimeException mockException2;
    private Error mockException3;

    public MockConnectionProperties() {
        this.methodExceptionFlagMap = new HashMap<>();
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getNetworkTimeout() {
        return networkTimeout;
    }

    public void setNetworkTimeout(int networkTimeout) {
        this.networkTimeout = networkTimeout;
    }

    public SQLException getMockException1() {
        return mockException1;
    }

    public void setMockException1(SQLException mockException1) {
        this.mockException1 = mockException1;
    }

    public RuntimeException getMockException2() {
        return mockException2;
    }

    public void setMockException2(RuntimeException mockException2) {
        this.mockException2 = mockException2;
    }

    public Error getMockException3() {
        return mockException3;
    }

    public void setMockException3(Error mockException3) {
        this.mockException3 = mockException3;
    }

    public int getHoldability() {
        return holdability;
    }

    public void setHoldability(int holdability) {
        this.holdability = holdability;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorState() {
        return errorState;
    }

    public void setErrorState(String errorState) {
        this.errorState = errorState;
    }

    public void enableExceptionOnMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodExceptionFlagMap.put(methodName, Boolean.TRUE);
            }
        }
    }

    public void disableExceptionOnMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodExceptionFlagMap.put(methodName, Boolean.FALSE);
            }
        }
    }

    public void mockThrowExceptionOnMethod(String methodName) throws SQLException {
        if (methodExceptionFlagMap.containsKey(methodName) && methodExceptionFlagMap.get(methodName)) {
            if (mockException1 != null) throw mockException1;
            if (mockException2 != null) throw mockException2;
            if (mockException3 != null) throw mockException3;

            throw new SQLException(error, this.errorState, this.errorCode);
        }
    }
}
