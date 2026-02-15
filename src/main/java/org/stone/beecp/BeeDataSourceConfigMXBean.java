/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

/**
 * Jmx bean interface of data source configuration.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeDataSourceConfigMXBean {

    String getUsername();

    String getJdbcUrl();

    String getDriverClassName();

    String getConnectionFactoryClassName();

    String getPoolName();

    boolean isFairMode();

    int getInitialSize();

    int getMaxActive();

    int getSemaphoreSize();

    Boolean isDefaultAutoCommit();

    Integer getDefaultTransactionIsolation();

    String getDefaultTransactionIsolationName();

    String getDefaultCatalog();

    Boolean isDefaultReadOnly();

    long getMaxWait();

    long getIdleTimeout();

    long getHoldTimeout();

    String getAliveTestSql();

    int getAliveTestTimeout();

    long getAliveAssumeTime();

    boolean isForceRecycleBorrowedOnClose();

    long getParkTimeForRetry();

    long getIntervalOfClearTimeout();

    String getPoolImplementClassName();

    boolean isRegisterMbeans();

    boolean isRegisterJvmHook();
}
