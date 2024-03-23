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
 * JMX Bean interface on {@link BeeDataSourceConfig}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeDataSourceConfigJmxBean {

    String getUsername();

    String getUrl();

    String getDriverClassName();

    String getConnectionFactoryClassName();

    String getPoolName();

    boolean isFairMode();

    int getInitialSize();

    int getMaxActive();

    int getBorrowSemaphoreSize();

    Boolean isDefaultAutoCommit();

    Integer getDefaultTransactionIsolationCode();

    String getDefaultTransactionIsolationName();

    String getDefaultCatalog();

    Boolean isDefaultReadOnly();

    long getMaxWait();

    long getIdleTimeout();

    long getHoldTimeout();

    String getAliveTestSql();

    int getAliveTestTimeout();

    long getValidAssumeTime();

    boolean isForceCloseUsingOnClear();

    long getDelayTimeForNextClear();

    long getTimerCheckInterval();

    String getPoolImplementClassName();

    boolean isEnableJmx();
}
