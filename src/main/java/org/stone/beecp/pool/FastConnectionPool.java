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
import org.stone.tools.extension.InterruptionReentrantLock;
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
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements BeeConnectionPool, BeeConnectionPoolJmxBean, PooledConnectionAliveTest, PooledConnectionTransferPolicy {
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");
    private static final Logger Log = LoggerFactory.getLogger(FastConnectionPool.class);

    private String poolName;
    private String poolMode;
    private String poolHostIP;
    private long poolThreadId;
    private String poolThreadName;

    private int poolMaxSize;
    private volatile int poolState;
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

    private PooledConnectionTransferPolicy transferPolicy;
    private boolean templatePooledConnIsReady;
    private PooledConnection templatePooledConn;
    private InterruptionReentrantLock pooledArrayLock;
    private volatile long pooledArrayLockedTimePoint;//milliseconds
    private volatile PooledConnection[] pooledArray;
    private boolean isRawXaConnFactory;
    private RawConnectionFactory rawConnFactory;
    private RawXaConnectionFactory rawXaConnFactory;
    private PooledConnectionAliveTest conValidTest;
    private ThreadPoolExecutor networkTimeoutExecutor;
    private AtomicInteger servantState;
    private AtomicInteger servantTryCount;
    private AtomicInteger idleScanState;
    private IdleTimeoutScanThread idleScanThread;
    private ConcurrentLinkedQueue<Borrower> waitQueue;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private BeeDataSourceConfig poolConfig;
    private FastConnectionPoolMonitorVo monitorVo;
    private ConnectionPoolHook exitHook;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //               1: Pool initializes and maintenance on pooled connections(7)                                    //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Method-1.1: pool initializes.
     *
     * @param config is a pool initialization object contains some field level items.
     * @throws SQLException when check failed on configuration or a error occurs during initialization
     */
    public void init(BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new PoolInitializeFailedException("Pool initialization configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {//initializes after pool state cas success from new to starting
            try {
                checkJdbcProxyClass();
                this.poolConfig = config.check();
                startup(POOL_STARTING);//Go,go! launch the pool
                this.poolState = POOL_READY;//ready to accept coming requests(love u,my pool)
            } catch (Throwable e) {
                Log.info("BeeCP({})Initialized failed", this.poolName, e);
                this.poolState = POOL_NEW;//reset state to new when failed
                throw e instanceof SQLException ? (SQLException) e : new PoolInitializeFailedException(e);
            }
        } else {
            throw new PoolInitializeFailedException("Pool has already been initialized or in initializing");
        }
    }

    // Method-1.2: launch the pool
    private void startup(final int poolWorkState) throws SQLException {
        this.poolName = poolConfig.getPoolName();
        Log.info("BeeCP({})starting up....", this.poolName);

        //step1: copy connection factory from config object
        //factory must be one of [RawConnectionFactory,RawXaConnectionFactory],this checked logic exists in config object
        Object rawFactory = poolConfig.getConnectionFactory();
        if (rawFactory instanceof RawXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (RawXaConnectionFactory) rawFactory;
        } else {
            this.rawConnFactory = (RawConnectionFactory) rawFactory;
        }

        //step2: creates objects related with pooled connections
        this.templatePooledConn = null;
        this.templatePooledConnIsReady = false;//this startup method can be called to restart pool,so need reset this field to false
        this.poolMaxSize = poolConfig.getMaxActive();
        if (POOL_STARTING == poolWorkState) {//just create once
            this.pooledArrayLock = new InterruptionReentrantLock();
            this.pooledArray = new PooledConnection[0];
        }

        //step3: creates initial connections by syn mode
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getMaxWait());//timeout for acquiring on a semaphore or a lock
        if (poolConfig.getInitialSize() > 0 && !poolConfig.isAsyncCreateInitConnection())
            createInitConnections(poolConfig.getInitialSize(), true);

        //step4: creates connections transfer policy
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

        //step5: copy some properties to pool local variables
        this.idleTimeoutMs = poolConfig.getIdleTimeout();
        this.holdTimeoutMs = poolConfig.getHoldTimeout();
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.aliveAssumeTimeMs = poolConfig.getAliveAssumeTime();
        this.aliveTestTimeout = poolConfig.getAliveTestTimeout();
        this.delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
        this.printRuntimeLog = poolConfig.isPrintRuntimeLog();
        this.semaphoreSize = poolConfig.getBorrowSemaphoreSize();

        //step6: creates a pool semaphore and threadLocal
        this.semaphore = new InterruptionSemaphore(this.semaphoreSize, isFairMode);
        this.threadLocal = new BorrowerThreadLocal();//as a cache to store one used connection

        //step7: creates pool others objects
        if (POOL_STARTING == poolWorkState) {
            this.waitQueue = new ConcurrentLinkedQueue<Borrower>();//wait queue(transfer released connections and exceptions of creation)
            this.servantTryCount = new AtomicInteger(0);//count of retry chance applied in a transfer thread
            this.servantState = new AtomicInteger(THREAD_WORKING);//work state of the servant thread
            this.idleScanState = new AtomicInteger(THREAD_WORKING);//work state of idle-scan thread
            this.idleScanThread = new IdleTimeoutScanThread(this);
            this.monitorVo = this.createPoolMonitorVo();//a view object contains pool info,such state,idle,using
            this.exitHook = new ConnectionPoolHook(this);//a hook works when JVM exit
            Runtime.getRuntime().addShutdownHook(this.exitHook);
            this.registerJmx();//registers configuration and pool into JMX

            setDaemon(true);
            //setPriority(3);
            setName("BeeCP(" + poolName + ")" + "-asyncAdd");
            start();

            this.idleScanThread.setDaemon(true);
            this.idleScanThread.setPriority(3);
            this.idleScanThread.setName("BeeCP(" + poolName + ")" + "-idleCheck");
            this.idleScanThread.start();
        }

        //step8: creates initial connections(by async mode or sync mode)
        if (poolConfig.getInitialSize() > 0 && poolConfig.isAsyncCreateInitConnection())
            new PoolInitAsyncCreateThread(this).start();

        //step9: print info of pool initialization after completion
        Log.info("BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}",
                poolName,
                poolMode,
                this.pooledArray.length,
                this.poolMaxSize,
                this.semaphoreSize,
                poolConfig.getMaxWait(),
                poolConfig.getDriverClassName());
    }

    //Method-1.3: creates initial connections with specified size
    private void createInitConnections(int initSize, boolean syn) throws SQLException {
        pooledArrayLock.lock();
        try {
            for (int i = 0; i < initSize; i++)
                this.createPooledConn(CON_IDLE);
        } catch (Throwable e) {
            for (PooledConnection p : this.pooledArray)
                this.removePooledConn(p, DESC_RM_INIT);
            if (syn) {//throws failure exception on syn mode
                if (e instanceof SQLException)
                    throw (SQLException) e;
                else
                    throw new PoolInitializeFailedException(e);
            } else {
                Log.warn("Failed to create initial connections", e);
            }
        } finally {
            pooledArrayLock.unlock();
        }
    }

    //Method-1.4: creates one pooled connection under lock
    private PooledConnection createPooledConn(int state) throws SQLException {
        //1:try to acquire lock for creating one pooled connection
        try {
            if (!this.pooledArrayLock.tryLock(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ConnectionCreateException("Timeout at acquiring lock to create a pooled connection");
        } catch (InterruptedException e) {
            throw new ConnectionCreateException("Interrupted at acquiring lock to create a pooled connection");
        }

        //2:creates one pooled connection if not reach max capacity,otherwise return null
        try {
            this.pooledArrayLockedTimePoint = System.currentTimeMillis();
            int l = this.pooledArray.length;
            if (l < this.poolMaxSize) {
                if (this.printRuntimeLog)
                    Log.info("BeeCP({}))Begin to create a new pooled connection with state:{}", this.poolName, state);

                Connection rawConn = null;
                XAConnection rawXaConn = null;
                XAResource rawXaRes = null;
                try {
                    if (this.isRawXaConnFactory) {
                        rawXaConn = this.rawXaConnFactory.create();//maybe stuck in driver? why not try <method>BeeDataSource.interruptThreadsOnCreationLock()<method>?
                        if (rawXaConn == null) {//if blocking interrupt on LockSupport.park in driver,maybe just return a null connection?
                            if (Thread.interrupted())
                                throw new ConnectionGetInterruptedException("Interrupted on creating a raw xaConnection by factory");
                            throw new ConnectionCreateException("Internal error occurred in xaConnection factory");
                        }

                        rawConn = rawXaConn.getConnection();
                        rawXaRes = rawXaConn.getXAResource();
                    } else {
                        rawConn = this.rawConnFactory.create();
                        if (rawConn == null) {
                            if (Thread.interrupted())
                                throw new ConnectionGetInterruptedException("Interrupted on creating a raw connection by factory");
                            throw new ConnectionCreateException("Internal error occurred in connection factory");
                        }
                    }

                    PooledConnection p;
                    if (this.templatePooledConnIsReady) {//create pooled connections by clone
                        p = this.templatePooledConn.setDefaultAndCreateByClone(rawConn, state, rawXaRes);
                    } else {
                        //create a template pooled connection to clone other news and set default to first connection
                        this.templatePooledConn = this.createTemplatePooledConn(rawConn);
                        this.templatePooledConnIsReady = true;//template pooled connection is ready
                        p = this.templatePooledConn.createFirstByClone(rawConn, state, rawXaRes);//create first pooled connection without default setting
                    }

                    if (this.printRuntimeLog)
                        Log.info("BeeCP({}))Created a new pooled connection:{} with state:{}", this.poolName, p, state);
                    PooledConnection[] arrayNew = new PooledConnection[l + 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, l);
                    arrayNew[l] = p;//tail
                    this.pooledArray = arrayNew;
                    return p;
                } catch (Throwable e) {
                    if (rawConn != null) oclose(rawConn);
                    else if (rawXaConn != null) oclose(rawXaConn);
                    throw e instanceof SQLException ? (SQLException) e : new ConnectionCreateException(e);
                }
            }
            return null;
        } finally {
            this.pooledArrayLockedTimePoint = 0L;
            this.pooledArrayLock.unlock();
        }
    }

    //Method-1.5: remove a pooled connection under lock
    private void removePooledConn(PooledConnection p, String removeType) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))Begin to remove a pooled connection:{} by reason:{}", this.poolName, p, removeType);
        p.onBeforeRemove();

        this.pooledArrayLock.lock();
        try {
            for (int l = this.pooledArray.length, i = l - 1; i >= 0; i--) {
                if (this.pooledArray[i] == p) {
                    PooledConnection[] arrayNew = new PooledConnection[l - 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, i);//copy pre
                    int m = l - i - 1;
                    if (m > 0) System.arraycopy(this.pooledArray, i + 1, arrayNew, i, m);//copy after
                    this.pooledArray = arrayNew;
                    if (this.printRuntimeLog)
                        Log.info("BeeCP({}))Removed a pooled connection:{} for reason:{}", this.poolName, p, removeType);
                    break;
                }
            }
        } finally {
            this.pooledArrayLock.unlock();
        }
    }

    //Method-1.6: get elapsed time of lock owner thread
    public long getElapsedTimeSinceCreationLock() {
        return this.pooledArrayLockedTimePoint > 0L ? System.currentTimeMillis() - this.pooledArrayLockedTimePoint : 0L;
    }

    //Method-1.7: interrupt queued waiters on creation lock and acquired thread,which may be stuck in driver
    public void interruptThreadsOnCreationLock() {
        try {
            this.pooledArrayLock.interruptQueuedWaitThreads();
            this.pooledArrayLock.interruptOwnerThread();
        } catch (Throwable e) {
            Log.warn("BeeCP({})Failed to interrupt threads on lock", e);
        }
    }

    //Method-1.8: creates a template pooled connection on first connection
    private PooledConnection createTemplatePooledConn(Connection rawCon) throws SQLException {
        //step1:get default value of property auto-commit from config or from first connection
        Boolean defaultAutoCommit = poolConfig.isDefaultAutoCommit();
        if (poolConfig.isEnableDefaultOnAutoCommit()) {
            if (defaultAutoCommit == null) {
                try {
                    defaultAutoCommit = rawCon.getAutoCommit();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to get auto commit from first connection", this.poolName, e);
                }
            }
            if (defaultAutoCommit == null) {
                defaultAutoCommit = Boolean.TRUE;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Default auto commit not configured and assign boolean(true)to default value", this.poolName);
            }
            try {
                rawCon.setAutoCommit(defaultAutoCommit);//setting test with default value
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnAutoCommit(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Failed to set default auto commit to first connection", this.poolName, e);
            }
        }

        //step2:get default value of property transaction-isolation from config or from first connection
        Integer defaultTransactionIsolation = poolConfig.getDefaultTransactionIsolationCode();
        if (poolConfig.isEnableDefaultOnTransactionIsolation()) {
            if (defaultTransactionIsolation == null) {
                try {
                    defaultTransactionIsolation = rawCon.getTransactionIsolation();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to get transaction isolation from first connection", this.poolName, e);
                }
            }
            if (defaultTransactionIsolation == null) {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Default transaction isolation not configured and set int(" + defaultTransactionIsolation + ")to default", this.poolName);
            }
            try {
                rawCon.setTransactionIsolation(defaultTransactionIsolation);//default setting test
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnTransactionIsolation(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Failed to set default transaction isolation to first connection", this.poolName, e);
            }
        }

        //step3:get default value of property read-only from config or from first connection
        Boolean defaultReadOnly = poolConfig.isDefaultReadOnly();
        if (poolConfig.isEnableDefaultOnReadOnly()) {
            if (defaultReadOnly == null) {
                try {
                    defaultReadOnly = rawCon.isReadOnly();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to get read only from first connection", this.poolName);
                }
            }
            if (defaultReadOnly == null) {
                defaultReadOnly = Boolean.FALSE;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Default read only not configured and assign boolean(false)to default value", this.poolName);
            }
            try {
                rawCon.setReadOnly(defaultReadOnly);//default setting test
            } catch (Throwable e) {
                poolConfig.setEnableDefaultOnTransactionIsolation(false);
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})Failed to set default read only to first connection", this.poolName, e);
            }
        }

        //step4:get default value of property catalog from config or from first connection
        String defaultCatalog = poolConfig.getDefaultCatalog();
        if (poolConfig.isEnableDefaultOnCatalog()) {
            if (isBlank(defaultCatalog)) {
                try {
                    defaultCatalog = rawCon.getCatalog();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to get catalog from first connection", this.poolName, e);
                }
            }
            if (!isBlank(defaultCatalog)) {
                try {
                    rawCon.setCatalog(defaultCatalog);//default setting test
                } catch (Throwable e) {
                    poolConfig.setEnableDefaultOnCatalog(false);
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to set default catalog to first connection", this.poolName, e);
                }
            }
        }

        //step5:get default value of property schema from config or from first connection
        String defaultSchema = poolConfig.getDefaultSchema();
        if (poolConfig.isEnableDefaultOnSchema()) {
            if (isBlank(defaultSchema)) {
                try {
                    defaultSchema = rawCon.getSchema();
                } catch (Throwable e) {
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to get schema from first connection", this.poolName, e);
                }
            }
            if (!isBlank(defaultSchema)) {
                try {
                    rawCon.setSchema(defaultSchema);//default setting test
                } catch (Throwable e) {
                    poolConfig.setEnableDefaultOnSchema(false);
                    if (this.printRuntimeLog)
                        Log.warn("BeeCP({})Failed to set default schema to first connection", this.poolName, e);
                }
            }
        }

        //step6: check driver whether support 'isAlive' method
        boolean supportIsValid = true;//assume support
        try {
            if (rawCon.isValid(this.aliveTestTimeout)) {
                conValidTest = this;
            } else {
                supportIsValid = false;
                if (this.printRuntimeLog) {
                    Log.warn("BeeCP({})'isAlive' method of connection not supported by driver", this.poolName);
                }
            }
        } catch (Throwable e) {
            supportIsValid = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({}) 'isAlive' method check failed for driver", this.poolName, e);
        }

        //step7:test driver whether support sql query timeout
        if (!supportIsValid) {
            String conTestSql = this.poolConfig.getAliveTestSql();
            boolean supportQueryTimeout = validateTestSql(poolName, rawCon, conTestSql, aliveTestTimeout, defaultAutoCommit);//check test sql
            conValidTest = new PooledConnectionAliveTestBySql(poolName, conTestSql, aliveTestTimeout, defaultAutoCommit, supportQueryTimeout, printRuntimeLog);
        }

        //step8: check driver whether support networkTimeout
        int defaultNetworkTimeout = 0;
        boolean supportNetworkTimeoutInd = true;//assume support
        try {
            defaultNetworkTimeout = rawCon.getNetworkTimeout();
            if (defaultNetworkTimeout < 0) {
                supportNetworkTimeoutInd = false;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})'networkTimeout' property of connection not supported by driver", this.poolName);
            } else {//driver support networkTimeout
                if (this.networkTimeoutExecutor == null) {
                    this.networkTimeoutExecutor = new ThreadPoolExecutor(poolMaxSize, poolMaxSize, 10, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(poolMaxSize), new PoolThreadThreadFactory("BeeCP(" + poolName + ")"));
                    this.networkTimeoutExecutor.allowCoreThreadTimeOut(true);
                }
                rawCon.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            }
        } catch (Throwable e) {
            supportNetworkTimeoutInd = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({}) 'networkTimeout' property check failed for driver", this.poolName, e);
        } finally {
            if (!supportNetworkTimeoutInd && networkTimeoutExecutor != null) {
                networkTimeoutExecutor.shutdown();
                networkTimeoutExecutor = null;
            }
        }

        //step9: create a base pooled connection for creation by clone
        return new PooledConnection(
                this,
                //1:defaultAutoCommit
                poolConfig.isEnableDefaultOnAutoCommit(),
                defaultAutoCommit,
                //2:defaultTransactionIsolation
                poolConfig.isEnableDefaultOnTransactionIsolation(),
                defaultTransactionIsolation,
                //3:defaultReadOnly
                poolConfig.isEnableDefaultOnReadOnly(),
                defaultReadOnly,
                //4:defaultCatalog
                poolConfig.isEnableDefaultOnCatalog(),
                defaultCatalog,
                poolConfig.isForceDirtyOnCatalogAfterSet(),
                //5:defaultCatalog
                poolConfig.isEnableDefaultOnSchema(),
                defaultSchema,
                poolConfig.isForceDirtyOnSchemaAfterSet(),
                //6:defaultNetworkTimeout
                supportNetworkTimeoutInd,
                defaultNetworkTimeout,
                networkTimeoutExecutor,
                //7:others
                poolConfig.getSqlExceptionCodeList(),
                poolConfig.getSqlExceptionStateList(),
                poolConfig.getSqlExceptionPredicate());
    }

    //***************************************************************************************************************//
    //                               2: Pooled connection borrow and release methods(8)                              //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1:borrows a connection from pool(return a resulted wrapper on connection)
    public final Connection getConnection() throws SQLException {
        return createProxyConnection(this.getPooledConnection());
    }

    //Method-2.2:borrows a XaConnection from pool(return a XA resulted wrapper on connection)
    public final XAConnection getXAConnection() throws SQLException {
        PooledConnection p = this.getPooledConnection();
        ProxyConnectionBase proxyConn = createProxyConnection(p);
        return new XaProxyConnection(proxyConn, this.isRawXaConnFactory ? new XaProxyResource(p.rawXaRes, proxyConn) : new XaResourceLocalImpl(proxyConn, p.defaultAutoCommit));
    }

    //Method-2.3:borrows a pooled connection from pool
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY)
            throw new ConnectionGetForbiddenException("Access rejected,cause:pool was closed or in clearing");

        //0: get the last used connection from threadLocal and try to hold it via cas
        Borrower b = this.threadLocal.get().get();
        if (b != null) {
            PooledConnection p = b.lastUsed;
            if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING)) {
                if (this.testOnBorrow(p)) return b.lastUsed = p;
                b.lastUsed = null;
            }
        } else {
            b = new Borrower();
            this.threadLocal.set(new WeakReference<Borrower>(b));
        }

        long deadline = System.nanoTime();
        try {
            //1: Acquires a permit from pool semaphore
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ConnectionGetTimeoutException("Timeout at acquiring semaphore to get a idle connection");
        } catch (InterruptedException e) {
            throw new ConnectionGetInterruptedException("Interrupted at acquiring semaphore to get a idle connection");
        }

        //2: try to search idle one,if not get,then try to create new one when pool not full
        PooledConnection p;
        try {
            p = this.searchOrCreate();
            if (p != null) {
                semaphore.release();
                return b.lastUsed = p;
            }
        } catch (SQLException e) {
            semaphore.release();
            throw e;
        }

        //3:try to get a transferred connection
        b.state = null;
        this.waitQueue.offer(b);//self in,self out
        SQLException cause = null;
        deadline += this.maxWaitNs;

        do {
            Object s = b.state;//PooledConnection,Throwable,null
            if (s instanceof PooledConnection) {
                p = (PooledConnection) s;
                if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                    this.waitQueue.remove(b);
                    this.semaphore.release();
                    return b.lastUsed = p;
                }
            } else if (s instanceof Throwable) {
                this.waitQueue.remove(b);
                this.semaphore.release();
                throw s instanceof SQLException ? (SQLException) s : new ConnectionGetException((Throwable) s);
            }

            if (cause != null) {
                BorrowStUpd.compareAndSet(b, s, cause);
            } else if (s != null) {//here:variable s must be a PooledConnection
                b.state = null;
            } else {//here:(s == null)
                long t = deadline - System.nanoTime();
                if (t > spinForTimeoutThreshold) {
                    if (this.servantTryCount.get() > 0 && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                        LockSupport.unpark(this);

                    LockSupport.parkNanos(t);//park exit:1:get transfer 2:timeout 3:interrupted
                    if (Thread.interrupted())
                        cause = new ConnectionGetInterruptedException("Interrupted while waiting in queue");
                } else if (t <= 0L) {//timeout
                    cause = new ConnectionGetTimeoutException("Timeout in wait queue");
                }
            }//end
        } while (true);//while
    }

    //Method-2.4: search an idle connection,if not get,then try to create new one when capacity not reach max
    private PooledConnection searchOrCreate() throws SQLException {
        PooledConnection[] array = this.pooledArray;
        for (PooledConnection p : array) {
            if (p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING) && this.testOnBorrow(p))
                return p;
        }
        if (this.pooledArray.length < this.poolMaxSize)
            return this.createPooledConn(CON_USING);
        return null;
    }

    //Method-2.5: try to wake up the servant thread in waiting state
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount.get();
            if (c >= this.poolMaxSize) return;
        } while (!this.servantTryCount.compareAndSet(c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    /**
     * Method-2.6: return a borrowed connection to pool,and try to transfer it  to one of waiters
     *
     * @param p released connection
     */
    public final void recycle(PooledConnection p) {
        if (isCompeteMode) p.state = CON_IDLE;
        Iterator<Borrower> iterator = this.waitQueue.iterator();

        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            if (p.state != stateCodeOnRelease) return;
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, p)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }

        if (isFairMode) p.state = CON_IDLE;
        tryWakeupServantThread();
    }

    /**
     * Method-2.7: transfer an exception to one of waiters in queue
     *
     * @param e transferred exception
     */
    private void transferException(Throwable e) {
        Iterator<Borrower> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    /**
     * Method-2.8: remove a bad connection with specified reason
     *
     * @param p bad connection
     */
    final void abandonOnReturn(PooledConnection p, String reason) {
        this.removePooledConn(p, reason);
        this.tryWakeupServantThread();
    }

    /**
     * Method-2.9: alive test on a borrowed connection
     *
     * @return boolean true means the checked connection is alive;false,it is bad
     */
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime > this.aliveAssumeTimeMs && !this.conValidTest.isAlive(p)) {
            this.removePooledConn(p, DESC_RM_BAD);
            this.tryWakeupServantThread();
            return false;
        } else {
            return true;
        }
    }

    public final int getStateCodeOnRelease() {
        return CON_IDLE;
    }

    public final boolean tryCatch(PooledConnection p) {
        return p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING);
    }

    //***************************************************************************************************************//
    //                       3: Pooled connection idle-timeout/hold-timeout scan methods(3)                          //                                                                                  //
    //***************************************************************************************************************//
    //Method-3.1: stop all inner threads of pool
    private void shutdownPoolThreads() {
        int curState = this.servantState.get();
        this.servantState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this);

        curState = this.idleScanState.get();
        this.idleScanState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this.idleScanThread);
    }

    //Method-3.2: thread override method to do search or creating new one
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

    /**
     * Method-3.3: close idle timeout connections when available permit size of semaphore is full
     */
    private void closeIdleTimeoutConnection() {
        //step1:print pool info before clean
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})Before idle clear,{idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2:remove idle timeout and hold timeout
        PooledConnection[] array = this.pooledArray;
        for (PooledConnection p : array) {
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

        //step3: print pool info after idle clean
        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})After idle clear,{idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                  4: Pool clear/close methods(5)                                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-4.1: remove all connections from pool
    public void clear(boolean forceCloseUsing) {
        try {
            clear(forceCloseUsing, null);
        } catch (SQLException e) {
            //do nothing
        }
    }

    //Method-4.2: removes all pooled connections,then startup with new configuration when it not be null and valid
    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            Log.info("BeeCP({})Begin to remove all connections", this.poolName);
            this.removeAllConnections(forceCloseUsing, DESC_RM_CLEAR);
            Log.info("BeeCP({})removed all connections", this.poolName);

            try {
                if (config != null) {
                    Log.info("BeeCP({})Begin to restart with new configuration", this.poolName);
                    this.poolConfig = config.check();

                    /*
                     * maybe pool re-initialized failed with error config,in order to let pool stable,
                     * so still reset pool state to ready,but just do clear again with right configuration to fix it
                     */
                    startup(POOL_CLEARING);
                }
            } catch (Throwable e) {
                Log.error("BeeCP({})Restarted failed", e);
                throw e instanceof SQLException ? (SQLException) e : new PoolInitializeFailedException(e);
            } finally {
                this.poolState = POOL_READY;//reset pool state to be ready once pool restart failed with the new config
                Log.info("BeeCP({})Reset pool state to ready after clearing", this.poolName);
            }
        }
    }

    //Method-4.3: remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        //1:interrupt waiters on lock(maybe stuck on socket)
        try {
            this.pooledArrayLock.interruptQueuedWaitThreads();
            this.pooledArrayLock.interruptOwnerThread();
        } catch (Throwable e) {
            Log.info("BeeCP({})Failed to interrupt threads on lock", e);
        }

        //2:interrupt waiters on semaphore
        this.semaphore.interruptQueuedWaitThreads();
        PoolInClearingException exception = new PoolInClearingException("Access rejected,pool in clearing");
        while (!this.waitQueue.isEmpty()) this.transferException(exception);

        //3:clear all connections
        while (true) {
            PooledConnection[] array = this.pooledArray;
            for (PooledConnection p : array) {
                final int state = p.state;
                if (state == CON_IDLE) {
                    if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) this.removePooledConn(p, source);
                } else if (state == CON_USING) {
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) {
                        if (force || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime >= holdTimeoutMs)) {//force close or hold timeout
                            oclose(proxyInUsing);
                            if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED))
                                this.removePooledConn(p, source);
                        }
                    } else {
                        this.removePooledConn(p, source);
                    }
                } else if (state == CON_CLOSED) {
                    this.removePooledConn(p, source);
                }
            } // for

            if (this.pooledArray.length == 0) break;
            LockSupport.parkNanos(this.delayTimeForNextClearNs);//delay to clear remained pooled connections
        } // while

        if (printRuntimeLog) {
            BeeConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})-{idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(this.delayTimeForNextClearNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeCP({})Begin to shutdown", this.poolName);
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
                Log.info("BeeCP({})Has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                  5: Pool controller/jmx methods(15)                                              //                                                                                  //
    //***************************************************************************************************************//
    //Method-5.1: indicator on runtime log print,true:enable on;false: enable off
    boolean isPrintRuntimeLog() {
        return printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    //Method-5.2: the length of array stores pooled connections
    public int getTotalSize() {
        return this.pooledArray.length;
    }

    //Method-5.3: size of idle pooled connections
    public int getIdleSize() {
        int idleSize = 0;
        PooledConnection[] array = this.pooledArray;
        //for (int i = 0, l = array.length; i < l; i++) {
        for (PooledConnection p : array) {
            if (p.state == CON_IDLE) idleSize++;
        }
        return idleSize;
    }

    //Method-5.4: size of using pooled connections
    public int getUsingSize() {
        int active = this.pooledArray.length - this.getIdleSize();
        return (active > 0) ? active : 0;
    }

    //Method-5.5: return pool name
    public String getPoolName() {
        return this.poolName;
    }

    //Method-5.6: size of waiting on semaphore
    public int getSemaphoreWaitingSize() {
        return this.semaphore.getQueueLength();
    }

    //Method-5.7: acquired count of semaphore permit
    public int getSemaphoreAcquiredSize() {
        return semaphoreSize - this.semaphore.availablePermits();
    }

    //Method-5.8: count of waiters in queue
    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : this.waitQueue)
            if (borrower.state == null) size++;
        return size;
    }

    //Method-5.8: register jmx
    private void registerJmx() {
        if (poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName), poolConfig);
        }
    }

    //Method-5.9: register jmx bean of pool
    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Throwable e) {
            Log.warn("BeeCP({})Failed to assembly jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.10: unregister jmx
    private void unregisterJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format("FastConnectionPool:type=BeeCP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName));
        }
    }

    //Method-5.11: jmx unregister
    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Throwable e) {
            Log.warn("BeeCP({})Failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.12: pooledConnection valid test method by connection method 'isAlive'
    public final boolean isAlive(final PooledConnection p) {
        try {
            if (p.rawConn.isValid(this.aliveTestTimeout)) {
                p.lastAccessTime = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})Failed to test connection with 'isAlive' method", this.poolName, e);
        }
        return false;
    }

    //Method-5.13: creates monitor view object,some runtime info of pool may filled into this object
    private FastConnectionPoolMonitorVo createPoolMonitorVo() {
        Thread currentThread = Thread.currentThread();
        this.poolThreadId = currentThread.getId();
        this.poolThreadName = currentThread.getName();

        try {
            this.poolHostIP = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            Log.info("BeeCP({})Failed to resolve host IP", this.poolName);
        }
        return new FastConnectionPoolMonitorVo();
    }

    //Method-5.14: pool monitor vo
    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolMaxSize(poolMaxSize);
        monitorVo.setThreadId(poolThreadId);
        monitorVo.setThreadName(poolThreadName);
        monitorVo.setHostIP(poolHostIP);

        int totSize = this.getTotalSize();
        int idleSize = this.getIdleSize();
        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
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
                pool.servantState.getAndSet(pool.pooledArray.length);
                if (!pool.waitQueue.isEmpty() && pool.servantState.get() == THREAD_WAITING && pool.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                    LockSupport.unpark(pool);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    //class-6.3: A timed thead to scan idle connections and close them
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
                    Log.warn("BeeCP({})Error at closing idle timeout connections", this.pool.poolName, e);
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
            Log.info("BeeCP({})JVM exit hook is running", this.pool.poolName);
            try {
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeCP({})Error at closing connection pool", this.pool.poolName, e);
            }
        }
    }

    //class-6.5:Fair transfer
    private static final class FairTransferPolicy implements PooledConnectionTransferPolicy {
        public int getStateCodeOnRelease() {
            return CON_USING;
        }

        public final boolean tryCatch(PooledConnection p) {
            return p.state == CON_USING;
        }
    }

    //class-6.6: threadLocal caches the last used connections of borrowers(only cache one per borrower)
    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<Borrower>(new Borrower());
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

        private PooledConnectionAliveTestBySql(String poolName, String testSql, int validTestTimeout,
                                               boolean isDefaultAutoCommit, boolean supportQueryTimeout, boolean printRuntimeLog) {
            this.poolName = poolName;
            this.testSql = testSql;
            this.printRuntimeLog = printRuntimeLog;
            this.validTestTimeout = validTestTimeout;
            this.isDefaultAutoCommit = isDefaultAutoCommit;//default
            this.supportQueryTimeout = supportQueryTimeout;
        }

        //method must work in transaction and rollback final to avoid testing dirty data into db
        public final boolean isAlive(PooledConnection p) {//
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
                            Log.warn("BeeCP({})Called failed on method 'setQueryTimeout' in sql tester", poolName, e);
                    }
                }

                //step3: execute test sql
                try {
                    st.execute(this.testSql);
                    p.lastAccessTime = System.currentTimeMillis();
                } finally {
                    rawConn.rollback();//must rollback avoid dirty data into db.if rollback failed,the connection need be abandon
                }
            } catch (Throwable e) {
                checkPassed = false;
                if (printRuntimeLog)
                    Log.warn("BeeCP({})SQL tested failed on borrowed connection", poolName, e);
            } finally {
                if (st != null) oclose(st);
                if (changed) {
                    try {
                        rawConn.setAutoCommit(true);
                    } catch (Throwable e) {
                        Log.warn("BeeCP({})Default value(true) reset failed on borrowed connection after sql test", poolName, e);
                        checkPassed = false;
                    }
                }
            }

            return checkPassed;
        }
    }
}
