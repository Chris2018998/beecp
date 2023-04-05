/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp;

/**
 * Bee DataSourceConfig JMX Bean interface
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

    String getValidTestSql();

    int getValidTestTimeout();

    long getValidAssumeTime();

    boolean isForceCloseUsingOnClear();

    long getDelayTimeForNextClear();

    long getTimerCheckInterval();

    String getPoolImplementClassName();

    boolean isEnableJmx();
}
