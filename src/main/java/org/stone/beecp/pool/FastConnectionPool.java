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
import org.stone.beecp.exception.*;
import org.stone.tools.BeanUtil;
import org.stone.tools.LogPrinter;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;
import org.stone.tools.extension.InterruptableSemaphore;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
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
import static org.stone.beecp.BeeMethodLog.Type_All;
import static org.stone.beecp.BeeMethodLog.Type_Pool_Log;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.*;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public class FastConnectionPool extends Thread implements BeeConnectionPool, FastConnectionPoolMXBean, PooledConnectionAliveTest, PooledConnectionTransferPolicy {
    static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantState");
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantTryCount");
    LogPrinter logPrinter = DefaultLogPrinter;

    String poolName;
    volatile int poolState;
    volatile int servantState;
    volatile int servantTryCount;
    BeeDataSourceConfig poolConfig;
    PooledConnection[] connectionArray;//fixed len
    ConcurrentLinkedQueue<Borrower> waitQueue;
    long methodLogTimeoutMs;//milliseconds
    MethodExecutionLogCache methodLogCache;
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
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private ScheduledFuture<?> timeoutLogsClearTaskFuture;
    private ScheduledFuture<?> timeoutConnectionsClearTaskFuture;
    private FastConnectionPoolMonitorVo simplePoolMonitorVo;

    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds
    private boolean supportHoldTimeout;
    private boolean collectMethodLogs;
    private boolean useThreadLocal;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private String poolNameOfRegisteredMBean;
    private ConnectionPoolHook exitHook;

    //***************************************************************************************************************//
    //                                         1: Pool Startup(1+1)                                                  //
    //***************************************************************************************************************//
    public void start(BeeDataSourceConfig config) throws SQLException {
        if (config == null)
            throw new BeeDataSourcePoolStartedFailureException("Data source configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {//initializes after cas success to change pool state
            try {
                checkJdbcProxyClass();
                startupInternal(POOL_STARTING, config.check());
                this.poolState = POOL_READY;//ready to accept coming requests(love u,my pool)
            } catch (Throwable e) {
                logPrinter.info("BeeCP({})-started failure", this.poolName, e);
                this.shutdownInternalThreads(false);//clear some internal member
                this.poolState = POOL_NEW;//reset state to new after failure
                throw new BeeDataSourcePoolStartedFailureException("Data source pool started failure", e);
            }
        } else {
            throw new BeeDataSourcePoolStartedFailureException("Data source pool is starting or already started up");
        }
    }

    private void startupInternal(final int poolWorkState, BeeDataSourceConfig config) throws SQLException {
        //step1: set log printer first
        this.logPrinter = getLogPrinter(FastConnectionPool.class, config.isPrintRuntimeLogs());

        //step2: set parameter configuration to pool local
        this.poolConfig = config;
        this.poolName = poolConfig.getPoolName();
        logPrinter.info("BeeCP({})-starting up....", this.poolName);

        //step3: set connection factory to pool local
        Object rawFactory = poolConfig.getConnectionFactory();
        if (rawFactory instanceof BeeXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (BeeXaConnectionFactory) rawFactory;
        } else {
            this.isRawXaConnFactory = false;
            this.rawConnFactory = (BeeConnectionFactory) rawFactory;
        }

        //step4: Create connection array and fill EMPTIED pooled connections
        this.connectionArrayInitialized = false;
        this.connectionArrayLen = poolConfig.getMaxActive();
        this.connectionArray = new PooledConnection[connectionArrayLen];
        this.connectionArrayInitLock = new InterruptableReentrantReadWriteLock();
        for (int i = 0; i < connectionArrayLen; i++)
            connectionArray[i] = new PooledConnection(this);

        //step5: Set Wait-time to pool local
        this.maxWaitMs = poolConfig.getMaxWait();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);
        PoolThreadThreadFactory poolThreadFactory = new PoolThreadThreadFactory(this.poolName);

        //step6: Create scheduled thread pool Executor,and schedule a timeout task here to interrupt possible blocks during creating
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2, poolThreadFactory);
        this.scheduledThreadPoolExecutor.setMaximumPoolSize(2);
        this.scheduledThreadPoolExecutor.setKeepAliveTime(10L, TimeUnit.SECONDS);
        this.scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);

        this.simplePoolMonitorVo = new FastConnectionPoolMonitorVo();
        this.timeoutConnectionsClearTaskFuture = this.scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                new ConnectionTimeoutTask(this), poolConfig.getIntervalOfClearTimeout(),
                poolConfig.getIntervalOfClearTimeout(), TimeUnit.MILLISECONDS);

        //step7: Create initial connections by synchronization mode(NOTE: this step maybe blocked during creation,so timeout task arranged before it)
        int initialSize = poolConfig.getInitialSize();
        if (initialSize > 0 && !poolConfig.isAsyncCreateInitConnections()) {
            try {
                createInitConnections(initialSize, true);
            } catch (SQLException e) {
                this.timeoutConnectionsClearTaskFuture.cancel(true);
                throw e;
            }
        }

        //step8: Create transfer policy
        String poolMode;
        if (poolConfig.isFairMode()) {
            poolMode = "fair";
            isFairMode = true;
            isCompeteMode = false;
            this.transferPolicy = new FairTransferPolicy();
        } else {
            poolMode = "compete";
            isFairMode = false;
            isCompeteMode = true;
            this.transferPolicy = this;
        }
        this.stateCodeOnRelease = this.transferPolicy.getStateCodeOnRelease();

        //step9: Copy some time configuration items to pool local
        this.idleTimeoutMs = poolConfig.getIdleTimeout();
        this.holdTimeoutMs = poolConfig.getHoldTimeout();
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.aliveAssumeTimeMs = poolConfig.getAliveAssumeTime();
        this.aliveTestTimeout = poolConfig.getAliveTestTimeout();
        this.parkTimeForRetryNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getParkTimeForRetry());
        this.methodLogTimeoutMs = poolConfig.getLogTimeout();

        //step10: Crete pool semaphore and thread local
        this.semaphoreSize = poolConfig.getSemaphoreSize();
        this.semaphore = new InterruptableSemaphore(this.semaphoreSize, isFairMode);
        this.useThreadLocal = poolConfig.isUseThreadLocal();
        if (useThreadLocal) this.threadLocal = new BorrowerThreadLocal();//create a new thread local

        //step11: Create queue and cache,thread execution pool
        if (POOL_STARTING == poolWorkState) {
            //Create queue and method log cache
            this.waitQueue = new ConcurrentLinkedQueue<>();//create one at start
            this.methodLogCache = new MethodExecutionLogCache();//create one at start

            //Set initial for servant thread
            this.servantTryCount = 0;//working control of servant thread
            this.servantState = THREAD_WORKING;//working control of servant thread
            this.setDaemon(true);
            this.setName("BeeCP(" + poolName + ")" + "-asyncAdd");
            this.start();

            this.exitHook = new ConnectionPoolHook(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);//JVM Hool register on start
        }

        //step12: initialize method execution log cache
        this.methodLogCache.init(
                poolName,
                poolConfig.getLogCacheSize(),
                poolConfig.getSlowConnectionThreshold(),
                poolConfig.getSlowSQLThreshold(),
                poolConfig.getLogListener());
        if (poolConfig.isEnableLogCache()) {
            this.conProxyFactory = new ProxyConnectionFactory4L(methodLogCache);
            this.collectMethodLogs = true;
        } else {
            this.conProxyFactory = new ProxyConnectionFactory();
            this.collectMethodLogs = false;
        }

        //step13: schedule two timed tasks to clear timeout objects(Connections and
        this.timeoutLogsClearTaskFuture = this.scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                new MethodLogTimeoutTask(this), poolConfig.getIntervalOfClearTimeoutLogs(),
                poolConfig.getIntervalOfClearTimeoutLogs(), TimeUnit.MILLISECONDS);

        //step14: create a thread to do pool initialization(create initial connections and fill them to array)
        if (initialSize > 0 && poolConfig.isAsyncCreateInitConnections())
            new PoolInitAsyncCreateThread(this, initialSize, "BeeCP(" + poolName + ")" + "-asyncInitialConnectionCreator").start();

        //step15: Register MXBean to Jmx server
        if (poolConfig.isRegisterMbeans()) this.registerMBeans(poolConfig);

        //step16:Print completion info at end
        String poolInitInfo;
        String driverClassNameOrFactoryName = poolConfig.getDriverClassName();
        if (isNotBlank(driverClassNameOrFactoryName)) {
            poolInitInfo = "BeeCP({})-has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}";
        } else {
            driverClassNameOrFactoryName = rawFactory.getClass().getName();
            poolInitInfo = "BeeCP({})-has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,factory:{}}";
        }
        logPrinter.info(poolInitInfo, poolName, poolMode, initialSize, connectionArrayLen, semaphoreSize, maxWaitMs, driverClassNameOrFactoryName);
    }

    //***************************************************************************************************************//
    //                                         2: Pooled Connection Creation(0+3)                                    //
    //***************************************************************************************************************//
    //Method call to create initial connections during pool starting and pool restarting
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
                        connectionArray[i].onRemove(DESC_RM_POOL_START);
                    throw e;
                } else {//print log under async mode
                    logPrinter.warn("Failed to create initial connections by async mode", e);
                }
            } finally {
                if (isWriteLocked) writeLock.unlock();
            }
        }
    }

    //Create connection and fill it to target pooled connection(wrapper)
    private PooledConnection fillRawConnection(PooledConnection p, int state, Thread creatingThread) throws SQLException {
        //1: print info of creation starting
        logPrinter.info("BeeCP({})-start to create a connection", this.poolName);

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
                    throw new XaConnectionCreatedException("A unknown error occurred when created an XA connection");
                }
                rawConn = rawXaConn.getConnection();
                rawXaRes = rawXaConn.getXAResource();
            } else {
                rawConn = this.rawConnFactory.create();
                if (rawConn == null) {
                    if (creatingThread.isInterrupted() && Thread.interrupted())
                        throw new ConnectionGetInterruptedException("An interruption occurred when created a connection");
                    throw new ConnectionCreatedException("A unknown error occurred when created a connection");
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
            logPrinter.info("BeeCP({})-created a new connection:{} to fill pooled connection:{}", this.poolName, rawConn, p);

            //5: return result
            return p;
        } catch (Throwable e) {
            p.state = CON_CLOSED;//reset to closed state
            if (rawConn != null) oclose(rawConn);
            else if (rawXaConn != null) oclose(rawXaConn);
            throw e instanceof SQLException ? (SQLException) e : new ConnectionCreatedException(e);
        } finally {
            p.creatingInfo = null;//clear filling of pooled connection
        }
    }

    //Read Default from first created connection to initialize connection array
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
        boolean enableDefaultCatalog = poolConfig.isEnableDefaultCatalog();
        if (enableDefaultCatalog) {
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
                logPrinter.warn("BeeCP({})-driver not support 'isValid' method call on connection", this.poolName);
            }
        } catch (Throwable e) {
            supportIsValid = false;
            logPrinter.warn("BeeCP({})-exception occurred when call 'isValid' method on initial test connection", this.poolName, e);
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
                logPrinter.warn("BeeCP({})-driver not support 'getNetworkTimeout()/setNetworkTimeout(time)' method call on connection", this.poolName);
            } else {//driver support networkTimeout
                int threadSize = Math.min(connectionArrayLen, NCPU);
                this.networkTimeoutExecutor = new ThreadPoolExecutor(threadSize, threadSize, 10L, SECONDS,
                        new LinkedBlockingQueue<Runnable>(connectionArrayLen), this.scheduledThreadPoolExecutor.getThreadFactory());//When code reach here,pool scheduledThreadPoolExecutor is created absolutely.
                this.networkTimeoutExecutor.allowCoreThreadTimeOut(true);
                firstConn.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            }
        } catch (Throwable e) {
            supportNetworkTimeoutInd = false;
            logPrinter.warn("BeeCP({})-exception occurred when call 'getNetworkTimeout()/setNetworkTimeout(time)' method on initial test connection", this.poolName, e);
            if (networkTimeoutExecutor != null) {
                networkTimeoutExecutor.shutdownNow();
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
                    enableDefaultCatalog,
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
    //                                         3: Pooled Connections Get(2+4)                                        //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.collectMethodLogs) {
            BeeMethodLog log = methodLogCache.beforeCall(Type_Pool_Log, "FastConnectionPool.getConnection()", null, null, null);
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

    public XAConnection getXAConnection() throws SQLException {
        if (this.collectMethodLogs) {
            BeeMethodLog log = methodLogCache.beforeCall(Type_Pool_Log, "FastConnectionPool.getXAConnection()", null, null, null);
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

    //******* Core method for get *****
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY)
            throw new BeeDataSourcePoolNotReadyException("Pool has been closed or is restarting");

        //1: try to reuse last used connection
        Borrower b = null;
        PooledConnection p;
        if (this.useThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null) {
                    int state = p.state;
                    if (state == CON_IDLE) {
                        if (ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED)) {
                            if (this.testOnBorrow(p)) return p;
                        } else if (p.state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                            return this.fillRawConnection(p, CON_BORROWED, b.thread);
                        }
                    } else if (state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                        return this.fillRawConnection(p, CON_BORROWED, b.thread);
                    }
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

    //Check borrowed connection is whether alive
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime - this.aliveAssumeTimeMs >= 0L && !this.conValidTest.isAlive(p)) {
            p.onRemove(DESC_RM_CON_BAD);
            this.tryWakeupServantThread();
            return false;
        } else {
            return true;
        }
    }

    //Attempt to search an idle connection or create one when size of connection not reach maxnumber
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

    //handle timeout or interruption
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

    //***************************************************************************************************************//
    //                                         4: Pooled Connections recycle(0+4)                                    //
    //***************************************************************************************************************//
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

    private void transferException(Throwable e) {
        for (Borrower b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    void abort(PooledConnection p, String reason) {
        p.onRemove(reason);
        this.tryWakeupServantThread();
    }

    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount;
            if (c >= this.connectionArrayLen) return;
        } while (!ServantTryCountUpd.compareAndSet(this, c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    //***************************************************************************************************************//
    //                                         5: Pool Restart(2+2)                                                  //
    //***************************************************************************************************************//
    public void restart(boolean forceRecycleBorrowed) throws SQLException {
        restart(forceRecycleBorrowed, false, null);
    }

    public void restart(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        restart(forceRecycleBorrowed, true, config);
    }

    //NOTE: If pool restarts failed with a new configuration,maybe its properties has been dirty during starting,So HOLD ITS STATE TO restart again
    private void restart(boolean forceRecycleBorrowed, boolean reinit, BeeDataSourceConfig config) throws SQLException {
        if (reinit && config == null)
            throw new BeeDataSourceConfigException("Data source configuration can't be null");

        int poolState = this.poolState;
        boolean hasRunToStartupInternal = false;
        if ((poolState == POOL_READY || poolState == POOL_RESTART_FAILED) && PoolStateUpd.compareAndSet(this, poolState, POOL_RESTARTING)) {
            try {
                BeeDataSourceConfig checkedConfig = null;
                if (reinit) {
                    checkedConfig = config.check();
                    this.shutdownInternalThreads(false);
                }

                logPrinter.info("BeeCP({})-begin to remove all connections", this.poolName);
                this.removeAllConnections(forceRecycleBorrowed, DESC_RM_POOL_RESTART);
                logPrinter.info("BeeCP({})-completed to remove all connections", this.poolName);

                if (reinit) {
                    logPrinter.info("BeeCP({})-begin to restart pool", this.poolName);

                    //3: Rerun pool
                    hasRunToStartupInternal = true;
                    startupInternal(POOL_RESTARTING, checkedConfig);//throws SQLException only fail to create initial connections or fail to set default

                    //note: if failed,this method may be recalled with correct configuration
                    logPrinter.info("BeeCP({})-restart pool successful", this.poolName);
                }
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                logPrinter.error("BeeCP({})-restarted failure", this.poolName, e);
                if (hasRunToStartupInternal) {
                    this.poolState = POOL_RESTART_FAILED;
                    this.shutdownInternalThreads(false);//clear some internal member
                } else {
                    this.poolState = POOL_READY;
                }
                if (e instanceof BeeDataSourcePoolException) {
                    throw (BeeDataSourcePoolException) e;
                } else {
                    throw new BeeDataSourcePoolRestartedFailureException("Data source pool restarted failure", e);
                }
            }
        } else {
            throw new BeeDataSourcePoolRestartedFailureException("Pool has been closed or is restarting");
        }
    }

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

        if (logPrinter.isEnableLogOutput()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            logPrinter.info("BeeCP({})-after clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                         6: Pool Suspend(2+0)                                                  //
    //***************************************************************************************************************//
    public boolean suspendPool() {
        return PoolStateUpd.compareAndSet(this, POOL_READY, POOL_SUSPENDED);
    }

    public boolean resumePool() {
        return PoolStateUpd.compareAndSet(this, POOL_SUSPENDED, POOL_READY);
    }

    //***************************************************************************************************************//
    //                                         7: Pool Close(3+1)                                                    //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    public String toString() {
        return getPoolStateDesc(this.poolState);
    }

    public void close() {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_RESTARTING) {
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                logPrinter.info("BeeCP({})-begin to shutdown pool", this.poolName);

                //1: Shutdown all internal threads(include thread pools)
                this.shutdownInternalThreads(true);

                //2: Close all pooled connections and remove them(*** important step ***)
                this.removeAllConnections(this.poolConfig.isForceRecycleBorrowedOnClose(), DESC_RM_POOL_SHUTDOWN);

                //3: set pool state to closed
                this.poolState = POOL_CLOSED;
                logPrinter.info("BeeCP({})-has shutdown pool", this.poolName);
                break;
            } else {//Compare set fail,then exit loop when pool's state is closed(POOL_CLOSING)
                break;
            }
        } while (true);
    }

    private void shutdownInternalThreads(boolean isCloseCall) {
        logPrinter.info("BeeCP({})-begin to shutdown pool internal threads", this.poolName);
        //1: Clear networkTimeoutExecutor
        if (this.networkTimeoutExecutor != null) {
            this.networkTimeoutExecutor.shutdownNow();
            this.networkTimeoutExecutor.getQueue().clear();
            this.networkTimeoutExecutor = null;
        }

        //2: Clear scheduledThreadPoolExecutor
        if (scheduledThreadPoolExecutor != null) {
            if (this.timeoutLogsClearTaskFuture != null) {
                this.timeoutLogsClearTaskFuture.cancel(true);
                this.timeoutLogsClearTaskFuture = null;
            }
            if (this.timeoutConnectionsClearTaskFuture != null) {
                this.timeoutConnectionsClearTaskFuture.cancel(true);
                this.timeoutConnectionsClearTaskFuture = null;
            }
            scheduledThreadPoolExecutor.shutdownNow();
            scheduledThreadPoolExecutor.getQueue().clear();
            scheduledThreadPoolExecutor = null;
        }

        //3: unregister Pool hook
        if (this.exitHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.exitHook);
            } catch (Throwable e) {
                //do nothing
            } finally {
                this.exitHook = null;
            }
        }

        if (isCloseCall) {
            //4: Shut down servant thread
            int curState = this.servantState;
            this.servantState = THREAD_EXIT; //set exit state
            if (curState == THREAD_WAITING) LockSupport.unpark(this);//signal the servant thread to exit
        }

        //5: Clear thread local
        this.threadLocal = null;
        //6: Clear wait queue and log cache
        if (this.waitQueue != null) this.waitQueue.clear();//clear for gc
        if (this.methodLogCache != null) this.methodLogCache.clear(Type_All);
        //7: Unregister MBeans
        if (this.poolNameOfRegisteredMBean != null) this.unregisterMBeans();
        logPrinter.info("BeeCP({})-completed to shutdown pool internal threads", this.poolName);
    }

    //***************************************************************************************************************//
    //                                  8: Pool method execution logs(5+1)                                           //
    //***************************************************************************************************************//
    public void changeLogListener(BeeMethodLogListener listener) {
        methodLogCache.setMethodExecutionListener(listener);
    }

    public List<BeeMethodLog> getLogs(int type) {
        return this.methodLogCache.getLog(type);
    }

    public void clearLogs(int type) {
        methodLogCache.clear(type);
    }

    public synchronized void enableLogCache(boolean enable) {
        if (enable != this.collectMethodLogs) {
            if (enable) {//enable
                this.conProxyFactory = new ProxyConnectionFactory4L(methodLogCache);
                this.collectMethodLogs = true;
                this.timeoutLogsClearTaskFuture = this.scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                        new MethodLogTimeoutTask(this), poolConfig.getIntervalOfClearTimeoutLogs(),
                        poolConfig.getIntervalOfClearTimeoutLogs(), TimeUnit.MILLISECONDS);
            } else {//disable
                this.conProxyFactory = new ProxyConnectionFactory();
                this.collectMethodLogs = false;
                this.methodLogCache.clear(Type_All);
                if (this.timeoutLogsClearTaskFuture != null) {
                    this.timeoutLogsClearTaskFuture.cancel(true);
                    this.timeoutLogsClearTaskFuture = null;
                }
            }
        }
    }

    public boolean cancelStatement(String id) throws SQLException {
        return methodLogCache.cancelStatement(id);
    }

    //***************************************************************************************************************//
    //                                  9: MBean Registration(0+2)                                                   //
    //***************************************************************************************************************//
    private void registerMBeans(BeeDataSourceConfig poolConfig) {
        String configMBeanName = String.format("org.stone.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName);
        try {
            BeanUtil.registerMBean(configMBeanName, poolConfig);
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})-failed to register a MBean with name:{}", this.poolName, configMBeanName, e);
        }

        String poolMBeanName = String.format("org.stone.beecp.pool.FastConnectionPool:type=BeeCP(%s)", this.poolName);
        try {
            BeanUtil.registerMBean(poolMBeanName, this);
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})-failed to register a MBean with name:{}", this.poolName, poolMBeanName, e);
        }
        this.poolNameOfRegisteredMBean = this.poolName;
    }

    private void unregisterMBeans() {
        String configMBeanName = String.format("org.stone.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolNameOfRegisteredMBean);
        try {
            BeanUtil.unregisterMBean(configMBeanName);
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})-failed to unregister a MBean with name:{}", this.poolNameOfRegisteredMBean, configMBeanName, e);
        }

        String poolMBeanName = String.format("org.stone.beecp.pool.FastConnectionPool:type=BeeCP(%s)", this.poolNameOfRegisteredMBean);
        try {
            BeanUtil.unregisterMBean(poolMBeanName);
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})-failed to unregister a MBean with name:{}", this.poolNameOfRegisteredMBean, poolMBeanName, e);
        }

        this.poolNameOfRegisteredMBean = null;
    }


    //***************************************************************************************************************//
    //                                  10: other methods(3+0)                                                       //
    //***************************************************************************************************************//
    public synchronized void enableLogPrinter(boolean enable) {
        this.logPrinter = getLogPrinter(FastConnectionPool.class, enable);
    }

    public List<Thread> interruptWaitingThreads() {
        List<Thread> threads = new LinkedList<>();
        //1: clear waiting thread on semaphore
        if (this.semaphore != null) threads.addAll(this.semaphore.interruptQueuedWaitThreads());

        //2: transfer exception to waiter in queue
        if (this.waitQueue != null && !this.waitQueue.isEmpty()) {
            BeeDataSourcePoolRestartedFailureException exception = new BeeDataSourcePoolRestartedFailureException("Pool has been closed or is restarting");
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

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        return getPoolMonitorVo(false);
    }

    private BeeConnectionPoolMonitorVo getPoolMonitorVo(boolean simple) {
        if (simple) {
            int idleSize = 0, borrowedSize = 0;
            for (PooledConnection p : connectionArray) {
                if (p != null) {
                    int state = p.state;
                    if (state == CON_IDLE) idleSize++;
                    else if (state == CON_BORROWED) borrowedSize++;
                }
            }

            int semaphoreRemainSize = 0;
            int semaphoreWaitingSize = 0;
            if (semaphore != null) {
                semaphoreRemainSize = this.semaphore.availablePermits();
                semaphoreWaitingSize = this.semaphore.getQueueLength();
            }
            this.simplePoolMonitorVo.fillSimple(idleSize, borrowedSize, semaphoreRemainSize, semaphoreWaitingSize);
            return simplePoolMonitorVo;
        } else {
            int idleSize = 0, borrowedSize = 0;
            int creatingCount = 0, creatingTimeoutCount = 0;
            if (connectionArray != null) {
                long currentTimeMillis = System.currentTimeMillis();
                for (PooledConnection p : connectionArray) {
                    if (p != null) {
                        int state = p.state;
                        if (state == CON_IDLE) idleSize++;
                        else if (state == CON_BORROWED) borrowedSize++;

                        ConnectionCreatingInfo creatingInfo = p.creatingInfo;
                        if (creatingInfo != null) {
                            creatingCount++;
                            if (currentTimeMillis - creatingInfo.creatingStartTime - maxWaitMs >= 0L)
                                creatingTimeoutCount++;
                        }
                    }
                }
            }

            int semaphoreRemainSize = 0;
            int semaphoreWaitingSize = 0;
            int transferWaitingSize = 0;
            if (semaphore != null) {
                semaphoreRemainSize = this.semaphore.availablePermits();
                semaphoreWaitingSize = this.semaphore.getQueueLength();
            }

            if (waitQueue != null) {
                for (Borrower borrower : this.waitQueue)
                    if (borrower.state == null) transferWaitingSize++;
            }

            return new FastConnectionPoolMonitorVo(
                    this.poolName,
                    this.isFairMode,
                    this.connectionArrayLen,
                    this.semaphoreSize,
                    this.useThreadLocal,
                    this.poolState,
                    idleSize,
                    borrowedSize,
                    semaphoreRemainSize,
                    semaphoreWaitingSize,
                    transferWaitingSize,
                    creatingCount,
                    creatingTimeoutCount,
                    this.logPrinter.isEnableLogOutput(),
                    this.collectMethodLogs);

        }
    }

    //***************************************************************************************************************//
    //                                  11: Override methods - completion transfer policy(2+0)                       //
    //***************************************************************************************************************//
    public int getStateCodeOnRelease() {
        return CON_IDLE;
    }

    public boolean tryCatch(PooledConnection p) {
        return p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED);
    }

    //***************************************************************************************************************//
    //                                  12: Override method - Alive test(1+0)                                        //
    //***************************************************************************************************************//
    public boolean isAlive(final PooledConnection p) {
        try {
            if (p.rawConn.isValid(this.aliveTestTimeout)) {
                p.lastAccessTime = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            logPrinter.warn("BeeCP({})-alive test failed on a borrowed connection", this.poolName, e);
        }
        return false;
    }

    //***************************************************************************************************************//
    //                                  13: Override method - servant thread(1+0)                                    //
    //***************************************************************************************************************//
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

    //***************************************************************************************************************//
    //                                  14: Help methods - timeout task call(0+1)                                    //
    //***************************************************************************************************************//
    void closeIdleTimeoutConnections() {
        //step1:print pool info before clean
        if (logPrinter.isEnableLogOutput()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo(true);
            logPrinter.info("BeeCP({})-before timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: clean timeout connection in a loop
        for (PooledConnection p : this.connectionArray) {
            if (p == null) continue;
            final int state = p.state;
            if (state == CON_CREATING) {
                ConnectionCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null && System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L) {
                    creatingInfo.creatingThread.interrupt();
                }
            } else if (state == CON_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                if (isTimeoutInIdle && ConStUpd.compareAndSet(p, state, CON_CLOSED)) {//need close idle
                    p.onRemove(DESC_RM_CON_IDLE);
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
        if (logPrinter.isEnableLogOutput()) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo(true);
            logPrinter.info("BeeCP({})-after timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                  15: Internal classes(0+8)                                                    //
    //***************************************************************************************************************//
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

    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<>(new Borrower(Thread.currentThread()));
        }
    }

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

    private static final class ConnectionTimeoutTask implements Runnable {
        private final FastConnectionPool pool;

        ConnectionTimeoutTask(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                pool.closeIdleTimeoutConnections();
            } catch (Throwable e) {
                pool.logPrinter.warn("BeeCP({})-an exception occurred while scanning idle-timeout connections", this.pool.poolName, e);
            }
        }
    }

    private static final class MethodLogTimeoutTask implements Runnable {
        private final FastConnectionPool pool;

        MethodLogTimeoutTask(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                pool.methodLogCache.clearTimeout(pool.methodLogTimeoutMs);
            } catch (Throwable e) {
                pool.logPrinter.warn("BeeCP({})-an exception occurred while scanning timeout method logs", this.pool.poolName, e);
            }
        }
    }

    private static class ConnectionPoolHook extends Thread {
        private final FastConnectionPool pool;

        ConnectionPoolHook(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            pool.logPrinter.info("BeeCP({})-detect Jvm exit,pool will be shutdown", this.pool.poolName);
            try {
                this.pool.close();
            } catch (Throwable e) {
                pool.logPrinter.error("BeeCP({})-an exception occurred when shutdown pool", this.pool.poolName, e);
            }
        }
    }

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
                        pool.logPrinter.warn("BeeCP({})-failed to set query timeout value on statement of a borrowed connection", poolName, e);
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
                pool.logPrinter.warn("BeeCP({})-connection alive test failed with sql,pool will abandon it", poolName, e);
            } finally {
                if (st != null) oclose(st);
                if (changed) {
                    try {
                        rawConn.setAutoCommit(true);
                    } catch (Throwable e) {
                        pool.logPrinter.warn("BeeCP({})-failed to reset 'auto-commit' to false after alive test,pool will abandon test connection", poolName, e);
                        checkPassed = false;
                    }
                }
            }

            return checkPassed;
        }
    }
}
