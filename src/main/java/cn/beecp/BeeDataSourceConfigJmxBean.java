/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp;

/**
 * Bee DataSourceConfig JMX Bean interface
 *
 * @author Chris.Liao
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

    boolean isDefaultAutoCommit();

    String getDefaultTransactionIsolationName();

    int getDefaultTransactionIsolationCode();

    String getDefaultCatalog();

    boolean isDefaultReadOnly();

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
