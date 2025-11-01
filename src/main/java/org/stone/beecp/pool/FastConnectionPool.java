/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.*;
import org.stone.beecp.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;
import org.stone.tools.extension.InterruptableSemaphore;
import org.stone.tools.logger.LogPrinter;
import org.stone.tools.logger.LogPrinterFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.BeeMethodExecutionLog.Type_Connection_Get;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public class FastConnectionPool extends Thread implements BeeConnectionPool, FastConnectionPoolMBean, PooledConnectionAliveTest, PooledConnectionTransferPolicy {
    static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantState");
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantTryCount");
    protected boolean usingMethodLogCache;
    protected MethodExecutionLogCache methodLogCache;
    LogPrinter logPrinter = LogPrinterFactory.CommonLogPrinter;

    String poolMode;
    String poolName;
    volatile int poolState;
    volatile int servantState;
    volatile int servantTryCount;
    BeeDataSourceConfig poolConfig;
    PooledConnection[] connectionArray;//fixed len
    ConcurrentLinkedQueue<Borrower> waitQueue;
    private boolean isFairMode;
    private boolean isCompeteMode;
    private int semaphoreSize;
    private InterruptableSemaphore semaphore;
    private long maxWaitMs;//milliseconds
    private long maxWaitNs;//nanoseconds

    private long aliveAssumeTimeMs;//milliseconds
    private int aliveTestTimeout;//seconds
    private long parkTimeForRetryNs;//nanoseconds
    private int stateCodeOnRelease;
    private int connectionArrayLen;
    private boolean connectionArrayInitialized;
    private InterruptableReentrantReadWriteLock connectionArrayInitLock;
    private PooledConnectionTransferPolicy transferPolicy;
    private boolean isRawXaConnFactory;
    private BeeConnectionFactory rawConnFactory;
    private BeeXaConnectionFactory rawXaConnFactory;
    private ProxyConnectionFactory conProxyFactory;
    private PooledConnectionAliveTest conValidTest;
    private ThreadPoolExecutor networkTimeoutExecutor;

    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds
    private boolean supportHoldTimeout;
    private IdleTimeoutScanWorker idleTimeoutConnectionScanThread;
    private long methodLogTimeoutMs;//milliseconds
    private MethodLogTimeoutScanWorker timeoutMethodLogClearThread;

    private boolean useThreadLocal;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private FastConnectionPoolMonitorVo monitorVo;
    private ConnectionPoolHook exitHook;

    //***************************************************************************************************************//
    //               1: Pool initializes and maintenance on pooled connections(6)                                    //                                                                                  //
    //***************************************************************************************************************//
    //Method-1.1: pool initializes.
    public void start(BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new PoolInitializeFailedException("Data source configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {//initializes after cas success to change pool state
            try {
                checkJdbcProxyClass();
                startupInternal(POOL_STARTING, config.check());
                this.poolState = POOL_READY;//ready to accept coming requests(love u,my pool)
            } catch (Throwable e) {
                logPrinter.info("BeeCP({})initialized failed", this.poolName, e);
                this.poolState = POOL_NEW;//reset state to new after failure
                throw e instanceof SQLException ? (SQLException) e : new PoolInitializeFailedException(e);
            }
        } else {
            throw new PoolInitializeFailedException("Pool has already initialized or in initializing");
        }
    }

    // Method-1.2: launch the pool
    private void startupInternal(final int poolWorkState, BeeDataSourceConfig poolConfig) throws SQLException {
        this.poolConfig = poolConfig;
        this.poolName = poolConfig.getPoolName();
        this.logPrinter = LogPrinterFactory.getLogPrinter(poolConfig.isPrintRuntimeLogs() ? FastConnectionPool.class : null);
        logPrinter.info("BeeCP({})starting up....", this.poolName);

        //step1: copy connection factory to pool local
        Object rawFactory = poolConfig.getConnectionFactory();
        if (rawFactory instanceof BeeXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (BeeXaConnectionFactory) rawFactory;
        } else {
            this.rawConnFactory = (BeeConnectionFactory) rawFactory;
        }

        //step2: create a fixed length array to store pooled connections(empty array)
        this.connectionArrayInitialized = false;
        this.connectionArrayLen = poolConfig.getMaxActive();
        this.connectionArray = new PooledConnection[connectionArrayLen];
        this.connectionArrayInitLock = new InterruptableReentrantReadWriteLock();//a lock for connection array initializing
        for (int i = 0; i < connectionArrayLen; i++)
            connectionArray[i] = new PooledConnection(this);

        //step3: fill initial connections to array by syn mode
        this.maxWaitMs = poolConfig.getMaxWait();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);
        int initialSize = poolConfig.getInitialSize();
        if (initialSize > 0 && !poolConfig.isAsyncCreateInitConnection())
            createInitConnections(poolConfig.getInitialSize(), true);

        //step4: create connection transfer policy tool(fair or unfair)
        if (poolConfig.isFairMode()) {
            poolMode = "fair";
            isFairMode = true;
            this.transferPolicy = new FairTransferPolicy();
        } else {
            poolMode = "compete";
            isCompeteMode = true;
            this.transferPolicy = this;
        }
        this.stateCodeOnRelease = this.transferPolicy.getStateCodeOnRelease();

        //step5: copy some time type parameters to local
        this.idleTimeoutMs = poolConfig.getIdleTimeout();
        this.holdTimeoutMs = poolConfig.getHoldTimeout();
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.aliveAssumeTimeMs = poolConfig.getAliveAssumeTime();
        this.aliveTestTimeout = poolConfig.getAliveTestTimeout();
        this.parkTimeForRetryNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getParkTimeForRetry());
        this.methodLogTimeoutMs = poolConfig.getMethodExecutionLogTimeout();

        //step6: creates pool semaphore and create threadLocal by configured indicator
        this.semaphoreSize = poolConfig.getSemaphoreSize();
        this.semaphore = new InterruptableSemaphore(this.semaphoreSize, isFairMode);
        this.useThreadLocal = poolConfig.isUseThreadLocal();
        if (this.threadLocal != null) this.threadLocal = null;//set null if not null
        if (useThreadLocal) this.threadLocal = new BorrowerThreadLocal();

        //step7: create wait queue,scanning thread,servant thread(an async thread to get connections and transfer to waiters)
        if (POOL_STARTING == poolWorkState) {
            //7.1: create wait queue
            this.waitQueue = new ConcurrentLinkedQueue<>();
            //7.2: create servant thread
            this.servantTryCount = 0;
            this.servantState = THREAD_WORKING;
            //7.3: create timeout scanning thread
            this.idleTimeoutConnectionScanThread = new IdleTimeoutScanWorker(this,
                    poolConfig.getIntervalOfClearTimeout(),
                    "BeeCP(" + poolName + ")" + "-idleScanner");

            //7.4: create method cache and its scanning thread
            this.methodLogCache = new MethodExecutionLogCache(
                    poolConfig.getMethodExecutionLogCacheSize(),
                    poolConfig.getSlowConnectionThreshold(),
                    poolConfig.getSlowSQLThreshold(),
                    poolConfig.getMethodExecutionListener());
            this.timeoutMethodLogClearThread = new MethodLogTimeoutScanWorker(this,
                    poolConfig.getIntervalOfClearTimeoutExecutionLogs(),
                    "BeeCP(" + poolName + ")" + "-methodExecutionLogTimeoutScanner");
            //7.5: create pool monitor object
            this.monitorVo = new FastConnectionPoolMonitorVo(poolName, poolMode, connectionArrayLen, semaphoreSize);
            this.exitHook = new ConnectionPoolHook(this);//a hook works when JVM exit
            //7.6: register JVM hook
            Runtime.getRuntime().addShutdownHook(this.exitHook);
            //7.7: register JMX
            this.registerJmx(poolConfig);

            //7.8: start pool threads
            setDaemon(true);
            setName("BeeCP(" + poolName + ")" + "-asyncAdd");
            start();

            this.idleTimeoutConnectionScanThread.start();
            this.timeoutMethodLogClearThread.start();
        } else {
            this.idleTimeoutConnectionScanThread.setInterval(poolConfig.getIntervalOfClearTimeout());
            this.timeoutMethodLogClearThread.setInterval(poolConfig.getIntervalOfClearTimeoutExecutionLogs());
        }

        //step8: create connection proxy factory
        if (poolConfig.isEnableMethodExecutionLogCache()) {
            this.conProxyFactory = new ProxyConnectionFactory4L(methodLogCache);
            this.usingMethodLogCache = true;
        } else {
            this.conProxyFactory = new ProxyConnectionFactory();
            this.usingMethodLogCache = false;
        }

        //step9: create a thread to do pool initialization(create initial connections and fill them to array)
        if (initialSize > 0 && poolConfig.isAsyncCreateInitConnection())
            new PoolInitAsyncCreateThread(this, initialSize, "BeeCP(" + poolName + ")" + "-asyncInitialConnectionCreator").start();

        //step10: print pool info at end
        String poolInitInfo;
        String driverClassNameOrFactoryName = poolConfig.getDriverClassName();
        if (isNotBlank(driverClassNameOrFactoryName)) {
            poolInitInfo = "BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}";
        } else {
            driverClassNameOrFactoryName = rawFactory.getClass().getName();
            poolInitInfo = "BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,factory:{}}";
        }
        logPrinter.info(poolInitInfo, poolName, poolMode, initialSize, connectionArrayLen, semaphoreSize, maxWaitMs, driverClassNameOrFactoryName);
    }

    //Method-1.3: creates initial connections
    void createInitConnections(int initSize, boolean syn) throws SQLException {
        ReentrantReadWriteLock.WriteLock writeLock = connectionArrayInitLock.writeLock();
        boolean isWriteLocked = !syn && !connectionArrayInitialized && !connectionArrayInitLock.isWriteLocked() && writeLock.tryLock();

        if (syn || isWriteLocked) {//sys or async locked
            int index = 0;
            try {
                Thread creatingThread = Thread.currentThread();
                while (index < initSize) {
                    PooledConnection p = connectionArray[index];
                    p.state = CON_CREATING;
                    this.fillRawConnection(p, CON_IDLE, creatingThread);
                    index++;
                }
            } catch (SQLException e) {
                if (syn) {
                    for (int i = 0; i < index; i++)
                        connectionArray[i].onRemove(DESC_RM_INIT);
                    throw e;
                } else {//print log under async mode
                    logPrinter.warn("Failed to create initial connections by async mode", e);
                }
            } finally {
                if (isWriteLocked) writeLock.unlock();
            }
        }
    }

    //Method-1.4: Search one or create one
    private PooledConnection searchOrCreate(Thread creatingThread) throws SQLException {
        //1: do initialization on connection array
        if (!connectionArrayInitialized) {
            ReentrantReadWriteLock.ReadLock readLock = connectionArrayInitLock.readLock();
            ReentrantReadWriteLock.WriteLock writeLock = connectionArrayInitLock.writeLock();
            try {
                if (!connectionArrayInitLock.isWriteLocked() && writeLock.tryLock()) {
                    try {
                        PooledConnection p = connectionArray[0];
                        p.state = CON_CREATING;
                        return this.fillRawConnection(p, CON_BORROWED, creatingThread);
                    } finally {
                        writeLock.unlock();
                    }
                } else if (readLock.tryLock(this.maxWaitNs, TimeUnit.NANOSECONDS)) {
                    readLock.unlock();
                    if (!connectionArrayInitialized)
                        throw new ConnectionGetException("Pool first connection created fail or first connection initialized fail");
                } else {
                    throw new ConnectionGetTimeoutException("Waited timeout on pool lock");
                }
            } catch (InterruptedException e) {
                throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool lock");
            }
        }

        //2: attempt to get an idle connection from head to tail by cas way or create one when in NOT Created state
        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_IDLE) {
                if (ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED)) {
                    if (this.testOnBorrow(p)) return p;
                } else if (p.state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                    return this.fillRawConnection(p, CON_BORROWED, creatingThread);
                }
            } else if (state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                return this.fillRawConnection(p, CON_BORROWED, creatingThread);
            }
        }
        return null;
    }

    //Method-1.5: create a created connection and fill it into pooled Connection(a pooled wrapper of connection)
    private PooledConnection fillRawConnection(PooledConnection p, int state, Thread creatingThread) throws SQLException {
        //1: print info of creation starting
        logPrinter.info("BeeCP({}))start to create a connection", this.poolName);

        //2: use factory to create a connection
        Connection rawConn = null;
        XAConnection rawXaConn = null;
        XAResource rawXaRes = null;

        try {
            p.creatingInfo = new ConnectionCreatingInfo(creatingThread);//set creating info(thread,start time)which can be read out by calling monitor method
            if (this.isRawXaConnFactory) {
                rawXaConn = this.rawXaConnFactory.create();//this call may be blocked
                if (rawXaConn == null) {
                    if (creatingThread.isInterrupted() && Thread.interrupted())
                        throw new ConnectionGetInterruptedException("An interruption occurred when created an XA connection");
                    throw new ConnectionCreateException("A unknown error occurred when created an XA connection");
                }
                rawConn = rawXaConn.getConnection();
                rawXaRes = rawXaConn.getXAResource();
            } else {
                rawConn = this.rawConnFactory.create();
                if (rawConn == null) {
                    if (creatingThread.isInterrupted() && Thread.interrupted())
                        throw new ConnectionGetInterruptedException("An interruption occurred when created a connection");
                    throw new ConnectionCreateException("A unknown error occurred when created a connection");
                }
            }

            //3: set default on created connection
            if (this.connectionArrayInitialized) {
                p.setRawConnection(state, rawConn, rawXaRes);
            } else {
                this.initPooledConnectionArray(rawConn);
                p.setRawConnection2(state, rawConn, rawXaRes);
                this.connectionArrayInitialized = true;//set initialization flag to true
            }

            //4: print log of creation ending
            logPrinter.info("BeeCP({}))created a new connection:{} to fill pooled connection:{}", this.poolName, rawConn, p);

            //5: return result
            return p;
        } catch (Throwable e) {
            p.state = CON_CLOSED;//reset to closed state
            if (rawConn != null) oclose(rawConn);
            else if (rawXaConn != null) oclose(rawXaConn);
            throw e instanceof SQLException ? (SQLException) e : new ConnectionCreateException(e);
        } finally {
            p.creatingInfo = null;//clear filling of pooled connection
        }
    }

    //Method-1.6: initialize pool array with first connection(read default from it or do some test on it)
    private void initPooledConnectionArray(Connection firstConn) throws SQLException {
        //step1: initialization for auto-commit property of connection(default get and default set)
        boolean defaultAutoCommit = true;
        boolean enableDefaultAutoCommit = poolConfig.isEnableDefaultAutoCommit();
        if (enableDefaultAutoCommit) {
            if (poolConfig.isDefaultAutoCommit() == null) {
                try {
                    defaultAutoCommit = firstConn.getAutoCommit();
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueGetException("Failed to get default value of 'auto-commit' from initial test connection", e);
                }
            } else {
                defaultAutoCommit = poolConfig.isDefaultAutoCommit().booleanValue();
            }

            try {
                firstConn.setAutoCommit(defaultAutoCommit);//default set test
            } catch (Throwable e) {
                throw new ConnectionDefaultValueSetException("Failed to set default value(" + defaultAutoCommit + ") of 'auto-commit' to initial test connection", e);
            }
        }

        //step2: initialization for transaction-isolation property of connection(default get and default set)
        int defaultTransactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
        boolean enableDefaultTransactionIsolation = poolConfig.isEnableDefaultTransactionIsolation();
        if (enableDefaultTransactionIsolation) {
            if (poolConfig.getDefaultTransactionIsolation() == null) {
                try {
                    defaultTransactionIsolation = firstConn.getTransactionIsolation();
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueGetException("Failed to get default value of 'transaction-isolation' from initial test connection", e);
                }
            } else {
                defaultTransactionIsolation = poolConfig.getDefaultTransactionIsolation().intValue();
            }

            try {
                firstConn.setTransactionIsolation(defaultTransactionIsolation);
            } catch (Throwable e) {
                throw new ConnectionDefaultValueSetException("Failed to set default value(" + defaultTransactionIsolation + ") of 'transaction-isolation' to initial test connection", e);
            }
        }

        //step3:get default value of property read-only from config or from first connection(default get and default set)
        boolean defaultReadOnly = false;
        boolean enableDefaultReadOnly = poolConfig.isEnableDefaultReadOnly();
        if (enableDefaultReadOnly) {
            if (poolConfig.isDefaultReadOnly() == null) {
                try {
                    defaultReadOnly = firstConn.isReadOnly();
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueGetException("Failed to get default value of 'read-only' from initial test connection", e);
                }
            } else {
                defaultReadOnly = poolConfig.isDefaultReadOnly().booleanValue();
            }

            try {
                firstConn.setReadOnly(defaultReadOnly);//set default
            } catch (Throwable e) {
                throw new ConnectionDefaultValueSetException("Failed to set default value(" + defaultReadOnly + ") of 'read-only' to initial test connection", e);
            }
        }

        //step4: initialization for catalog property of connection(get default,test default)
        String defaultCatalog = poolConfig.getDefaultCatalog();
        boolean enableDefaultOnCatalog = poolConfig.isEnableDefaultCatalog();
        if (enableDefaultOnCatalog) {
            if (isBlank(defaultCatalog)) {
                try {
                    defaultCatalog = firstConn.getCatalog();
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueGetException("Failed to get default value of 'catalog' from initial test connection", e);
                }
            }

            if (isNotBlank(defaultCatalog)) {
                try {
                    firstConn.setCatalog(defaultCatalog);//set default
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueSetException("Failed to set default value(" + defaultCatalog + ") of 'catalog' to initial test connection", e);
                }
            }
        }

        //step5: initialization for schema property of connection(get default,test default)
        String defaultSchema = poolConfig.getDefaultSchema();
        boolean enableDefaultSchema = poolConfig.isEnableDefaultSchema();
        if (enableDefaultSchema) {
            if (isBlank(defaultSchema)) {
                try {
                    defaultSchema = firstConn.getSchema();
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueGetException("Failed to get default value of 'schema' from initial test connection", e);
                }
            }
            if (isNotBlank(defaultSchema)) {
                try {
                    firstConn.setSchema(defaultSchema);//set default
                } catch (Throwable e) {
                    throw new ConnectionDefaultValueSetException("Failed to set default value(" + defaultSchema + ") of 'schema' to initial test connection", e);
                }
            }
        }

        //step6: check isValid method of connection,if passed,then use this method to do alive check on borrowed connections
        boolean supportIsValid = true;//assume support
        try {
            if (firstConn.isValid(this.aliveTestTimeout)) {
                conValidTest = this;
            } else {
                supportIsValid = false;
                logPrinter.warn("BeeCP({})Driver not support 'isValid' method call on connection", this.poolName);
            }
        } catch (Throwable e) {
            supportIsValid = false;
            logPrinter.warn("BeeCP({})Exception occurred when call 'isValid' method on initial test connection", this.poolName, e);
        }

        //step7: second way: if isValid method is not supported, then execute alive test sql to validate it
        if (!supportIsValid) {
            String conTestSql = this.poolConfig.getAliveTestSql();
            boolean supportQueryTimeout = validateTestSQL(poolName, firstConn, conTestSql, aliveTestTimeout, defaultAutoCommit);//check test sql
            conValidTest = new PooledConnectionAliveTestBySql(poolName, conTestSql, aliveTestTimeout, defaultAutoCommit, supportQueryTimeout, this);
        }

        //step8: network timeout check supported in driver or factory
        int defaultNetworkTimeout = 0;
        boolean supportNetworkTimeoutInd = true;//assume supportable
        try {
            defaultNetworkTimeout = firstConn.getNetworkTimeout();
            if (defaultNetworkTimeout < 0) {
                supportNetworkTimeoutInd = false;
                logPrinter.warn("BeeCP({})Driver not support 'getNetworkTimeout()/setNetworkTimeout(time)' method call on connection", this.poolName);
            } else {//driver support networkTimeout
                if (this.networkTimeoutExecutor == null) {
                    int poolMaxSize = poolConfig.getMaxActive();
                    this.networkTimeoutExecutor = new ThreadPoolExecutor(poolMaxSize, poolMaxSize, 10L, SECONDS,
                            new LinkedBlockingQueue<Runnable>(poolMaxSize), new PoolThreadThreadFactory("BeeCP(" + poolName + ")"));
                    this.networkTimeoutExecutor.allowCoreThreadTimeOut(true);
                }
                firstConn.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            }
        } catch (Throwable e) {
            supportNetworkTimeoutInd = false;
            logPrinter.warn("BeeCP({})Exception occurred when call 'getNetworkTimeout()/setNetworkTimeout(time)' method on initial test connection", this.poolName, e);
            if (networkTimeoutExecutor != null) {
                networkTimeoutExecutor.shutdown();
                networkTimeoutExecutor = null;
            }
        }

        //step9: Ok,we fill default value prepared in previous steps to all pooled connections at final step
        for (int i = 0; i < connectionArrayLen; i++) {
            connectionArray[i].init(
                    //1:defaultAutoCommit
                    enableDefaultAutoCommit,
                    defaultAutoCommit,
                    //2:defaultTransactionIsolation
                    enableDefaultTransactionIsolation,
                    defaultTransactionIsolation,
                    //3:defaultReadOnly
                    enableDefaultReadOnly,
                    defaultReadOnly,
                    //4:defaultCatalog
                    enableDefaultOnCatalog,
                    defaultCatalog,
                    poolConfig.isForceDirtyWhenSetCatalog(),
                    //5:defaultCatalog
                    enableDefaultSchema,
                    defaultSchema,
                    poolConfig.isForceDirtyWhenSetSchema(),
                    //6:defaultNetworkTimeout
                    supportNetworkTimeoutInd,
                    defaultNetworkTimeout,
                    networkTimeoutExecutor,
                    //7:others
                    poolConfig.getSqlExceptionCodeList(),
                    poolConfig.getSqlExceptionStateList(),
                    poolConfig.getPredicate());
        }
    }

    //***************************************************************************************************************//
    //                               2: Connection getting and connection release (10)                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1: Attempts to get a connection from pool
    public Connection getConnection() throws SQLException {
        if (this.usingMethodLogCache) {
            BeeMethodExecutionLog log = methodLogCache.beforeCall(Type_Connection_Get, "FastConnectionPool.getConnection()", null, null, null);
            try {
                Connection con = this.conProxyFactory.createProxyConnection(this.getPooledConnection());
                methodLogCache.afterCall(con, 0L, null, log);
                return con;
            } catch (SQLException e) {
                methodLogCache.afterCall(e, 0L, null, log);
                throw e;
            }
        } else {
            return this.conProxyFactory.createProxyConnection(this.getPooledConnection());
        }
    }

    //Method-2.2: Attempts to get a XAConnection from pool
    public XAConnection getXAConnection() throws SQLException {
        if (this.usingMethodLogCache) {
            BeeMethodExecutionLog log = methodLogCache.beforeCall(Type_Connection_Get, "FastConnectionPool.getXAConnection()", null, null, null);
            try {
                PooledConnection p = this.getPooledConnection();
                ProxyConnectionBase proxyConn = this.conProxyFactory.createProxyConnection(p);
                XAResource proxyResource = this.isRawXaConnFactory ? new XaProxyResource(p.rawXaRes, proxyConn) : new XaResourceLocalImpl(proxyConn);
                XAConnection xaConn = new XaProxyConnection(proxyConn, proxyResource);
                methodLogCache.afterCall(xaConn, 0L, null, log);
                return xaConn;
            } catch (SQLException e) {
                methodLogCache.afterCall(e, 0L, null, log);
                throw e;
            }
        } else {
            PooledConnection p = this.getPooledConnection();
            ProxyConnectionBase proxyConn = this.conProxyFactory.createProxyConnection(p);
            XAResource proxyResource = this.isRawXaConnFactory ? new XaProxyResource(p.rawXaRes, proxyConn) : new XaResourceLocalImpl(proxyConn);
            return new XaProxyConnection(proxyConn, proxyResource);
        }
    }

    //Method-2.3: attempt to get pooled connection(key method)
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY)
            throw new ConnectionGetForbiddenException("Pool has been closed or is restarting");

        //1: try to reuse last used connection
        Borrower b = null;
        PooledConnection p;
        if (this.useThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED)) {
                    if (this.testOnBorrow(p)) return p;
                    b.lastUsed = null;//bad connection
                }
            }
        }

        try {
            //2: get a permit from pool semaphore(reduce concurrency to get connections)
            long deadline = System.currentTimeMillis();
            if (this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS)) {
                try {
                    //3: search an idle connection or create a connection on NOT filled pooled connection
                    Thread borrowThread = b != null ? b.thread : Thread.currentThread();
                    if ((p = this.searchOrCreate(borrowThread)) != null) {
                        if (this.useThreadLocal) {
                            if (b != null)
                                b.lastUsed = p;
                            else
                                this.threadLocal.set(new WeakReference<>(new Borrower(borrowThread, p)));
                        }
                        return p;
                    }

                    //4: join into wait queue for transferred connection released from another borrower
                    if (b != null)
                        b.state = null;
                    else {
                        b = new Borrower(borrowThread);
                        if (this.useThreadLocal) this.threadLocal.set(new WeakReference<>(b));
                    }
                    this.waitQueue.offer(b);

                    deadline += this.maxWaitMs;
                    //5: spin in a loop
                    do {
                        Object s = b.state;//acceptable types: PooledConnection,Throwable,null
                        if (s instanceof PooledConnection) {
                            p = (PooledConnection) s;
                            if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                                this.waitQueue.remove(b);
                                return b.lastUsed = p;
                            }
                        } else if (s instanceof Throwable) {//here: s must be throwable object
                            this.waitQueue.remove(b);
                            throw s instanceof SQLException ? (SQLException) s : new ConnectionGetException((Throwable) s);
                        }

                        long t = deadline - System.currentTimeMillis();
                        if (t > 0L) {//timeout check,if not,then attempt to wake up servant thread to get one for it before parking
                            if (s != null) b.state = null;
                            if (this.servantTryCount > 0 && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
                                LockSupport.unpark(this);

                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(t));//park over,a transferred connection maybe arrived or an exception,or an interruption occurred while waiting
                            if (borrowThread.isInterrupted() && Thread.interrupted())
                                this.handleTimeoutAndInterruption(false, null, b);
                        } else {//if not get a connection then throws a timeout exception
                            return this.handleTimeoutAndInterruption(true, s, b);
                        }
                    } while (true);//while
                } finally {
                    semaphore.release();
                }
            } else {
                throw new ConnectionGetTimeoutException("Waited timeout on pool semaphore");
            }
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        }
    }

    //Method-2.4: handle timeout and interruption in spin
    private PooledConnection handleTimeoutAndInterruption(boolean isTimeout, Object s, Borrower b) throws SQLException {
        this.waitQueue.remove(b);

        PooledConnection p = null;
        if (s == null) {
            s = b.state;
            if (s instanceof PooledConnection) {
                p = (PooledConnection) s;
                if (!(this.transferPolicy.tryCatch(p) && this.testOnBorrow(p))) {
                    p = null;
                }
            } else if (s instanceof Throwable) {
                throw s instanceof SQLException ? (SQLException) s : new ConnectionGetException((Throwable) s);
            }
        }

        if (isTimeout) {
            if (p != null) return b.lastUsed = p;
            throw new ConnectionGetTimeoutException("Waited timeout for a released connection");
        } else {
            if (p != null) this.recycle(p);
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting for a released connection");
        }
    }


    //Method-2.5: increment servant's count of retry to get connections
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount;
            if (c >= this.connectionArrayLen) return;
        } while (!ServantTryCountUpd.compareAndSet(this, c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    //Method-2.6: recycle a pooled connection and then transfer it to a waiter if exists
    void recycle(PooledConnection p) {
        if (isCompeteMode) p.state = CON_IDLE;
        for (Borrower b : this.waitQueue) {
            if (p.state != stateCodeOnRelease) return;
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, p)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }

        if (isFairMode) p.state = CON_IDLE;
        tryWakeupServantThread();
    }

    //Method-2.7: terminate a Pooled Connection
    void abort(PooledConnection p, String reason) {
        p.onRemove(reason);
        this.tryWakeupServantThread();
    }

    //Method-2.8: transfer an exception occurred in searching,this method called by servant thread
    private void transferException(Throwable e) {
        for (Borrower b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-2.9: do alive test on a borrowed pooled connection
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime - this.aliveAssumeTimeMs >= 0L && !this.conValidTest.isAlive(p)) {
            p.onRemove(DESC_RM_BAD);
            this.tryWakeupServantThread();
            return false;
        } else {
            return true;
        }
    }

    public int getStateCodeOnRelease() {
        return CON_IDLE;
    }

    public boolean tryCatch(PooledConnection p) {
        return p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED);
    }

    //***************************************************************************************************************//
    //                       3: add new connections and clean timeout connections(3)                                  //                                                                                  //
    //***************************************************************************************************************//
    //Method-3.1: shutdown pool inner threads
    private void shutdownPoolThreads() {
        int curState = this.servantState;
        this.servantState = THREAD_EXIT;
        if (curState == THREAD_WAITING) LockSupport.unpark(this);

        idleTimeoutConnectionScanThread.shutdown();
        timeoutMethodLogClearThread.shutdown();
    }

    //Method-3.2: run method of pool servant
    public void run() {
        Thread currentThread = Thread.currentThread();
        do {
            int servantState = this.servantState;
            if (servantState == THREAD_EXIT) break;
            if (servantState == THREAD_WORKING) {
                if (servantTryCount > 0 && !waitQueue.isEmpty()) {
                    ServantTryCountUpd.decrementAndGet(this);//only here to decrement
                    try {
                        PooledConnection p = searchOrCreate(currentThread);
                        if (p != null) recycle(p);
                    } catch (Throwable e) {
                        this.transferException(e);
                    }
                } else if (ServantStateUpd.compareAndSet(this, THREAD_WORKING, THREAD_WAITING)) {
                    LockSupport.park();
                }
            } else {//THREAD_WAITING(maybe park fail)
                this.servantState = THREAD_WORKING;
            }
        } while (true);
    }

    //Method-3.3: clean timeout connections(idle timeout and hold timeout)
    void closeIdleTimeoutConnections() {
        //step1:print pool info before clean
        if (logPrinter.isOutputLogs()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            logPrinter.info("BeeCP({})before timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: interrupt current creation of a connection when this operation is timeout
        this.interruptTimeoutBlockingCreation();

        //step3: clean timeout connection in a loop
        for (PooledConnection p : this.connectionArray) {
            final int state = p.state;
            if (state == CON_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                if (isTimeoutInIdle && ConStUpd.compareAndSet(p, state, CON_CLOSED)) {//need close idle
                    p.onRemove(DESC_RM_IDLE);
                    this.tryWakeupServantThread();
                }
            } else if (state == CON_BORROWED && supportHoldTimeout) {
                if (System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) oclose(proxyInUsing);
                }
            }
        }

        //step4: print pool info after clean
        if (logPrinter.isOutputLogs()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            logPrinter.info("BeeCP({})after timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //Method-3.4: Clear timeout jdbc logs
    private void clearMethodTimeoutLogs() {
        methodLogCache.clearTimeout(this.methodLogTimeoutMs);
    }

    //***************************************************************************************************************//
    //                                  4: pool clean and pool close  (6)                                            //                                                                                  //
    //***************************************************************************************************************//
    public void restart(boolean forceRecycleBorrowed) throws SQLException {
        restart(forceRecycleBorrowed, false, null);
    }

    //Method-4.2: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    public void restart(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        restart(forceRecycleBorrowed, true, config);
    }

    //Method-4.3: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    private void restart(boolean forceRecycleBorrowed, boolean reinit, BeeDataSourceConfig config) throws SQLException {
        if (reinit && config == null)
            throw new BeeDataSourceConfigException("Data source configuration can't be null");

        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_RESTARTING)) {
            try {
                BeeDataSourceConfig checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                logPrinter.info("BeeCP({})begin to remove all connections", this.poolName);
                this.removeAllConnections(forceRecycleBorrowed, DESC_RM_CLEAR);
                logPrinter.info("BeeCP({})completed to remove all connections", this.poolName);

                if (reinit) {
                    logPrinter.info("BeeCP({})start to reinitialize pool", this.poolName);
                    startupInternal(POOL_RESTARTING, checkedConfig);//throws SQLException only fail to create initial connections or fail to set default
                    //note: if failed,this method may be recalled with correct configuration
                    logPrinter.info("BeeCP({})pool restart successful", this.poolName);
                }
            } finally {
                this.poolState = POOL_READY;
            }
        } else {
            throw new PoolInClearingException("Pool has been closed or is restarting");
        }
    }

    //Method-4.4: remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        //1: interrupt all threads waits on lock or blocking in factory.create method call
        this.interruptWaitingThreads();

        //2:clear all connections
        int closedCount = 0;
        while (true) {
            for (PooledConnection p : this.connectionArray) {
                final int state = p.state;
                if (state == CON_IDLE) {
                    if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) {
                        closedCount++;
                        p.onRemove(source);
                    }
                } else if (state == CON_BORROWED) {
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) {
                        if (force || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L)) //force close or hold timeout
                            oclose(proxyInUsing);
                    }
                } else if (state == CON_CLOSED) {
                    closedCount++;
                }
            }

            if (closedCount == this.connectionArrayLen) break;
            LockSupport.parkNanos(this.parkTimeForRetryNs);//delay to clear remained pooled connections
            closedCount = 0;
        } // while

        if (logPrinter.isOutputLogs()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            logPrinter.info("BeeCP({})after clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //Method-4.5: closed check
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    public boolean isReady() {
        return this.poolState == POOL_READY;
    }

    //Method-4.6: shut down the pool and set its state to closed
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_RESTARTING) {
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                logPrinter.info("BeeCP({})begin to shutdown pool", this.poolName);
                this.unregisterJmx();
                this.shutdownPoolThreads();
                this.removeAllConnections(this.poolConfig.isForceRecycleBorrowedOnClose(), DESC_RM_DESTROY);
                if (networkTimeoutExecutor != null) this.networkTimeoutExecutor.shutdownNow();

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }
                this.poolState = POOL_CLOSED;
                logPrinter.info("BeeCP({})has shutdown pool", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                  5: Pool log print and method execution logs cache(6)                         //                                                                                  //
    //***************************************************************************************************************//
    public boolean isEnabledLogPrint() {
        return logPrinter.isOutputLogs();
    }

    public void enableLogPrint(boolean enable) {
        this.logPrinter = LogPrinterFactory.getLogPrinter(enable ? FastConnectionPool.class : null);
    }

    public boolean isEnabledMethodExecutionLogCache() {
        return this.usingMethodLogCache;
    }

    public List<BeeMethodExecutionLog> getMethodExecutionLog(int type) {
        return this.methodLogCache.getLog(type);
    }

    public List<BeeMethodExecutionLog> clearMethodExecutionLog(int type) {
        return methodLogCache.clear(type);
    }

    public void enableMethodExecutionLogCache(boolean enable) {
        if (enable) {//enable
            if (!usingMethodLogCache) {//re-enable
                this.conProxyFactory = new ProxyConnectionFactory4L(methodLogCache);
                this.usingMethodLogCache = true;
            }
        } else if (usingMethodLogCache) {//disable
            this.conProxyFactory = new ProxyConnectionFactory();
            this.usingMethodLogCache = false;
        }
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener listener) {
        methodLogCache.setMethodExecutionListener(listener);
    }

    public boolean cancelStatement(Object id) throws SQLException {
        return methodLogCache.cancelStatement(id);
    }

    //***************************************************************************************************************//
    //                                  6: Jmx methods (16)                                                          //                                                                                  //
    //***************************************************************************************************************//
    public String getPoolName() {
        return this.poolName;
    }

    public int getSemaphoreSize() {
        return this.semaphoreSize;
    }

    public int getSemaphoreWaitingSize() {
        return this.semaphore.getQueueLength();
    }

    public int getSemaphoreAcquiredSize() {
        return semaphoreSize - this.semaphore.availablePermits();
    }

    public boolean isPrintRuntimeLog() {
        return logPrinter.isOutputLogs();
    }

    public void setPrintRuntimeLog(boolean enable) {
        enableLogPrint(enable);
    }

    public int getMaxSize() {
        return this.connectionArrayLen;
    }

    public int getIdleSize() {
        int idleSize = 0;
        for (PooledConnection p : connectionArray) {
            if (p.state == CON_IDLE) idleSize++;
        }
        return idleSize;
    }

    public int getBorrowedSize() {
        int usingSize = 0;
        for (PooledConnection p : connectionArray) {
            if (p.state == CON_BORROWED) usingSize++;
        }
        return usingSize;
    }

    public int getTotalSize() {
        int totalSize = 0;
        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_IDLE || state == CON_BORROWED) totalSize++;
        }
        return totalSize;
    }

    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : this.waitQueue)
            if (borrower.state == null) size++;
        return size;
    }

    private void registerJmx(BeeDataSourceConfig poolConfig) {
        if (poolConfig.isRegisterMbeans()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName), poolConfig);
        }
    }

    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            mBeanServer.registerMBean(bean, new ObjectName(regName));
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})failed to register jmx-bean:{}", this.poolName, regName, e);
        }
    }

    private void unregisterJmx() {
        if (this.poolConfig.isRegisterMbeans()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName));
        }
    }

    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            mBeanServer.unregisterMBean(new ObjectName(regName));
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //***************************************************************************************************************//
    //                                  7: other methods (3)                                                         //                                                                                  //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads() {
        List<Thread> threads = new LinkedList<>();
        //1: clear waiting thread on semaphore
        if (this.semaphore != null) threads.addAll(this.semaphore.interruptQueuedWaitThreads());

        //2: transfer exception to waiter in queue
        if (this.waitQueue != null && !this.waitQueue.isEmpty()) {
            PoolInClearingException exception = new PoolInClearingException("Pool has been closed or is restarting");
            while (!this.waitQueue.isEmpty()) this.transferException(exception);
        }

        //3: interrupt threads on lock
        if (connectionArrayInitLock != null) threads.addAll(connectionArrayInitLock.interruptAllThreads());

        //4: attempt to interrupt creating of connections
        if (connectionArray != null) {
            for (PooledConnection p : connectionArray) {
                ConnectionCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        }

        return threads;
    }

    private void interruptTimeoutBlockingCreation() {
        for (PooledConnection p : connectionArray) {
            ConnectionCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null && System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L) {
                creatingInfo.creatingThread.interrupt();
            }
        }
    }

    //Method-7.2: do alive test on a pooed connection
    public boolean isAlive(final PooledConnection p) {
        try {
            if (p.rawConn.isValid(this.aliveTestTimeout)) {
                p.lastAccessTime = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})alive test failed on a borrowed connection", this.poolName, e);
        }
        return false;
    }

    //Method-7.3: pool monitor vo
    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        int borrowedSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;
        int semaphoreWaitingSize = this.getSemaphoreWaitingSize();
        int transferWaitingSize = this.getTransferWaitingSize();

        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_BORROWED) borrowedSize++;
            if (state == CON_IDLE) idleSize++;

            ConnectionCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) {
                creatingCount++;
                if (System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L)
                    creatingTimeoutCount++;
            }
        }

        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setSemaphoreAcquiredSize(this.semaphoreSize - this.semaphore.availablePermits());
        monitorVo.setBorrowedSize(borrowedSize);
        monitorVo.setCreatingSize(creatingCount);
        monitorVo.setCreatingTimeoutSize(creatingTimeoutCount);
        monitorVo.setSemaphoreWaitingSize(semaphoreWaitingSize);
        monitorVo.setTransferWaitingSize(transferWaitingSize);
        monitorVo.setEnabledLogPrint(this.logPrinter.isOutputLogs());
        monitorVo.setEnableMethodExecutionLogCache(this.usingMethodLogCache);
        return this.monitorVo;
    }

    //***************************************************************************************************************//
    //                                  8: some inner classes                                                        //                                                                                  //
    //***************************************************************************************************************//
    //class-8.1:Thread factory
    private static class PoolThreadThreadFactory implements ThreadFactory {
        private final String poolName;

        PoolThreadThreadFactory(String poolName) {
            this.poolName = poolName;
        }

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, poolName + "-networkTimeoutRestThread");
            th.setDaemon(true);
            return th;
        }
    }


    //class-8.2: A thread running to create new connections when pool starting up
    private static final class PoolInitAsyncCreateThread extends Thread {
        private final int initialSize;
        private final FastConnectionPool pool;

        PoolInitAsyncCreateThread(FastConnectionPool pool, int initialSize, String threadName) {
            super(threadName);
            this.pool = pool;
            this.initialSize = initialSize;
        }

        public void run() {
            try {
                pool.createInitConnections(initialSize, false);
                pool.servantTryCount = pool.connectionArray.length;

                if (!pool.waitQueue.isEmpty() && pool.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(pool, THREAD_WAITING, THREAD_WORKING))
                    LockSupport.unpark(pool);//wakeup servant thread
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    //class-8.3: A timed thread to scan idle connections and close them
    private static final class IdleTimeoutScanWorker extends Thread {
        private final FastConnectionPool pool;
        private long checkTimeIntervalNanos;
        private volatile int workerState;

        IdleTimeoutScanWorker(FastConnectionPool pool, long timerCheckInterval, String threadName) {
            super(threadName);
            this.pool = pool;
            this.workerState = THREAD_WORKING;//default status
            this.checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(timerCheckInterval);
            this.setDaemon(true);
        }

        void shutdown() {
            this.workerState = THREAD_EXIT;
            LockSupport.unpark(this);
        }

        void setInterval(long timerCheckInterval) {
            long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(timerCheckInterval);
            if (checkTimeIntervalNanos - this.checkTimeIntervalNanos != 0L) {
                this.checkTimeIntervalNanos = checkTimeIntervalNanos;
                LockSupport.unpark(this);
            }
        }

        public void run() {
            while (workerState == THREAD_WORKING) {
                LockSupport.parkNanos(checkTimeIntervalNanos);
                try {
                    pool.closeIdleTimeoutConnections();
                } catch (Throwable e) {
                    pool.logPrinter.warn("BeeCP({})an exception occurred while scanning idle-timeout connections", this.pool.poolName, e);
                }
            }
        }
    }

    //class-8.4: A timed thread to clear timeout jdbc logs
    private static final class MethodLogTimeoutScanWorker extends Thread {
        private final FastConnectionPool pool;
        private long checkTimeIntervalNanos;
        private volatile int workerState;

        MethodLogTimeoutScanWorker(FastConnectionPool pool,
                                   long jdbcCallLogClearInterval, String threadName) {
            super(threadName);
            this.pool = pool;
            this.workerState = THREAD_WORKING;//default status
            this.checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(jdbcCallLogClearInterval);
            this.setDaemon(true);
        }

        void shutdown() {
            this.workerState = THREAD_EXIT;
            LockSupport.unpark(this);
        }

        void setInterval(long timerCheckInterval) {
            long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(timerCheckInterval);
            if (checkTimeIntervalNanos - this.checkTimeIntervalNanos != 0L) {
                this.checkTimeIntervalNanos = checkTimeIntervalNanos;
                LockSupport.unpark(this);
            }
        }

        public void run() {
            while (workerState == THREAD_WORKING) {
                LockSupport.parkNanos(checkTimeIntervalNanos);
                try {
                    pool.clearMethodTimeoutLogs();
                } catch (Throwable e) {
                    pool.logPrinter.warn("BeeCP({})an exception occurred while scanning timeout method logs", this.pool.poolName, e);
                }
            }
        }
    }

    //class-8.5:JVM exit hook
    private static class ConnectionPoolHook extends Thread {
        private final FastConnectionPool pool;

        ConnectionPoolHook(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            pool.logPrinter.info("BeeCP({})detect Jvm exit,pool will be shutdown", this.pool.poolName);
            try {
                this.pool.close();
            } catch (Throwable e) {
                pool.logPrinter.error("BeeCP({})an exception occurred when shutdown pool", this.pool.poolName, e);
            }
        }
    }

    //class-8.6:Fair transfer
    private static final class FairTransferPolicy implements PooledConnectionTransferPolicy {
        FairTransferPolicy() {
        }

        public int getStateCodeOnRelease() {
            return CON_BORROWED;
        }

        public boolean tryCatch(PooledConnection p) {
            return p.state == CON_BORROWED;
        }
    }

    //class-8.7: threadLocal caches the last used connections of borrowers(only cache one per borrower)
    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<>(new Borrower(Thread.currentThread()));
        }
    }

    //class-8.8: Connection alive test by sql
    private static class PooledConnectionAliveTestBySql implements PooledConnectionAliveTest {
        private final String poolName;
        private final String testSql;
        private final int validTestTimeout;
        private final boolean isDefaultAutoCommit;
        private final boolean supportQueryTimeout;
        private final FastConnectionPool pool;

        public PooledConnectionAliveTestBySql(String poolName, String testSql, int validTestTimeout,
                                              boolean isDefaultAutoCommit, boolean supportQueryTimeout,
                                              FastConnectionPool pool) {
            this.poolName = poolName;
            this.testSql = testSql;
            this.validTestTimeout = validTestTimeout;
            this.isDefaultAutoCommit = isDefaultAutoCommit;
            this.supportQueryTimeout = supportQueryTimeout;
            this.pool = pool;
        }

        //In order to avoid possible dirty data into db,the value of auto-commit property of the target connection must be false
        public boolean isAlive(PooledConnection p) {//
            Statement st = null;
            boolean changed = false;
            Connection rawConn = p.rawConn;
            boolean checkPassed = true;//assume test result passed

            try {
                //step1: setAutoCommit(false) if in auto-commit mode
                if (this.isDefaultAutoCommit) {
                    rawConn.setAutoCommit(false);
                    changed = true;
                }

                //step2: create statement to for test
                st = rawConn.createStatement();
                if (this.supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(validTestTimeout);//time unit is second
                    } catch (Throwable e) {
                        pool.logPrinter.warn("BeeCP({})failed to set query timeout value on statement of a borrowed connection", poolName, e);
                    }
                }

                //step3: execute test sql
                try {
                    st.execute(this.testSql);
                    p.lastAccessTime = System.currentTimeMillis();
                } finally {
                    rawConn.rollback();//avoid possible dirty data into db
                }
            } catch (Throwable e) {
                checkPassed = false;
                pool.logPrinter.warn("BeeCP({})connection alive test failed with sql,pool will abandon it", poolName, e);
            } finally {
                if (st != null) oclose(st);
                if (changed) {
                    try {
                        rawConn.setAutoCommit(true);
                    } catch (Throwable e) {
                        pool.logPrinter.warn("BeeCP({})failed to reset 'auto-commit' to false after alive test,pool will abandon test connection", poolName, e);
                        checkPassed = false;
                    }
                }
            }

            return checkPassed;
        }
    }
}
