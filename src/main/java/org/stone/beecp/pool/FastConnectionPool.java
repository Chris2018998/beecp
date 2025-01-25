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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements BeeConnectionPool, FastConnectionPoolMBean, PooledConnectionAliveTest, PooledConnectionTransferPolicy {
    static final Logger Log = LoggerFactory.getLogger(FastConnectionPool.class);
    static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantState");
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "servantTryCount");

    String poolName;
    volatile int poolState;
    volatile int idleScanState;
    volatile int servantState;
    volatile int servantTryCount;
    BeeDataSourceConfig poolConfig;
    PooledConnection[] connectionArray;//fixed len
    ConcurrentLinkedQueue<Borrower> waitQueue;

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
    private long parkTimeForRetryNs;//nanoseconds
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
    private IdleTimeoutScanThread idleScanThread;
    private boolean enableThreadLocal;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private FastConnectionPoolMonitorVo monitorVo;
    private ConnectionPoolHook exitHook;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //               1: Pool initializes and maintenance on pooled connections(6)                                    //                                                                                  //
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
        this.connectionArrayInitLock = new InterruptionReentrantReadWriteLock();//a lock for connection array initializing
        for (int i = 0; i < connectionArrayLen; i++)
            connectionArray[i] = new PooledConnection(this);

        //step3: fill initial connections to array by syn mode
        this.printRuntimeLog = poolConfig.isPrintRuntimeLog();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getMaxWait());
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

        //step6: creates pool semaphore and create threadLocal by configured indicator
        this.semaphoreSize = poolConfig.getBorrowSemaphoreSize();
        this.semaphore = new InterruptionSemaphore(this.semaphoreSize, isFairMode);
        this.enableThreadLocal = poolConfig.isEnableThreadLocal();
        if (this.threadLocal != null) this.threadLocal = null;//set null if not null
        if (enableThreadLocal) this.threadLocal = new BorrowerThreadLocal();

        //step7: create wait queue,scanning thread,servant thread(an async thread to get connections and transfer to waiters)
        if (POOL_STARTING == poolWorkState) {
            this.waitQueue = new ConcurrentLinkedQueue<>();
            //a working count for servant thread to get connections
            this.servantTryCount = 0;
            this.servantState = THREAD_WORKING;
            this.idleScanState = THREAD_WORKING;
            this.idleScanThread = new IdleTimeoutScanThread(this);

            this.monitorVo = this.createPoolMonitorVo();//pool monitor object
            this.exitHook = new ConnectionPoolHook(this);//a hook works when JVM exit
            Runtime.getRuntime().addShutdownHook(this.exitHook);
            this.registerJmx();

            setDaemon(true);
            setName("BeeCP(" + poolName + ")" + "-asyncAdd");
            start();

            this.idleScanThread.setDaemon(true);
            this.idleScanThread.setName("BeeCP(" + poolName + ")" + "-idleScanner");
            this.idleScanThread.start();
        }

        //step8: create a thread to do pool initialization(create initial connections and fill them to array)
        if (initialSize > 0 && poolConfig.isAsyncCreateInitConnection())
            new PoolInitAsyncCreateThread(this).start();

        //step9: print pool info at end
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
                        connectionArray[i].onRemove(DESC_RM_INIT);
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
                        return this.fillRawConnection(p, CON_BORROWED);
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
                    return this.fillRawConnection(p, CON_BORROWED);
                }
            } else if (state == CON_CLOSED && ConStUpd.compareAndSet(p, CON_CLOSED, CON_CREATING)) {
                return this.fillRawConnection(p, CON_BORROWED);
            }
        }
        return null;
    }

    //Method-1.5: create a created connection and fill it into pooled Connection(a pooled wrapper of connection)
    private PooledConnection fillRawConnection(PooledConnection p, int state) throws SQLException {
        //1: print info of creation starting
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))start to create a connection", this.poolName);

        //2: use factory to create connection
        Connection rawConn = null;
        XAConnection rawXaConn = null;
        XAResource rawXaRes = null;

        try {
            p.creatingInfo = new ConnectionCreatingInfo();//set creating info(thread,start time)which can be read out by calling monitor method
            if (this.isRawXaConnFactory) {
                rawXaConn = this.rawXaConnFactory.create();//this call may be blocked
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

            //3: set default on created connection
            if (connectionArrayInitialized) {
                p.setRawConnection(state, rawConn, rawXaRes);
            } else {
                this.initPooledConnectionArray(rawConn);
                p.setRawConnection2(state, rawConn, rawXaRes);
                connectionArrayInitialized = true;//set initialization flag to true
            }

            //4: print log of creation ending
            if (this.printRuntimeLog)
                Log.info("BeeCP({}))created a new connection:{} to fill pooled connection:{}", this.poolName, rawConn, p);

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
        //step1: initialization for auto-commit property of connection(get default, test default)
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
                firstConn.setAutoCommit(defaultAutoCommit);//set default for test
            } catch (Throwable e) {
                isEnableDefaultOnAutoCommit = false;//disable when test fail
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of auto-commit property on first connection object", this.poolName, defaultAutoCommit, e);
            }
        } else if (defaultAutoCommit == null) {
            defaultAutoCommit = Boolean.TRUE;
        }

        //step2: initialization for transaction-isolation property of connection(get default,test default)
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
                firstConn.setTransactionIsolation(defaultTransactionIsolation);//set default for test
            } catch (Throwable e) {
                isEnableDefaultOnTransactionIsolation = false;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of transaction-isolation property on first connection object", this.poolName, defaultTransactionIsolation, e);
            }
        } else if (defaultTransactionIsolation == null) {
            defaultTransactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
        }

        //step3:get default value of property read-only from config or from first connection
        Boolean defaultReadOnly = poolConfig.isDefaultReadOnly();
        boolean isEnableDefaultOnReadOnly = poolConfig.isEnableDefaultOnReadOnly();
        if (poolConfig.isEnableDefaultOnReadOnly()) {
            if (defaultReadOnly == null) {
                try {
                    defaultReadOnly = firstConn.isReadOnly();
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
                firstConn.setReadOnly(defaultReadOnly);//set default for test
            } catch (Throwable e) {
                isEnableDefaultOnReadOnly = false;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})failed to set default value({}) of read-only property on first connection object", this.poolName, defaultTransactionIsolation, e);
            }
        } else if (defaultReadOnly == null) {
            defaultReadOnly = Boolean.FALSE;
        }

        //step4: initialization for catalog property of connection(get default,test default)
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
                    firstConn.setCatalog(defaultCatalog);//test default by setting it
                } catch (Throwable e) {
                    isEnableDefaultOnCatalog = false;
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to set default value({}) of catalog property on first connection object", this.poolName, defaultCatalog, e);
                }
            }
        }

        //step5: initialization for schema property of connection(get default,test default)
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
                    firstConn.setSchema(defaultSchema);//test default by setting
                } catch (Throwable e) {
                    isEnableDefaultOnSchema = false;
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})failed to set default value({}) of schema property on first connection object", this.poolName, defaultSchema, e);
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
                if (this.printRuntimeLog) {
                    Log.warn("BeeCP({})get false from call of isValid method on first connection object", this.poolName);
                }
            }
        } catch (Throwable e) {
            supportIsValid = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})isValid method tested failed on first connection object", this.poolName, e);
        }

        //step7: second way: if isValid method is not supported, then execute alive test sql to validate it
        if (!supportIsValid) {
            String conTestSql = this.poolConfig.getAliveTestSql();
            boolean supportQueryTimeout = validateTestSql(poolName, firstConn, conTestSql, aliveTestTimeout, defaultAutoCommit);//check test sql
            conValidTest = new PooledConnectionAliveTestBySql(poolName, conTestSql, aliveTestTimeout, defaultAutoCommit, supportQueryTimeout, printRuntimeLog);
        }

        //step8: network timeout check supported in driver or factory
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

        //step9: Ok,we fill default value prepared in previous steps to all pooled connections at final step
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
    //                               2: Connection getting and connection release (10)                               //                                                                                  //
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

    //Method-2.3: attempt to get pooled connection(key method)
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY)
            throw new ConnectionGetForbiddenException("Pool was closed or in cleaning");

        //1: firstly, get last used connection from threadLocal if threadLocal is supported
        Borrower b = null;
        PooledConnection p;
        if (this.enableThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_BORROWED)) {
                    if (this.testOnBorrow(p)) return b.lastUsed = p;
                    b.lastUsed = null;
                }
            }
        }

        //2: get a permit from pool semaphore(reduce concurrency to get connections)
        long deadline = System.nanoTime();
        try {
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ConnectionGetTimeoutException("Waited timeout on pool semaphore");
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        }

        //3: search an idle connection or create a connection on NOT filled pooled connection
        try {
            p = this.searchOrCreate();
        } catch (SQLException e) {
            semaphore.release();
            throw e;
        }
        //3.1: check searched result
        final boolean hasCached = b != null;
        if (p != null) {//release permit of semaphore and put the get connection to threadLocal if cache supportable
            semaphore.release();
            if (this.enableThreadLocal) {
                if (hasCached)
                    b.lastUsed = p;
                else
                    this.threadLocal.set(new WeakReference<>(new Borrower(p)));
            }
            return p;
        }

        //4: join into wait queue for transferred connection released from another borrower
        if (hasCached)
            b.state = null;
        else
            b = new Borrower();
        this.waitQueue.offer(b);
        SQLException cause = null;
        deadline += this.maxWaitNs;

        //5: spin in a loop
        do {
            final Object s = b.state;//acceptable types: PooledConnection,Throwable,null
            if (s instanceof PooledConnection) {
                p = (PooledConnection) s;
                if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                    this.semaphore.release();
                    this.waitQueue.remove(b);

                    if (this.enableThreadLocal) {//put to thread local
                        b.lastUsed = p;
                        if (!hasCached) this.threadLocal.set(new WeakReference<>(b));
                    }
                    return p;
                }
            } else if (s instanceof Throwable) {//here: s must be throwable object
                this.semaphore.release();
                this.waitQueue.remove(b);
                throw s instanceof SQLException ? (SQLException) s : new ConnectionGetException((Throwable) s);
            }

            //reach here:s==null or s is a PooledConnection
            if (cause != null) {
                BorrowStUpd.compareAndSet(b, s, cause);
            } else {
                //** if transferred object is not one of[PooledConnection,Throwable,null],set state to null same to a pooled Connection for robustness
                if (s != null) b.state = null;
                final long t = deadline - System.nanoTime();
                if (t > 0L) {//getting time out check,if not,then attempt to wake up servant thread to work to get one for it before parking
                    if (this.servantTryCount > 0 && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
                        LockSupport.unpark(this);

                    LockSupport.parkNanos(t);//park over,a transferred connection maybe arrived or an exception,or an interruption occurred while waiting
                    if (Thread.interrupted())
                        cause = new ConnectionGetInterruptedException("An interruption occurred while waiting for a released connection");
                } else {//throw a timeout exception
                    cause = new ConnectionGetTimeoutException("Waited timeout for a released connection");
                }
            }
        } while (true);//while
    }

    //Method-2.4: increment servant's count of retry to get connections
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount;
            if (c >= this.connectionArrayLen) return;
        } while (!ServantTryCountUpd.compareAndSet(this, c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    //Method-2.4: recycle a pooled connection and then transfer it to a waiter if exists
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

    //Method-2.6: terminate a Pooled Connection
    void abort(PooledConnection p, String reason) {
        p.onRemove(reason);
        this.tryWakeupServantThread();
    }

    //Method-2.7: transfer an exception occurred in searching,this method called by servant thread
    private void transferException(Throwable e) {
        for (Borrower b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-2.8: do alive test on a borrowed pooled connection
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime >= this.aliveAssumeTimeMs && !this.conValidTest.isAlive(p)) {
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

        curState = this.idleScanState;
        this.idleScanState = THREAD_EXIT;
        if (curState == THREAD_WAITING) LockSupport.unpark(this.idleScanThread);
    }

    //Method-3.2: run method of pool servant
    public void run() {
        while (poolState != POOL_CLOSED) {
            while (servantState == THREAD_WORKING) {
                int c = servantTryCount;
                if (c <= 0 || (waitQueue.isEmpty() && ServantTryCountUpd.compareAndSet(this, c, 0))) break;
                ServantTryCountUpd.decrementAndGet(this);

                try {
                    PooledConnection p = searchOrCreate();
                    if (p != null) recycle(p);
                } catch (Throwable e) {
                    this.transferException(e);
                }
            }

            if (servantState == THREAD_EXIT)
                break;
            if (servantTryCount == 0 && ServantStateUpd.compareAndSet(this, THREAD_WORKING, THREAD_WAITING))
                LockSupport.park();
        }
    }

    //Method-3.3: clean timeout connections(idle timeout and hold timeout)
    void closeIdleTimeoutConnection() {
        //step1:print pool info before clean
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})before timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: interrupt current creation of a connection when this operation is timeout
        this.interruptConnectionCreating(true);

        //step3: clean timeout connection in a loop
        for (PooledConnection p : this.connectionArray) {
            final int state = p.state;
            if (state == CON_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime >= this.idleTimeoutMs;
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
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})after timed scan,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                  4: pool clean and pool close  (6)                                            //                                                                                  //
    //***************************************************************************************************************//
    public void clear(boolean forceRecycleBorrowed) throws SQLException {
        clear(forceRecycleBorrowed, false, null);
    }

    //Method-4.2: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    public void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        clear(forceRecycleBorrowed, true, config);
    }

    //Method-4.3: close all connections in pool and removes them from pool,then re-initializes pool with new configuration
    private void clear(boolean forceRecycleBorrowed, boolean reinit, BeeDataSourceConfig config) throws SQLException {
        if (reinit && config == null)
            throw new BeeDataSourceConfigException("Pool reinitialization configuration can't be null");

        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            try {
                BeeDataSourceConfig checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                Log.info("BeeCP({})begin to remove all connections", this.poolName);
                this.removeAllConnections(forceRecycleBorrowed, DESC_RM_CLEAR);
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
        //1: interrupt waiters on semaphore
        this.semaphore.interruptQueuedWaitThreads();
        //2: interrupt all threads waits on lock or blocking in factory.create method call
        this.interruptConnectionCreating(false);
        //3: transfer exception to waiter in queue
        if (!this.waitQueue.isEmpty()) {
            PoolInClearingException exception = new PoolInClearingException("Pool was closed or in cleaning");
            while (!this.waitQueue.isEmpty()) this.transferException(exception);
        }

        //4:clear all connections
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
                        if (force || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime >= holdTimeoutMs)) //force close or hold timeout
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

        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})after clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeCP({})begin to shutdown pool", this.poolName);
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
                Log.info("BeeCP({})has shutdown pool", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                  5: Pool query/pool interruption/pool jmx/connection test(16)                 //                                                                                  //
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
        return getIdleSize() + getBorrowedSize();
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
    public int getBorrowedSize() {
        int usingSize = 0;
        for (PooledConnection p : connectionArray) {
            if (p.state == CON_BORROWED) usingSize++;
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

    //Method-5.12: interrupt some threads creating connections
    public Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({})attempt to interrupt connection creation,only for timeout:{}", this.poolName, onlyInterruptTimeout);

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

        return threads.toArray(new Thread[0]);
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

    //Method-5.17: do alive test on a pooed connection
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
        int borrowedSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;
        for (PooledConnection p : connectionArray) {
            int state = p.state;
            if (state == CON_BORROWED) borrowedSize++;
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
        monitorVo.setBorrowedSize(borrowedSize);
        monitorVo.setCreatingCount(creatingCount);
        monitorVo.setCreatingTimeoutCount(creatingTimeoutCount);
        monitorVo.setSemaphoreWaitingSize(this.getSemaphoreWaitingSize());
        monitorVo.setTransferWaitingSize(this.getTransferWaitingSize());
        return this.monitorVo;
    }

    //***************************************************************************************************************//
    //                                  6: some inner classes                                                        //                                                                                  //
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
                pool.servantTryCount = pool.connectionArray.length;

                if (!pool.waitQueue.isEmpty() && pool.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(pool, THREAD_WAITING, THREAD_WORKING))
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
            final long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(this.pool.poolConfig.getTimerCheckInterval());
            while (pool.idleScanState == THREAD_WORKING) {
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
            return CON_BORROWED;
        }

        public boolean tryCatch(PooledConnection p) {
            return p.state == CON_BORROWED;
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
