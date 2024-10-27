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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.*;
import org.stone.beecp.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;
import org.stone.tools.extension.InterruptionSemaphore;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.*;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements BeeConnectionPool, FastConnectionPoolMBean, PooledConnectionAliveTest, PooledConnectionTransferPolicy {
    static final Logger Log = LoggerFactory.getLogger(FastConnectionPool.class);
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");

    String poolName;
    volatile int poolState;
    PooledConnection[] connectionArray;//fixed len
    AtomicInteger servantState;
    AtomicInteger idleScanState;
    ConcurrentLinkedQueue<Borrower> waitQueue;
    BeeDataSourceConfig poolConfig;

    private String poolMode;
    private String poolHostIP;
    private long poolThreadId;
    private String poolThreadName;
    private boolean isFairMode;
    private boolean isCompeteMode;
    private int semaphoreSize;
    private InterruptionSemaphore semaphore;
    private long maxWaitNs;//nanoseconds
    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds
    private boolean supportHoldTimeout;
    private long aliveAssumeTimeMs;//milliseconds
    private int aliveTestTimeout;//seconds
    private long delayTimeForNextClearNs;//nanoseconds
    private int stateCodeOnRelease;
    private int connectionArrayLen;
    private boolean connectionArrayInitialized;
    private InterruptionReentrantReadWriteLock connectionArrayInitLock;
    private PooledConnectionTransferPolicy transferPolicy;
    private boolean isRawXaConnFactory;
    private BeeConnectionFactory rawConnFactory;
    private BeeXaConnectionFactory rawXaConnFactory;
    private PooledConnectionAliveTest conValidTest;
    private ThreadPoolExecutor networkTimeoutExecutor;
    private AtomicInteger servantTryCount;
    private IdleTimeoutScanThread idleScanThread;
    private boolean enableThreadLocal;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private FastConnectionPoolMonitorVo monitorVo;
    private ConnectionPoolHook exitHook;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //               1: Pool initializes and maintenance on pooled connections(8)                                    //                                                                                  //
    //***************************************************************************************************************//
    //Method-1.1: pool initializes.
    public void init(BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new PoolInitializeFailedException("Pool initialization configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {//initializes after cas success to change pool state
            try {
                checkJdbcProxyClass();
                this.poolConfig = config.check();
                startup(POOL_STARTING);
                this.poolState = POOL_READY;//ready to accept coming requests(love u,my pool)
            } catch (Throwable e) {
                Log.info("BeeCP({})initialized failed", this.poolName, e);
                this.poolState = POOL_NEW;//reset state to new after failure
                throw e instanceof SQLException ? (SQLException) e : new PoolInitializeFailedException(e);
            }
        } else {
            throw new PoolInitializeFailedException("Pool has already initialized or in initializing");
        }
    }

    // Method-1.2: launch the pool
    private void startup(final int poolWorkState) throws SQLException {
        this.poolName = poolConfig.getPoolName();
        Log.info("BeeCP({})starting up....", this.poolName);

        //step1: set connection to pool local
        Object rawFactory = poolConfig.getConnectionFactory();
        if (rawFactory instanceof BeeXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (BeeXaConnectionFactory) rawFactory;
        } else {
            this.rawConnFactory = (BeeConnectionFactory) rawFactory;
        }

        //step2: create a connection array with fixed len
        this.connectionArrayInitialized = false;
        this.connectionArrayLen = poolConfig.getMaxActive();
        this.connectionArray = new PooledConnection[connectionArrayLen];
        this.connectionArrayInitLock = new InterruptionReentrantReadWriteLock();
        for (int i = 0; i < connectionArrayLen; i++)
            connectionArray[i] = new PooledConnection(this, i);

        //step3: creates initial connections
        this.printRuntimeLog = poolConfig.isPrintRuntimeLog();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getMaxWait());
        int initialSize = poolConfig.getInitialSize();
        if (initialSize > 0 && !poolConfig.isAsyncCreateInitConnection())
            createInitConnections(poolConfig.getInitialSize(), true);

        //step4: creates a transfer to transfer released connections to waiters
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

        //step5: copy some number type items to pool local
        this.idleTimeoutMs = poolConfig.getIdleTimeout();
        this.holdTimeoutMs = poolConfig.getHoldTimeout();
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.aliveAssumeTimeMs = poolConfig.getAliveAssumeTime();
        this.aliveTestTimeout = poolConfig.getAliveTestTimeout();
        this.delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
        this.semaphoreSize = poolConfig.getBorrowSemaphoreSize();

        //step6: creates pool semaphore and pool threadLocal
        this.enableThreadLocal = poolConfig.isEnableThreadLocal();
        this.semaphore = new InterruptionSemaphore(this.semaphoreSize, isFairMode);
        if (this.threadLocal != null) this.threadLocal = null;//help gc when reinitialize
        if (enableThreadLocal) this.threadLocal = new BorrowerThreadLocal();//as a cache to store one used connection

        //step7: creates wait queue,scan thread and others
        if (POOL_STARTING == poolWorkState) {
            this.waitQueue = new ConcurrentLinkedQueue<>();
            //count of retry to search idles or create news in servant thread of pool
            this.servantTryCount = new AtomicInteger(0);
            this.servantState = new AtomicInteger(THREAD_WORKING);

            this.idleScanState = new AtomicInteger(THREAD_WORKING);
            this.idleScanThread = new IdleTimeoutScanThread(this);

            this.monitorVo = this.createPoolMonitorVo();//pool monitor object
            this.exitHook = new ConnectionPoolHook(this);//a hook works when JVM exit
            Runtime.getRuntime().addShutdownHook(this.exitHook);
            this.registerJmx();

            setDaemon(true);
            setName("BeeCP(" + poolName + ")" + "-asyncAdd");
            start();

            this.idleScanThread.setDaemon(true);
            this.idleScanThread.setPriority(3);
            this.idleScanThread.setName("BeeCP(" + poolName + ")" + "-idleScanner");
            this.idleScanThread.start();
        }

        //step8: creates initial connections with an async thread
        if (initialSize > 0 && poolConfig.isAsyncCreateInitConnection())
            new PoolInitAsyncCreateThread(this).start();

        //step9: print pool info after completion of pool initialization
        String poolInitInfo;
        String driverClassNameOrFactoryName;
        if (isNotBlank(poolConfig.getDriverClassName())) {
            driverClassNameOrFactoryName = poolConfig.getDriverClassName();
            poolInitInfo = "BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}";
        } else {
            driverClassNameOrFactoryName = rawFactory.getClass().getName();
            poolInitInfo = "BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,factory:{}}";
        }
        Log.info(poolInitInfo, poolName, poolMode, initialSize, connectionArrayLen, semaphoreSize, poolConfig.getMaxWait(), driverClassNameOrFactoryName);
    }

    //Method-1.3: creates initial connections
    void createInitConnections(int initSize, boolean syn) throws SQLException {
        ReentrantReadWriteLock.WriteLock writeLock = connectionArrayInitLock.writeLock();
        boolean isWriteLocked = !syn && !connectionArrayInitialized && !connectionArrayInitLock.isWriteLocked() && writeLock.tryLock();

        if (syn || isWriteLocked) {
            int index = 0;
            try {
                while (index < initSize) {
                    PooledConnection p = connectionArray[index];
                    p.state = CON_CREATING;
                    this.fillRawConnection(p, CON_IDLE);
                    index++;
                }
            } catch (SQLException e) {
                if (syn) {
                    for (int i = 0; i < index; i++)
                        this.removePooledConn(connectionArray[i], DESC_RM_INIT);
                    throw e;
                } else {//print log under async mode
                    Log.warn("Failed to create initial connections by async mode", e);
                }
            } finally {
                if (isWriteLocked) writeLock.unlock();
            }
        }
    }

    //Method-1.4: Search one or create one
    private PooledConnection searchOrCreate() throws SQLException {
        //1: do initialization on connection array
        if (!connectionArrayInitialized) {
            ReentrantReadWriteLock.ReadLock readLock = connectionArrayInitLock.readLock();
            ReentrantReadWriteLock.WriteLock writeLock = connectionArrayInitLock.writeLock();
            try {
                if (!connectionArrayInitLock.isWriteLocked() && writeLock.tryLock()) {
                    try {
                        PooledConnection p = connectionArray[0];
                        p.state = CON_CREATING;
                        return this.fillRawConnection(p, CON_USING);
                    } finally {
                        writeLock.unlock();
                    }
                } else if (readLock.tryLock(this.maxWaitNs, TimeUnit.NANOSECONDS)) {
                    readLock.unlock();
                    if (!connectionArrayInitialized)
                        throw new ConnectionCreateException("Pool initialized failed on first created connection or failed to create it");
                } else {
                    throw new ConnectionCreateException("Waited timeout on pool lock");
                }
            } catch (InterruptedException e) {
                throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool lock");
            }
        }

        //2: search one or create one
        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_IDLE) {
                if (ConStUpd.compareAndSet(p, CON_IDLE, CON_USING)) {
                    if (this.testOnBorrow(p)) return p;
                } else if (p.state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                    return this.fillRawConnection(p, CON_USING);
                }
            } else if (state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                return this.fillRawConnection(p, CON_USING);
            }
        }
        return null;
    }

    //Method-1.5: creates a pooled connection and fill it into given pooled connection
    private PooledConnection fillRawConnection(PooledConnection p, int state) throws SQLException {
        //1: print runtime log of connection creation
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))begin to create a raw connection", this.poolName);

        //2: create a connection by factory
        Connection rawConn = null;
        XAConnection rawXaConn = null;
        XAResource rawXaRes = null;
        p.creatingInfo = new ConnectionCreatingInfo();

        try {
            if (this.isRawXaConnFactory) {
                //maybe blocked in factory method,if true,call{@code BeeDataSource.interruptThreadsOnCreationLock()} to interrupt blocking
                rawXaConn = this.rawXaConnFactory.create();
                if (rawXaConn == null) {
                    if (Thread.interrupted())
                        throw new ConnectionGetInterruptedException("An interruption occurred when created an XA connection");
                    throw new ConnectionCreateException("A unknown error occurred when created an XA connection");
                }
                rawConn = rawXaConn.getConnection();
                rawXaRes = rawXaConn.getXAResource();
            } else {
                rawConn = this.rawConnFactory.create();
                if (rawConn == null) {
                    if (Thread.interrupted())
                        throw new ConnectionGetInterruptedException("An interruption occurred when created a connection");
                    throw new ConnectionCreateException("A unknown error occurred when created a connection");
                }
            }

            //3: initialize pooled connection array
            if (connectionArrayInitialized) {
                p.setRawConnection(state, rawConn, rawXaRes);
            } else {
                this.initPooledConnectionArray(rawConn);
                p.setRawConnection2(state, rawConn, rawXaRes);
                connectionArrayInitialized = true;
            }

            //4: print runtime log of creation
            if (this.printRuntimeLog)
                Log.info("BeeCP({}))created a new connection:{} to fill pooled connection:{}", this.poolName, rawConn, p);

            //5: return result
            return p;
        } catch (Throwable e) {
            p.creatingInfo = null;
            p.state = CON_CLOSED;//reset to closed state
            if (rawConn != null) oclose(rawConn);
            else if (rawXaConn != null) oclose(rawXaConn);
            throw e instanceof SQLException ? (SQLException) e : new ConnectionCreateException(e);
        }
    }

    //Method-1.6: remove a pooled connection under lock(logical removal)
    private void removePooledConn(PooledConnection p, String cause) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))begin to remove a pooled connection:{} for cause:{}", this.poolName, p, cause);
        p.onRemove();
    }

    //Method-1.8: create a pooled connection array with first connection
    private void initPooledConnectionArray(Connection firstConn) throws SQLException {
        //step1:set default value of property auto-commit on first connection
        Boolean defaultAutoCommit = poolConfig.isDefaultAutoCommit();
        boolean isEnableDefaultOnAutoCommit = poolConfig.isEnableDefaultOnAutoCommit();
        if (isEnableDefaultOnAutoCommit) {
            if (defaultAutoCommit == null) {
                try {
                    defaultAutoCommit = firstConn.getAutoCommit();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to get value of auto-commit property from first connection object", this.poolName, e);
                }
            }
            if (defaultAutoCommit == null) {
                defaultAutoCommit = Boolean.TRUE;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})assign {} as default value of auto-commit property for connections", this.poolName, true);
            }
            try {
                firstConn.setAutoCommit(defaultAutoCommit);//setting test with default value
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnAutoCommit(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of auto-commit property on first connection object", this.poolName, defaultAutoCommit, e);
            }
        } else if (defaultAutoCommit == null) {
            defaultAutoCommit = Boolean.FALSE;
        }

        //step2:set default value of property transaction-isolation
        Integer defaultTransactionIsolation = poolConfig.getDefaultTransactionIsolationCode();
        boolean isEnableDefaultOnTransactionIsolation = poolConfig.isEnableDefaultOnTransactionIsolation();
        if (isEnableDefaultOnTransactionIsolation) {
            if (defaultTransactionIsolation == null) {
                try {
                    defaultTransactionIsolation = firstConn.getTransactionIsolation();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to get value of transaction-isolation property from first connection object", this.poolName, e);
                }
            }
            if (defaultTransactionIsolation == null) {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})assign {} as default value of transaction-isolation property for connections", this.poolName, defaultTransactionIsolation);
            }
            try {
                firstConn.setTransactionIsolation(defaultTransactionIsolation);//default setting test
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnTransactionIsolation(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of transaction-isolation property on first connection object", this.poolName, defaultTransactionIsolation, e);
            }
        } else if (defaultTransactionIsolation == null) {
            defaultTransactionIsolation = Integer.valueOf(0);
        }

        //step3:get default value of property read-only from config or from first connection
        Boolean defaultReadOnly = poolConfig.isDefaultReadOnly();
        boolean isEnableDefaultOnReadOnly = poolConfig.isEnableDefaultOnReadOnly();
        if (poolConfig.isEnableDefaultOnReadOnly()) {
            if (defaultReadOnly == null) {
                try {
                    defaultReadOnly = Boolean.valueOf(firstConn.isReadOnly());
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to get value of read-only property from first connection object", this.poolName);
                }
            }
            if (defaultReadOnly == null) {
                defaultReadOnly = Boolean.FALSE;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})assign {} as default value of read-only property for connections", this.poolName, false);
            }
            try {
                firstConn.setReadOnly(defaultReadOnly);//default setting test
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnTransactionIsolation(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of read-only property on first connection object", this.poolName, defaultTransactionIsolation, e);
            }
        } else if (defaultReadOnly == null) {
            defaultReadOnly = Boolean.FALSE;
        }

        //step4:get default value of property catalog from config or from first connection
        String defaultCatalog = poolConfig.getDefaultCatalog();
        boolean isEnableDefaultOnCatalog = poolConfig.isEnableDefaultOnCatalog();
        if (isEnableDefaultOnCatalog) {
            if (isBlank(defaultCatalog)) {
                try {
                    defaultCatalog = firstConn.getCatalog();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to get value of catalog property from first connection object", this.poolName, e);
                }
            }
            if (isNotBlank(defaultCatalog)) {
                try {
                    firstConn.setCatalog(defaultCatalog);//default setting test
                } catch (Throwable e) {
                    poolConfig.setEnableDefaultOnCatalog(false);
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to set default value({}) of catalog property on first connection object", this.poolName, defaultCatalog, e);
                }
            }
        }

        //step5:get default value of property schema from config or from first connection
        String defaultSchema = poolConfig.getDefaultSchema();
        boolean isEnableDefaultOnSchema = poolConfig.isEnableDefaultOnSchema();
        if (isEnableDefaultOnSchema) {
            if (isBlank(defaultSchema)) {
                try {
                    defaultSchema = firstConn.getSchema();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to get value of schema property from first connection object", this.poolName, e);
                }
            }
            if (isNotBlank(defaultSchema)) {
                try {
                    firstConn.setSchema(defaultSchema);//default setting test
                } catch (Throwable e) {
                    poolConfig.setEnableDefaultOnSchema(false);
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to set default value({}) of schema property on first connection object", this.poolName, defaultSchema, e);
                }
            }
        }

        //step6: check driver whether support 'isAlive' method
        boolean supportIsValid = true;//assume support
        try {
            if (firstConn.isValid(this.aliveTestTimeout)) {
                conValidTest = this;
            } else {
                supportIsValid = false;
                if (this.printRuntimeLog) {
                    Log.warn("BeeCP({})get false from call of isValid method on first connection object", this.poolName);
                }
            }
        } catch (Throwable e) {
            supportIsValid = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})isValid method tested failed on first connection object", this.poolName, e);
        }

        //step7:test driver whether support sql query timeout
        if (!supportIsValid) {
            String conTestSql = this.poolConfig.getAliveTestSql();
            boolean supportQueryTimeout = validateTestSql(poolName, firstConn, conTestSql, aliveTestTimeout, defaultAutoCommit);//check test sql
            conValidTest = new PooledConnectionAliveTestBySql(poolName, conTestSql, aliveTestTimeout, defaultAutoCommit, supportQueryTimeout, printRuntimeLog);
        }

        //step8: check driver whether support networkTimeout
        int defaultNetworkTimeout = 0;
        boolean supportNetworkTimeoutInd = true;//assume supportable
        try {
            defaultNetworkTimeout = firstConn.getNetworkTimeout();
            if (defaultNetworkTimeout < 0) {
                supportNetworkTimeoutInd = false;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})networkTimeout property not supported by connections due to a negative number returned from first connection object", this.poolName);
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
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})networkTimeout property tested failed on first connection object", this.poolName, e);
            if (networkTimeoutExecutor != null) {
                networkTimeoutExecutor.shutdown();
                networkTimeoutExecutor = null;
            }
        }

        //step9: create a fixed length array and full-fill pooled connections
        boolean defaultCatalogIsNotBlank = !isBlank(defaultCatalog);
        boolean defaultSchemaIsNotBlank = !isBlank(defaultSchema);
        for (int i = 0; i < connectionArrayLen; i++) {
            connectionArray[i].init(
                    //1:defaultAutoCommit
                    isEnableDefaultOnAutoCommit,
                    defaultAutoCommit,
                    //2:defaultTransactionIsolation
                    isEnableDefaultOnTransactionIsolation,
                    defaultTransactionIsolation,
                    //3:defaultReadOnly
                    isEnableDefaultOnReadOnly,
                    defaultReadOnly,
                    //4:defaultCatalog
                    isEnableDefaultOnCatalog,
                    defaultCatalogIsNotBlank,
                    defaultCatalog,
                    poolConfig.isForceDirtyOnCatalogAfterSet(),
                    //5:defaultCatalog
                    isEnableDefaultOnSchema,
                    defaultSchemaIsNotBlank,
                    defaultSchema,
                    poolConfig.isForceDirtyOnSchemaAfterSet(),
                    //6:defaultNetworkTimeout
                    supportNetworkTimeoutInd,
                    defaultNetworkTimeout,
                    networkTimeoutExecutor,
                    //7:others
                    poolConfig.getSqlExceptionCodeList(),
                    poolConfig.getSqlExceptionStateList(),
                    poolConfig.getEvictPredicate());
        }
    }

    //***************************************************************************************************************//
    //                               2: Pooled connection borrow and release methods(11)                             //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1: Attempts to get a connection from pool
    public Connection getConnection() throws SQLException {
        return createProxyConnection(this.getPooledConnection());
    }

    //Method-2.2: Attempts to get a XAConnection from pool
    public XAConnection getXAConnection() throws SQLException {
        PooledConnection p = this.getPooledConnection();
        ProxyConnectionBase proxyConn = createProxyConnection(p);

        XAResource proxyResource = this.isRawXaConnFactory ? new XaProxyResource(p.rawXaRes, proxyConn) : new XaResourceLocalImpl(proxyConn, p.defaultAutoCommit);
        return new XaProxyConnection(proxyConn, proxyResource);
    }

    //Method-2.3: Attempts to get a pooled connection from pool(this method is core place of pool)
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY)
            throw new ConnectionGetForbiddenException("Pool was closed or in cleaning");

        //1: try to reuse connection in thread local
        Borrower b = null;
        PooledConnection p;
        if (this.enableThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING)) {
                    if (this.testOnBorrow(p)) return b.lastUsed = p;
                    b.lastUsed = null;
                }
            }
        }

        //2: try to acquire a permit from pool semaphore
        long deadline = System.nanoTime();
        try {
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ConnectionGetTimeoutException("Waited timeout on pool semaphore");
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        }

        //3: try to search idle one or create new one
        try {
            p = this.searchOrCreate();
            if (p != null) {
                semaphore.release();
                //put connection to thread local
                if (this.enableThreadLocal)
                    putToThreadLocal(p, b, b != null);
                return p;
            }
        } catch (SQLException e) {
            semaphore.release();
            throw e;
        }

        //4: add the borrower to wait queue
        boolean hasCached;
        if (b == null) {
            hasCached = false;
            b = new Borrower();
        } else {
            b.state = null;
            hasCached = true;
        }
        this.waitQueue.offer(b);
        SQLException cause = null;
        deadline += this.maxWaitNs;

        //5: self-spin to get transferred connection
        do {
            Object s = b.state;//one of possible values: PooledConnection,Throwable,null
            if (s instanceof PooledConnection) {
                p = (PooledConnection) s;
                if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                    this.semaphore.release();
                    this.waitQueue.remove(b);

                    if (this.enableThreadLocal)//put to thread local
                        putToThreadLocal(p, b, hasCached);
                    return p;
                }
            } else if (s instanceof Throwable) {
                this.semaphore.release();
                this.waitQueue.remove(b);
                throw s instanceof SQLException ? (SQLException) s : new ConnectionGetException((Throwable) s);
            }

            if (cause != null) {
                BorrowStUpd.compareAndSet(b, s, cause);
            } else if (s != null) {//here:variable s must be a PooledConnection
                b.state = null;
            } else {//here:(s == null)
                long t = deadline - System.nanoTime();
                if (t > spinForTimeoutThreshold) {//notify pool servant thread to get one before parking
                    if (this.servantTryCount.get() > 0 && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                        LockSupport.unpark(this);

                    LockSupport.parkNanos(t);//park end (1: a transfer arrived 2: park timeout 3: an interruption occurred)
                    if (Thread.interrupted())
                        cause = new ConnectionGetInterruptedException("An interruption occurred while waiting for a released connection");
                } else if (t <= 0L) {//timeout
                    cause = new ConnectionGetTimeoutException("Waited timeout for a released connection");
                }
            }//end
        } while (true);//while
    }

    //Method-2.4: put borrowed pooled connection to thread local
    private void putToThreadLocal(PooledConnection p, Borrower b, boolean hasCached) {
        if (hasCached) {
            b.lastUsed = p;
        } else {
            if (b == null) b = new Borrower();
            b.lastUsed = p;
            this.threadLocal.set(new WeakReference<>(b));
        }
    }

    //Method-2.5: add count of retry and notify servant thread work if exists waiter
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount.get();
            if (c >= this.connectionArrayLen) return;
        } while (!this.servantTryCount.compareAndSet(c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    //Method-2.6: recycle a Pooled Connection,may transfer it to one waiter
    public void recycle(PooledConnection p) {
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
        this.removePooledConn(p, reason);
        this.tryWakeupServantThread();
    }

    //Method-2.8: transfer an exception to a waiter
    private void transferException(Throwable e) {
        for (Borrower b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-2.9: alive test on a borrowed connection
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime > this.aliveAssumeTimeMs && !this.conValidTest.isAlive(p)) {
            this.removePooledConn(p, DESC_RM_BAD);
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
        return p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING);
    }

    //***************************************************************************************************************//
    //                       3: Pooled connection idle-timeout/hold-timeout scan methods(3)                          //                                                                                  //
    //***************************************************************************************************************//
    //Method-3.1: shutdown pool inner threads
    private void shutdownPoolThreads() {
        int curState = this.servantState.get();
        this.servantState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this);

        curState = this.idleScanState.get();
        this.idleScanState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this.idleScanThread);
    }

    //Method-3.2: run method of pool servant
    public void run() {
        while (poolState != POOL_CLOSED) {
            while (servantState.get() == THREAD_WORKING) {
                int c = servantTryCount.get();
                if (c <= 0 || (waitQueue.isEmpty() && servantTryCount.compareAndSet(c, 0))) break;
                servantTryCount.decrementAndGet();

                try {
                    PooledConnection p = searchOrCreate();
                    if (p != null) recycle(p);
                } catch (Throwable e) {
                    this.transferException(e);
                }
            }

            if (servantState.get() == THREAD_EXIT)
                break;
            if (servantTryCount.get() == 0 && servantState.compareAndSet(THREAD_WORKING, THREAD_WAITING))
                LockSupport.park();
        }
    }

    //Method-3.3: clean timeout connections(idle timeout and hold timeout)
    void closeIdleTimeoutConnection() {
        //step1:print pool info before clean
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})before timed scan,idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: interrupt current creation of a connection when this operation is timeout
        this.interruptConnectionCreating(true);

        //step3: clean timeout connection in a loop
        for (PooledConnection p : this.connectionArray) {
            final int state = p.state;
            if (state == CON_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime >= this.idleTimeoutMs;
                if (isTimeoutInIdle && ConStUpd.compareAndSet(p, state, CON_CLOSED)) {//need close idle
                    this.removePooledConn(p, DESC_RM_IDLE);
                    this.tryWakeupServantThread();
                }
            } else if (state == CON_USING && supportHoldTimeout) {
                if (System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) {
                        oclose(proxyInUsing);
                    } else {
                        this.removePooledConn(p, DESC_RM_BAD);
                        this.tryWakeupServantThread();
                    }
                }
            } else if (state == CON_CLOSED) {
                this.removePooledConn(p, DESC_RM_CLOSED);
                this.tryWakeupServantThread();
            }
        }

        //step4: print pool info after clean
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})after timed scan,idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                  4: Pool clear/close methods(6)                                               //                                                                                  //
    //***************************************************************************************************************//
    public void clear(boolean forceCloseUsing) throws SQLException {
        clear(forceCloseUsing, false, null);
    }

    //Method-4.2: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException {
        clear(forceCloseUsing, true, config);
    }

    //Method-4.3: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    private void clear(boolean forceCloseUsing, boolean reinit, BeeDataSourceConfig config) throws SQLException {
        if (reinit && config == null)
            throw new BeeDataSourceConfigException("Configuration for pool reinitialization can' be null");

        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            try {
                BeeDataSourceConfig checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                Log.info("BeeCP({})begin to remove all connections", this.poolName);
                this.removeAllConnections(forceCloseUsing, DESC_RM_CLEAR);
                Log.info("BeeCP({})completed to remove all connections", this.poolName);

                if (reinit) {
                    this.poolConfig = checkedConfig;
                    Log.info("BeeCP({})start to reinitialize pool", this.poolName);
                    startup(POOL_CLEARING);//throws SQLException only fail to create initial connections or fail to set default
                    //note: if failed,this method may be recalled with correct configuration
                    Log.info("BeeCP({})completed to reinitialize pool successful", this.poolName);
                }
            } finally {
                this.poolState = POOL_READY;
            }
        } else {
            throw new PoolInClearingException("Pool was closed or in cleaning");
        }
    }

    //Method-4.4: remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        //1:interrupt waiters on semaphore
        this.semaphore.interruptQueuedWaitThreads();
        if (!this.waitQueue.isEmpty()) {
            PoolInClearingException exception = new PoolInClearingException("Force exit due to pool in clearing");
            while (!this.waitQueue.isEmpty()) this.transferException(exception);
        }

        //2: interrupt all threads of connection creation
        this.interruptConnectionCreating(false);

        //3:clear all connections
        int closedCount;
        while (true) {
            closedCount = 0;
            for (PooledConnection p : this.connectionArray) {
                final int state = p.state;
                if (state == CON_IDLE) {
                    if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) {
                        closedCount++;
                        this.removePooledConn(p, source);
                    }
                } else if (state == CON_USING) {
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) {
                        if (force || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime >= holdTimeoutMs)) {//force close or hold timeout
                            oclose(proxyInUsing);
                            if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) {
                                closedCount++;
                                this.removePooledConn(p, source);
                            }
                        }
                    } else {
                        this.removePooledConn(p, source);
                    }
                } else if (state == CON_CLOSED) {
                    closedCount++;
                    this.removePooledConn(p, source);
                }
            }

            if (closedCount == this.connectionArrayLen) break;
            LockSupport.parkNanos(this.delayTimeForNextClearNs);//delay to clear remained pooled connections
        } // while

        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})after clean,idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //Method-4.5: closed check
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //Method-4.6: shut down the pool and set its state to closed
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(this.delayTimeForNextClearNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeCP({})begin to shutdown pool", this.poolName);
                this.unregisterJmx();
                this.shutdownPoolThreads();
                this.removeAllConnections(this.poolConfig.isForceCloseUsingOnClear(), DESC_RM_DESTROY);
                if (networkTimeoutExecutor != null) this.networkTimeoutExecutor.shutdownNow();

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }
                this.poolState = POOL_CLOSED;
                Log.info("BeeCP({})has shutdown pool", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                  5: Pool controller/jmx methods(16)                                           //                                                                                  //
    //***************************************************************************************************************//
    //Method-5.1: indicator on runtime log print,true:enable on;false: enable off
    boolean isPrintRuntimeLog() {
        return printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    //Method-5.3: the length of array stores pooled connections
    public int getTotalSize() {
        return getIdleSize() + getUsingSize();
    }

    //Method-5.4: size of idle pooled connections
    public int getIdleSize() {
        int idleSize = 0;
        for (PooledConnection p : connectionArray) {
            if (p.state == CON_IDLE) idleSize++;
        }
        return idleSize;
    }

    //Method-5.5: size of using pooled connections
    public int getUsingSize() {
        int usingSize = 0;
        for (PooledConnection p : connectionArray) {
            if (p.state == CON_USING) usingSize++;
        }
        return usingSize;
    }

    //Method-5.6: return pool name
    public String getPoolName() {
        return this.poolName;
    }

    //Method-5.7: size of waiting on semaphore
    public int getSemaphoreWaitingSize() {
        return this.semaphore.getQueueLength();
    }

    //Method-5.8: acquired count of semaphore permit
    public int getSemaphoreAcquiredSize() {
        return semaphoreSize - this.semaphore.availablePermits();
    }

    //Method-5.9: count of waiters in queue
    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : this.waitQueue)
            if (borrower.state == null) size++;
        return size;
    }

    //Method-5.10: Get connection count in creating
    public int getConnectionCreatingCount() {
        int count = 0;
        for (PooledConnection p : connectionArray) {
            ConnectionCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) count++;
        }
        return count;
    }

    //Method-5.11: Get connection count in creating
    public int getConnectionCreatingTimeoutCount() {
        int count = 0;
        for (PooledConnection p : connectionArray) {
            ConnectionCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null && System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs)
                count++;
        }
        return count;
    }

    //Method-5.12: interrupt some threads creating connections
    public Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({})attempt to interrupt connection creation,only interrupt create timeout:{}", this.poolName, onlyInterruptTimeout);

        Set<Thread> threads = new HashSet<>(this.semaphoreSize);
        //1: maybe connection array is in initializing,so attempt to interrupt threads on lock
        if (!connectionArrayInitialized) {
            Thread holdThread = connectionArrayInitLock.interruptOwnerThread();
            List<Thread> waitThreads = connectionArrayInitLock.interruptQueuedWaitThreads();
            if (holdThread != null) threads.add(holdThread);
            if (!waitThreads.isEmpty()) threads.addAll(waitThreads);
        }

        //2: attempt to interrupt creating of connections
        if (onlyInterruptTimeout) {
            for (PooledConnection p : connectionArray) {
                ConnectionCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null && System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        } else {
            for (PooledConnection p : connectionArray) {
                ConnectionCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        }

        Thread[] interruptThreads = new Thread[threads.size()];
        return threads.toArray(interruptThreads);
    }

    //Method-5.13: register jmx
    private void registerJmx() {
        if (poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName), poolConfig);
        }
    }

    //Method-5.14: register jmx bean of pool
    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Throwable e) {
            Log.warn("BeeCP({})failed to register jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.15: unregister jmx
    private void unregisterJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName));
        }
    }

    //Method-5.16: jmx unregister
    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Throwable e) {
            Log.warn("BeeCP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.17: pooledConnection valid test method by connection method 'isAlive'
    public boolean isAlive(final PooledConnection p) {
        try {
            if (p.rawConn.isValid(this.aliveTestTimeout)) {
                p.lastAccessTime = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})alive test failed on a borrowed connection", this.poolName, e);
        }
        return false;
    }

    //Method-5.18: creates monitor view object,some runtime info of pool may fill into this object
    private FastConnectionPoolMonitorVo createPoolMonitorVo() {
        Thread currentThread = Thread.currentThread();
        this.poolThreadId = currentThread.getId();
        this.poolThreadName = currentThread.getName();

        try {
            this.poolHostIP = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            Log.info("BeeCP({})failed to resolve host IP", this.poolName);
        }
        return new FastConnectionPoolMonitorVo();
    }

    //Method-5.19: pool monitor vo
    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        int usingSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;
        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_USING) usingSize++;
            if (state == CON_IDLE) idleSize++;

            ConnectionCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) {
                creatingCount++;
                if (System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs)
                    creatingTimeoutCount++;
            }
        }

        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolMaxSize(connectionArrayLen);
        monitorVo.setThreadId(poolThreadId);
        monitorVo.setThreadName(poolThreadName);
        monitorVo.setHostIP(poolHostIP);
        monitorVo.setPoolState(poolState);

        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(usingSize);
        monitorVo.setCreatingCount(creatingCount);
        monitorVo.setCreatingTimeoutCount(creatingTimeoutCount);
        monitorVo.setSemaphoreWaitingSize(this.getSemaphoreWaitingSize());
        monitorVo.setTransferWaitingSize(this.getTransferWaitingSize());
        return this.monitorVo;
    }

    //***************************************************************************************************************//
    //                                  6: Pool inner interface/class(7)                                             //                                                                                  //
    //***************************************************************************************************************//
    //class-6.1:Thread factory
    private static final class PoolThreadThreadFactory implements ThreadFactory {
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

    //class-6.2: A thread running to create new connections when pool starting up
    private static final class PoolInitAsyncCreateThread extends Thread {
        private final FastConnectionPool pool;

        PoolInitAsyncCreateThread(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                pool.createInitConnections(pool.poolConfig.getInitialSize(), false);
                pool.servantState.getAndSet(pool.connectionArray.length);
                if (!pool.waitQueue.isEmpty() && pool.servantState.get() == THREAD_WAITING && pool.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                    LockSupport.unpark(pool);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    //class-6.3: A timed thread to scan idle connections and close them
    private static final class IdleTimeoutScanThread extends Thread {
        private final FastConnectionPool pool;

        IdleTimeoutScanThread(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            final AtomicInteger idleScanState = pool.idleScanState;
            final long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(this.pool.poolConfig.getTimerCheckInterval());
            while (idleScanState.get() == THREAD_WORKING) {
                LockSupport.parkNanos(checkTimeIntervalNanos);
                try {
                    if (pool.poolState == POOL_READY)
                        pool.closeIdleTimeoutConnection();
                } catch (Throwable e) {
                    Log.warn("BeeCP({})an exception occurred while scanning idle-timeout connections", this.pool.poolName, e);
                }
            }
        }
    }

    //class-6.4:JVM exit hook
    private static class ConnectionPoolHook extends Thread {
        private final FastConnectionPool pool;

        ConnectionPoolHook(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            Log.info("BeeCP({})detect Jvm exit,pool will be shutdown", this.pool.poolName);
            try {
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeCP({})an exception occurred when shutdown pool", this.pool.poolName, e);
            }
        }
    }

    //class-6.5:Fair transfer
    private static final class FairTransferPolicy implements PooledConnectionTransferPolicy {
        FairTransferPolicy() {
        }

        public int getStateCodeOnRelease() {
            return CON_USING;
        }

        public boolean tryCatch(PooledConnection p) {
            return p.state == CON_USING;
        }
    }

    //class-6.6: threadLocal caches the last used connections of borrowers(only cache one per borrower)
    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<>(new Borrower());
        }
    }

    //class-6.7: alive test on borrowed connections by executing a SQL
    private static final class PooledConnectionAliveTestBySql implements PooledConnectionAliveTest {
        private final String testSql;
        private final String poolName;
        private final boolean printRuntimeLog;
        private final int validTestTimeout;
        private final boolean isDefaultAutoCommit;
        private final boolean supportQueryTimeout;

        PooledConnectionAliveTestBySql(String poolName, String testSql, int validTestTimeout,
                                       boolean isDefaultAutoCommit, boolean supportQueryTimeout, boolean printRuntimeLog) {
            this.poolName = poolName;
            this.testSql = testSql;
            this.printRuntimeLog = printRuntimeLog;
            this.validTestTimeout = validTestTimeout;
            this.isDefaultAutoCommit = isDefaultAutoCommit;//default
            this.supportQueryTimeout = supportQueryTimeout;
        }

        //method must work in transaction and rollback final to avoid testing dirty data into db
        public boolean isAlive(PooledConnection p) {//
            Statement st = null;
            boolean changed = false;
            Connection rawConn = p.rawConn;
            boolean checkPassed = true;

            try {
                //step1: setAutoCommit
                if (this.isDefaultAutoCommit) {
                    rawConn.setAutoCommit(false);
                    changed = true;
                }

                //step2: create sqlTrace
                st = rawConn.createStatement();
                if (this.supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(validTestTimeout);
                    } catch (Throwable e) {
                        if (printRuntimeLog)
                            Log.warn("BeeCP({})failed to set query timeout value on statement of a borrowed connection", poolName, e);
                    }
                }

                //step3: execute test sql
                try {
                    st.execute(this.testSql);//alive test sql executed under condition that value of autoCommit property must be false
                    p.lastAccessTime = System.currentTimeMillis();
                } finally {
                    rawConn.rollback();//avoid data generated during test saving to db
                }
            } catch (Throwable e) {
                checkPassed = false;
                if (printRuntimeLog)
                    Log.warn("BeeCP({})alive test failure on a borrowed connection by sql,pool will abandon it", poolName, e);
            } finally {
                if (st != null) oclose(st);
                if (changed) {
                    try {
                        rawConn.setAutoCommit(true);
                    } catch (Throwable e) {
                        Log.warn("BeeCP({})failed to reset autoCommit property of a borrowed connection after alive test,pool will abandon it", poolName, e);
                        checkPassed = false;
                    }
                }
            }

            return checkPassed;
        }
    }
}
