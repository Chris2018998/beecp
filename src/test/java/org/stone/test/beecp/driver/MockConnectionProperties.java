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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.locks.LockSupport;

/**
 * A properties holder in mock connection
 *
 * @author Chris Liao
 */
public class MockConnectionProperties {
    private final HashMap<String, Boolean> methodDelayFlagMap;
    private final HashMap<String, Boolean> methodExceptionFlagMap;

    //connection properties
    private boolean autoCommit = true;
    private boolean readOnly = false;
    private boolean valid = true;
    private int transactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
    private String catalog;
    private String schema;
    private int networkTimeout;
    private int holdability;
    //for eviction test
    private String error;
    private int errorCode;
    private String errorState;

    //protected long parkNanos;
    private long parkNanos;
    private long sleepMillis;
    private SQLException mockException1;
    private RuntimeException mockException2;
    private Error mockException3;

    public MockConnectionProperties() {
        this.methodDelayFlagMap = new HashMap<>(32);
        this.methodExceptionFlagMap = new HashMap<>(32);
    }

    //****************************************************************************************************************//
    //                                     1: Exception Setting                                                       //
    //****************************************************************************************************************//
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

    public void setParkNanos(long parkNanos) {
        this.parkNanos = parkNanos;
    }

    public void setSleepMillis(long sleepMillis) {
        this.sleepMillis = sleepMillis;
    }

    public void throwsExceptionWhenCallMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodExceptionFlagMap.put(methodName, Boolean.TRUE);
            }
        }
    }

    public void clearExceptionableMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodExceptionFlagMap.remove(methodName);
            }
        }
    }

    public void parkWhenCallMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodDelayFlagMap.put(methodName, Boolean.TRUE);
            }
        }
    }

    public void clearParkableMethod(String names) {
        if (names != null) {
            for (String methodName : names.split(",")) {
                this.methodDelayFlagMap.remove(methodName);
            }
        }
    }

    public void interceptBeforeCall(String methodName) throws SQLException {
        if (methodExceptionFlagMap.containsKey(methodName)) {
            if (mockException1 != null) throw mockException1;
            if (mockException2 != null) throw mockException2;
            if (mockException3 != null) throw mockException3;
            throw new SQLException(error, this.errorState, this.errorCode);
        }

        if (methodDelayFlagMap.containsKey(methodName)) {
            if (this.sleepMillis > 0L) {
                try {
                    Thread.sleep(this.sleepMillis);
                } catch (InterruptedException e) {
                    throw new SQLException(e);
                }
            } else if (this.parkNanos > 0L) {
                LockSupport.parkNanos(parkNanos);
            } else {
                LockSupport.park();
            }
        }
    }

    //****************************************************************************************************************//
    //                                     2: Properties set/get                                                      //
    //****************************************************************************************************************//
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

}
