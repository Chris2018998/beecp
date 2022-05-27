/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.RawConnectionFactory;
import cn.beecp.RawXaConnectionFactory;
import cn.beecp.pool.atomic.AtomicIntegerFieldUpdaterImpl;
import cn.beecp.pool.atomic.AtomicReferenceFieldUpdaterImpl;
import cn.beecp.pool.exception.PoolClosedException;
import cn.beecp.pool.exception.PoolCreateFailedException;
import cn.beecp.pool.exception.PoolInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJmxBean, PooledConnectionValidTest, PooledConnectionTransferPolicy {
    private static final BorrowerState BOWER_NORMAL = new BorrowerState();
    private static final BorrowerState BOWER_WAITING = new BorrowerState();
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = AtomicIntegerFieldUpdaterImpl.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = AtomicReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastConnectionPool> PoolStateUpd = AtomicIntegerFieldUpdaterImpl.newUpdater(FastConnectionPool.class, "poolState");
    private static final Logger Log = LoggerFactory.getLogger(FastConnectionPool.class);
    private final Object synLock = new Object();

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
    private PoolSemaphore semaphore;
    private long maxWaitNs;//nanoseconds
    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds
    private long validAssumeTime;//milliseconds
    private int validTestTimeout;//seconds
    private long delayTimeForNextClearNs;//nanoseconds
    private Boolean defaultAutoCommit;//maybe value from first raw connection

    private int stateCodeOnRelease;
    private PooledConnectionTransferPolicy transferPolicy;
    private boolean templatePooledConnNotCreated = true;
    private PooledConnection templatePooledConn;
    private volatile PooledConnection[] pooledArray;
    private boolean isRawXaConnFactory;
    private RawConnectionFactory rawConnFactory;
    private RawXaConnectionFactory rawXaConnFactory;
    private PooledConnectionValidTest conValidTest;
    private ThreadPoolExecutor networkTimeoutExecutor;

    private AtomicInteger servantState;
    private AtomicInteger servantTryCount;
    private AtomicInteger idleScanState;
    private IdleTimeoutScanThread idleScanThread;
    private ConcurrentLinkedQueue<Borrower> waitQueue;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private BeeDataSourceConfig poolConfig;
    private ConnectionPoolMonitorVo monitorVo;
    private ConnectionPoolHook exitHook;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //               1: Pool initialize and Pooled connection create/remove methods(7)                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-1.1: check some statement classes whether exists
    private static void checkProxyClasses() throws SQLException {
        String[] classNames = {
                "cn.beecp.pool.Borrower",
                "cn.beecp.pool.PooledConnection",
                "cn.beecp.pool.ProxyConnection",
                "cn.beecp.pool.ProxyStatement",
                "cn.beecp.pool.ProxyPsStatement",
                "cn.beecp.pool.ProxyCsStatement",
                "cn.beecp.pool.ProxyDatabaseMetaData",
                "cn.beecp.pool.ProxyResultSet"};
        try {
            for (String className : classNames)
                Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new PoolCreateFailedException("Jdbc statement classes missed", e);
        }
    }

    /**
     * Method-1.2: pool initialize with configuration
     *
     * @param config pool configuration
     * @throws SQLException configuration check fail or initiated connection create failed
     */
    public void init(BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new PoolCreateFailedException("Configuration can't be null");
        if (this.poolState != POOL_NEW) throw new PoolCreateFailedException("Pool has initialized");

        //step1:check
        checkProxyClasses();
        this.poolConfig = config.check();//why need a copy here?
        this.poolName = this.poolConfig.getPoolName();
        this.poolMaxSize = this.poolConfig.getMaxActive();
        Log.info("BeeCP({})starting....", this.poolName);

        //step2;try to create initial pooled connection
        Object rawFactory = this.poolConfig.getConnectionFactory();
        if (rawFactory instanceof RawXaConnectionFactory) {
            this.isRawXaConnFactory = true;
            this.rawXaConnFactory = (RawXaConnectionFactory) rawFactory;
        } else if (rawFactory instanceof RawConnectionFactory) {
            this.rawConnFactory = (RawConnectionFactory) rawFactory;
        } else {
            throw new PoolCreateFailedException("Invalid connection factory");
        }
        this.pooledArray = new PooledConnection[0];
        this.createInitConnections(this.poolConfig.getInitialSize());

        //step3;create transfer policy by config
        if (this.poolConfig.isFairMode()) {
            poolMode = "fair";
            isFairMode = true;
            this.transferPolicy = new FairTransferPolicy();
        } else {
            poolMode = "compete";
            isCompeteMode = true;
            this.transferPolicy = this;
        }
        this.stateCodeOnRelease = this.transferPolicy.getStateCodeOnRelease();

        //step4;copy configuration to pool
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(this.poolConfig.getMaxWait());
        this.idleTimeoutMs = this.poolConfig.getIdleTimeout();
        this.holdTimeoutMs = this.poolConfig.getHoldTimeout();
        this.validAssumeTime = this.poolConfig.getValidAssumeTime();
        this.validTestTimeout = this.poolConfig.getValidTestTimeout();
        this.delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(this.poolConfig.getDelayTimeForNextClear());
        this.printRuntimeLog = this.poolConfig.isPrintRuntimeLog();

        this.semaphoreSize = this.poolConfig.getBorrowSemaphoreSize();
        this.semaphore = new PoolSemaphore(this.semaphoreSize, isFairMode);
        this.waitQueue = new ConcurrentLinkedQueue<Borrower>();
        this.threadLocal = new ThreadLocal<WeakReference<Borrower>>();
        this.servantTryCount = new AtomicInteger(0);
        this.servantState = new AtomicInteger(THREAD_WORKING);
        this.idleScanState = new AtomicInteger(THREAD_WORKING);
        this.idleScanThread = new IdleTimeoutScanThread(this);

        this.monitorVo = this.createPoolMonitorVo();
        this.exitHook = new ConnectionPoolHook(this);
        Runtime.getRuntime().addShutdownHook(this.exitHook);
        this.registerJmx();

        setDaemon(true);
        setName(this.poolName + "-workServant");
        this.idleScanThread.setDaemon(true);
        this.idleScanThread.setName(this.poolName + "-idleCheck");
        setPriority(Thread.MIN_PRIORITY);
        this.idleScanThread.setPriority(Thread.MIN_PRIORITY);

        start();
        this.idleScanThread.start();
        this.poolState = POOL_READY;

        Log.info("BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}",
                this.poolName,
                poolMode,
                this.pooledArray.length,
                this.poolMaxSize,
                this.semaphoreSize,
                this.poolConfig.getMaxWait(),
                this.poolConfig.getDriverClassName());
    }

    /**
     * Method-1.3: create specified size connections at pool initialization,
     * if zero,then try to create one
     *
     * @throws SQLException error occurred in creating connections
     */
    private void createInitConnections(int initSize) throws SQLException {
        try {
            int size = initSize > 0 ? initSize : 1;
            for (int i = 0; i < size; i++)
                this.createPooledConn(CON_IDLE);
        } catch (Throwable e) {
            for (PooledConnection p : this.pooledArray)
                this.removePooledConn(p, DESC_RM_INIT);
            if (initSize > 0) throw e instanceof SQLException ? (SQLException) e : new PoolInternalException(e);
        }
    }

    //Method-1.4: create one pooled connection
    private PooledConnection createPooledConn(int state) throws SQLException {
        synchronized (this.synLock) {
            int l = this.pooledArray.length;
            if (l < this.poolMaxSize) {
                if (this.printRuntimeLog)
                    Log.info("BeeCP({}))begin to create a new pooled connection,state:{}", this.poolName, state);

                Connection rawConn = null;
                XAConnection rawXaConn = null;
                XAResource rawXaRes = null;
                try {
                    if (this.isRawXaConnFactory) {
                        rawXaConn = this.rawXaConnFactory.create();
                        rawConn = rawXaConn.getConnection();
                        rawXaRes = rawXaConn.getXAResource();
                    } else {
                        rawConn = this.rawConnFactory.create();
                    }

                    if (this.templatePooledConnNotCreated) {
                        templatePooledConn = this.createTemplatePooledConn(rawConn);
                        templatePooledConnNotCreated = false;//template pooled connection remark as created
                    }

                    PooledConnection p = this.templatePooledConn.setDefaultAndCopy(rawConn, state, rawXaRes);
                    if (this.printRuntimeLog)
                        Log.info("BeeCP({}))has created a new pooled connection:{},state:{}", this.poolName, p, state);
                    PooledConnection[] arrayNew = new PooledConnection[l + 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, l);
                    arrayNew[l] = p;// tail
                    this.pooledArray = arrayNew;
                    return p;
                } catch (Throwable e) {
                    if (rawConn != null) oclose(rawConn);
                    else if (rawXaConn != null) oclose(rawXaConn);
                    throw e instanceof SQLException ? (SQLException) e : new PoolInternalException(e);
                }
            } else {
                return null;
            }
        }
    }

    //Method-1.5: remove one pooled connection
    private void removePooledConn(PooledConnection p, String removeType) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))begin to remove pooled connection:{},reason:{}", this.poolName, p, removeType);
        p.onBeforeRemove();
        synchronized (this.synLock) {
            for (int l = this.pooledArray.length, i = l - 1; i >= 0; i--) {
                if (this.pooledArray[i] == p) {
                    PooledConnection[] arrayNew = new PooledConnection[l - 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, i);//copy pre
                    int m = l - i - 1;
                    if (m > 0) System.arraycopy(this.pooledArray, i + 1, arrayNew, i, m);//copy after
                    this.pooledArray = arrayNew;
                    if (this.printRuntimeLog)
                        Log.info("BeeCP({}))has removed pooled connection:{},reason:{}", this.poolName, p, removeType);
                    break;
                }
            }
        }
    }

    //Method-1.6: test first connection and create template pooled connection
    private PooledConnection createTemplatePooledConn(Connection rawCon) throws SQLException {
        //step1:get autoCommit default value
        this.defaultAutoCommit = this.poolConfig.isDefaultAutoCommit();
        if (this.defaultAutoCommit == null) this.defaultAutoCommit = rawCon.getAutoCommit();

        //step2:get transactionIsolation default value
        Integer defaultTransactionIsolation = this.poolConfig.getDefaultTransactionIsolationCode();
        if (defaultTransactionIsolation == null) defaultTransactionIsolation = rawCon.getTransactionIsolation();

        //step3:get readOnly default value
        Boolean defaultReadOnly = this.poolConfig.isDefaultReadOnly();
        if (defaultReadOnly == null) defaultReadOnly = rawCon.isReadOnly();

        //step4:get catalog default value
        String defaultCatalog = this.poolConfig.getDefaultCatalog();
        if (isBlank(defaultCatalog))
            try {
                defaultCatalog = rawCon.getCatalog();
            } catch (Throwable e) {
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})driver not support 'catalog'", this.poolName);
            }

        //step5:get schema default value
        String defaultSchema = this.poolConfig.getDefaultSchema();
        if (isBlank(defaultSchema))
            try {
                defaultSchema = rawCon.getSchema();
            } catch (Throwable e) {
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})driver not support 'schema'", this.poolName);
            }

        //step6: check driver whether support networkTimeout
        int defaultNetworkTimeout = 0;
        boolean supportNetworkTimeoutInd = true;//assume support
        try {
            defaultNetworkTimeout = rawCon.getNetworkTimeout();
            if (defaultNetworkTimeout < 0) {
                supportNetworkTimeoutInd = false;
                if (this.printRuntimeLog)
                    Log.warn("BeeCP({})driver not support 'networkTimeout'", this.poolName);
            } else {
                ThreadPoolExecutor networkTimeoutExecutor = new ThreadPoolExecutor(1, poolMaxSize, 10, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(this.poolMaxSize), new PoolThreadThreadFactory());
                networkTimeoutExecutor.allowCoreThreadTimeOut(true);
                rawCon.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
                this.networkTimeoutExecutor = networkTimeoutExecutor;
            }
        } catch (Throwable e) {
            supportNetworkTimeoutInd = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})driver not support 'networkTimeout',cause:", this.poolName, e);
        }

        //step7: check driver whether support 'isValid' method
        boolean supportIsValid = true;//assume support
        try {
            if (rawCon.isValid(this.validTestTimeout)) {
                conValidTest = this;
            } else {
                supportIsValid = false;
                if (this.printRuntimeLog) {
                    Log.warn("BeeCP({})driver not support 'isValid'", this.poolName);
                }
            }
        } catch (Throwable e) {
            supportIsValid = false;
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})driver not support 'isValid',cause:", this.poolName, e);
        }

        //step8: check driver support 'isValid' method
        if (!supportIsValid) {
            String conTestSql = this.poolConfig.getValidTestSql();
            boolean supportQueryTimeout = validateTestSql(this.poolName, rawCon, conTestSql, this.validTestTimeout, this.defaultAutoCommit);//check sql
            conValidTest = new PooledConnectionValidTestBySql(conTestSql, this.defaultAutoCommit, supportQueryTimeout);
        }

        //step9:create a pooled connection with some value,other pooled connection will copy it
        return new PooledConnection(
                this.defaultAutoCommit,
                defaultTransactionIsolation,
                defaultReadOnly,
                defaultCatalog,
                defaultSchema,
                defaultNetworkTimeout,
                supportNetworkTimeoutInd,
                this.networkTimeoutExecutor,
                this);
    }


    //***************************************************************************************************************//
    //                               2: Pooled connection borrow and release methods(8)                              //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1:borrow one connection from pool
    public final Connection getConnection() throws SQLException {
        return createProxyConnection(this.getPooledConnection());
    }

    //Method-2.2:borrow one XaConnection from pool
    public final XAConnection getXAConnection() throws SQLException {
        PooledConnection p = this.getPooledConnection();
        ProxyConnectionBase proxyCon = createProxyConnection(p);
        return new XaProxyConnection(proxyCon, this.isRawXaConnFactory ? new XaProxyResource(p.rawXaRes, proxyCon) : new XaResourceLocalImpl(proxyCon, this.defaultAutoCommit));
    }

    //Method-2.3:borrow one connection from pool
    private PooledConnection getPooledConnection() throws SQLException {
        if (this.poolState != POOL_READY) throw new PoolClosedException("Pool has shut down or in clearing");

        //0:try to get from threadLocal cache
        WeakReference<Borrower> r = this.threadLocal.get();
        Borrower b = r != null ? r.get() : null;
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
            //1:try to acquire a permit
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new SQLException("Request timeout");
        } catch (InterruptedException e) {
            throw new SQLException("Request interrupted");
        }

        try {//semaphore acquired
            //2:try search one or create one
            PooledConnection p = this.searchOrCreate();
            if (p != null) return b.lastUsed = p;

            //3:try to get one transferred connection
            b.state = BOWER_NORMAL;
            this.waitQueue.offer(b);
            boolean failed = false;
            Throwable cause = null;
            deadline += this.maxWaitNs;
            Thread thd = b.thread;

            do {
                Object s = b.state;//PooledConnection,Throwable,BOWER_NORMAL
                if (s instanceof PooledConnection) {
                    p = (PooledConnection) s;
                    if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                        this.waitQueue.remove(b);
                        return b.lastUsed = p;
                    }
                } else if (s instanceof Throwable) {
                    this.waitQueue.remove(b);
                    throw s instanceof SQLException ? (SQLException) s : new PoolInternalException((Throwable) s);
                }

                if (failed) {
                    BorrowStUpd.compareAndSet(b, s, cause);
                } else if (s instanceof PooledConnection) {
                    b.state = BOWER_NORMAL;
                    Thread.yield();
                } else {//here:(s == BOWER_NORMAL)
                    long t = deadline - System.nanoTime();
                    if (t > 0L) {
                        if (b.state == BOWER_NORMAL && BorrowStUpd.compareAndSet(b, BOWER_NORMAL, BOWER_WAITING)) {
                            if (this.servantTryCount.get() > 0 && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                                LockSupport.unpark(this);

                            LockSupport.parkNanos(t);//block exit:1:get transfer 2:interrupted 3:timeout 4:unpark before parkNanos
                            if (thd.isInterrupted()) {
                                failed = true;
                                cause = new SQLException("Request interrupted");
                            }
                            if (b.state == BOWER_WAITING && BorrowStUpd.compareAndSet(b, BOWER_WAITING, failed ? cause : BOWER_NORMAL) && !failed)
                                Thread.yield();
                        }
                    } else {//timeout
                        failed = true;
                        cause = new SQLException("Request timeout");
                    }
                }//end (s == BOWER_NORMAL)
            } while (true);//while
        } finally {
            this.semaphore.release();
        }
    }

    //Method-2.4: search one idle connection,if not found,then try to create one
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

    //Method-2.5: try to wakeup servant thread to work if it waiting
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
     * Method-2.6: Connection return to pool after it end use,if exist waiter in pool,
     * then try to transfer the connection to one waiting borrower
     *
     * @param p target connection need release
     */
    public final void recycle(PooledConnection p) {
        if (isCompeteMode) p.state = CON_IDLE;
        Iterator<Borrower> iterator = this.waitQueue.iterator();
        W:
        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            Object state;
            do {
                if (p.state != stateCodeOnRelease)
                    return;
                state = b.state;
                if (!(state instanceof BorrowerState)) continue W;
            } while (!BorrowStUpd.compareAndSet(b, state, p));
            if (state == BOWER_WAITING) LockSupport.unpark(b.thread);
            return;
        }

        if (isFairMode) p.state = CON_IDLE;
        tryWakeupServantThread();
    }

    /**
     * Method-2.7: Connection create failed by creator,then transfer the failed cause exception to one waiting borrower,
     * which will end wait and throw the exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(Throwable e) {
        Iterator<Borrower> iterator = waitQueue.iterator();
        W:
        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            Object state;
            do {
                state = b.state;
                if (!(state instanceof BorrowerState)) continue W;
            } while (!BorrowStUpd.compareAndSet(b, state, e));
            if (state == BOWER_WAITING) LockSupport.unpark(b.thread);
            return;
        }
    }

    /**
     * Method-2.8: when exception occur on return,then remove it from pool
     *
     * @param p target connection need release
     */
    final void abandonOnReturn(PooledConnection p) {
        this.removePooledConn(p, DESC_RM_BAD);
        this.tryWakeupServantThread();
    }

    /**
     * Method-2.9: check one borrowed connection alive state,if not alive,then remove it from pool
     *
     * @return boolean, true:alive
     */
    private boolean testOnBorrow(PooledConnection p) {
        if (System.currentTimeMillis() - p.lastAccessTime > this.validAssumeTime && !this.conValidTest.isValid(p)) {
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
    //                       3: Pooled connection idle-timeout/hold-timeout scan methods(4)                          //                                                                                  //
    //***************************************************************************************************************//
    //Method-3.1: check whether exists borrows under semaphore
    private boolean existBorrower() {
        return this.semaphoreSize > this.semaphore.availablePermits();
    }

    //Method-3.2 shutdown two work threads in pool
    private void shutdownPoolThread() {
        int curState = this.servantState.get();
        this.servantState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this);

        curState = this.idleScanState.get();
        this.idleScanState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this.idleScanThread);
    }

    //Method-3.3: pool servant thread run method
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
     * Method-3.4: inner timer will call the method to clear some idle timeout connections
     * or dead connections,or long time not active connections in using state
     */
    private void closeIdleTimeoutConnection() {
        if (this.poolState == POOL_READY) {
            PooledConnection[] array = this.pooledArray;
            for (PooledConnection p : array) {
                final int state = p.state;
                if (state == CON_IDLE && !this.existBorrower()) {
                    boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime >= this.idleTimeoutMs;
                    if (isTimeoutInIdle && ConStUpd.compareAndSet(p, state, CON_CLOSED)) {//need close idle
                        this.removePooledConn(p, DESC_RM_IDLE);
                        this.tryWakeupServantThread();
                    }
                } else if (state == CON_USING) {
                    if (System.currentTimeMillis() - p.lastAccessTime >= this.holdTimeoutMs) {//hold timeout
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
        }

        if (printRuntimeLog) {
            ConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})-{idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                  4: Pool clear/close methods(5)                                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-4.1: remove all connections from pool
    public void clear() {
        this.clear(false);
    }

    //Method-4.2: remove all connections from pool
    public void clear(boolean force) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            Log.info("BeeCP({})begin to remove connections", this.poolName);
            this.removeAllConnections(force, DESC_RM_CLEAR);
            this.poolState = POOL_READY;// restore state;
            Log.info("BeeCP({})all connections were removed and restored to accept new requests", this.poolName);
        }
    }

    //Method-4.3: remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        this.semaphore.interruptWaitingThreads();
        PoolClosedException exception = new PoolClosedException("Pool has shut down or in clearing");
        while (!this.waitQueue.isEmpty()) this.transferException(exception);

        while (this.pooledArray.length > 0) {
            PooledConnection[] array = this.pooledArray;
            for (PooledConnection p : array) {
                final int state = p.state;
                if (state == CON_IDLE) {
                    if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) this.removePooledConn(p, source);
                } else if (state == CON_USING) {
                    ProxyConnectionBase proxyInUsing = p.proxyInUsing;
                    if (proxyInUsing != null) {
                        if (force || System.currentTimeMillis() - p.lastAccessTime >= this.holdTimeoutMs) {//force close or hold timeout
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
            if (this.pooledArray.length > 0) LockSupport.parkNanos(this.delayTimeForNextClearNs);
        } // while

        if (printRuntimeLog) {
            ConnectionPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeCP({})-{idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //Method-4.4: closed check
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //Method-4.5: close pool
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if ((poolStateCode == POOL_NEW || poolStateCode == POOL_READY) && PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSED)) {
                Log.info("BeeCP({})begin to shutdown", this.poolName);
                this.shutdownPoolThread();
                this.unregisterJmx();
                this.removeAllConnections(this.poolConfig.isForceCloseUsingOnClear(), DESC_RM_DESTROY);
                if (networkTimeoutExecutor != null) {
                    networkTimeoutExecutor.getQueue().clear();
                    networkTimeoutExecutor.shutdownNow();
                }

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }
                Log.info("BeeCP({})has shutdown", this.poolName);
                break;
            } else if (poolStateCode == POOL_CLOSED) {
                break;
            } else {
                LockSupport.parkNanos(this.delayTimeForNextClearNs);// default wait 3 seconds
            }
        } while (true);
    }


    //***************************************************************************************************************//
    //                                  5: Pool monitor/jmx methods(15)                                              //                                                                                  //
    //***************************************************************************************************************//
    //Method-5.1: set pool info debug switch
    public void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    //Method-5.2: size of all pooled connections
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

    //Method-5.5: waiting size for semaphore
    public int getSemaphoreWaitingSize() {
        return this.semaphore.getQueueLength();
    }

    //Method-5.6: using size of semaphore permit
    public int getSemaphoreAcquiredSize() {
        return this.poolConfig.getBorrowSemaphoreSize() - this.semaphore.availablePermits();
    }

    //Method-5.7: waiting size in transfer queue
    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : this.waitQueue)
            if (borrower.state instanceof BorrowerState) size++;
        return size;
    }

    //Method-5.8: register pool to jmx
    private void registerJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName), this.poolConfig);
        }
    }

    //Method-5.9: jmx register
    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeCP({})failed to register jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.10: pool unregister from jmx
    private void unregisterJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", this.poolName));
        }
    }

    //Method-5.11: jmx unregister
    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeCP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.12: pooledConnection valid test method by connection method 'isValid'
    public final boolean isValid(final PooledConnection p) {
        try {
            if (p.rawConn.isValid(this.validTestTimeout)) {
                p.lastAccessTime = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            if (this.printRuntimeLog)
                Log.warn("BeeCP({})failed to test connection with 'isValid' method", this.poolName, e);
        }
        return false;
    }

    //Method-5.13 create monitor vo
    private ConnectionPoolMonitorVo createPoolMonitorVo() {
        Thread currentThread = Thread.currentThread();
        this.poolThreadId = currentThread.getId();
        this.poolThreadName = currentThread.getName();

        try {
            this.poolHostIP = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            Log.info("BeeCP({})failed to resolve pool hose ip", this.poolName);
        }

        return new ConnectionPoolMonitorVo();
    }

    //Method-5.14: pool monitor vo
    public ConnectionPoolMonitorVo getPoolMonitorVo() {
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
        private final Random seqRandom = new Random();

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, "networkTimeoutRestThread-" + this.seqRandom.nextInt());
            th.setDaemon(true);
            return th;
        }
    }

    //class-6.2:Semaphore extend
    private static final class PoolSemaphore extends Semaphore {
        PoolSemaphore(int permits, boolean fair) {
            super(permits, fair);
        }

        void interruptWaitingThreads() {
            for (Thread thread : getQueuedThreads()) {
                Thread.State state = thread.getState();
                if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                    thread.interrupt();
                }
            }
        }
    }

    //class-6.3:Idle scan thread
    private static final class IdleTimeoutScanThread extends Thread {
        private final FastConnectionPool pool;
        private final AtomicInteger idleScanState;

        IdleTimeoutScanThread(FastConnectionPool pool) {
            this.pool = pool;
            idleScanState = pool.idleScanState;
        }

        public void run() {
            long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(this.pool.poolConfig.getTimerCheckInterval());
            while (this.idleScanState.get() == THREAD_WORKING) {
                LockSupport.parkNanos(checkTimeIntervalNanos);
                try {
                    this.pool.closeIdleTimeoutConnection();
                } catch (Throwable e) {
                    Log.warn("BeeCP({})Error at closing idle timeout connections,cause:", this.pool.poolName, e);
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
            try {
                Log.info("BeeCP({})ConnectionPoolHook Running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeCP({})Error at closing connection pool,cause:", this.pool.poolName, e);
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

    //class-6.6:PooledConnection Valid Test
    private final class PooledConnectionValidTestBySql implements PooledConnectionValidTest {
        private final String testSql;
        private final boolean isDefaultAutoCommit;
        private final boolean supportQueryTimeout;

        PooledConnectionValidTestBySql(String testSql, boolean isDefaultAutoCommit, boolean supportQueryTimeout) {
            this.testSql = testSql;
            this.isDefaultAutoCommit = isDefaultAutoCommit;
            this.supportQueryTimeout = supportQueryTimeout;
        }

        public final boolean isValid(PooledConnection p) {
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

                //step2: create statement
                st = rawConn.createStatement();
                if (this.supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(validTestTimeout);
                    } catch (Throwable e) {
                        if (printRuntimeLog)
                            Log.warn("BeeCP({})failed to setQueryTimeout", poolName, e);
                    }
                }

                //step3: execute test sql
                try {
                    st.execute(this.testSql);
                    p.lastAccessTime = System.currentTimeMillis();
                } finally {
                    rawConn.rollback();
                }
            } catch (Throwable e) {
                checkPassed = false;
                if (printRuntimeLog)
                    Log.warn("BeeCP({})failed to test connection by sql", poolName, e);
            } finally {
                if (st != null) oclose(st);
                if (changed) {
                    try {
                        rawConn.setAutoCommit(true);
                    } catch (Throwable e) {
                        Log.warn("BeeCP({})failed to rest autoCommit to default value:true after sql-test", poolName, e);
                        checkPassed = false;
                    }
                }
            }

            return checkPassed;
        }
    }
}
