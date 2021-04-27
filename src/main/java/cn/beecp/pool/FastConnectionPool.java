/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.ConnectionFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static cn.beecp.pool.PoolStaticCenter.*;
import static java.lang.System.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import static java.util.concurrent.locks.LockSupport.unpark;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJmxBean {
    private static final long spinForTimeoutThreshold = 1000L;
    private static final int maxTimedSpins = (Runtime.getRuntime().availableProcessors() < 2) ? 0 : 32;
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "state");
    private static final String DESC_RM_PRE_INIT = "pre_init";
    private static final String DESC_RM_INIT = "init";
    private static final String DESC_RM_BAD = "bad";
    private static final String DESC_RM_IDLE = "idle";
    private static final String DESC_RM_CLOSED = "closed";
    private static final String DESC_RM_CLEAR = "clear";
    private static final String DESC_RM_DESTROY = "destroy";
    private static final AtomicInteger poolNameIndex = new AtomicInteger(1);
    private final Object connArrayLock = new Object();
    private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
    private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
    private final ConnectionPoolMonitorVo monitorVo = new ConnectionPoolMonitorVo();
    private int PoolMaxSize;
    private long MaxWaitNanos;//nanoseconds
    private int ConUnCatchStateCode;
    private long ConTestInterval;//milliseconds
    private long DelayTimeForNextClearNanos;//nanoseconds
    private ConnectionTester conTester;
    private ConnectionPoolHook exitHook;
    private BeeDataSourceConfig poolConfig;
    private int semaphoreSize;
    private Semaphore semaphore;
    private TransferPolicy transferPolicy;
    private ConnectionFactory conFactory;
    private volatile PooledConnection[] conArray = new PooledConnection[0];
    private DynAddPooledConnTask dynAddPooledConnTask;
    private ThreadPoolExecutor poolTaskExecutor;
    private int networkTimeout;
    private boolean supportSchema = true;
    private boolean supportIsValid = true;
    private boolean supportNetworkTimeout = true;
    private boolean supportIsValidTest;
    private boolean supportNetworkTimeoutTest;
    private String poolName = "";
    private String poolMode = "";
    private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
    private AtomicInteger needAddConSize = new AtomicInteger(0);
    private AtomicInteger idleThreadState = new AtomicInteger(THREAD_WORKING);

    /**
     * initialize pool with configuration
     *
     * @param config data source configuration
     * @throws SQLException check configuration fail or to create initiated connection
     */
    public void init(BeeDataSourceConfig config) throws SQLException {
        if (poolState.get() == POOL_UNINIT) {
            checkProxyClasses();
            if (config == null) throw new SQLException("Configuration can't be null");
            poolConfig = config.check();//why need a copy here?
            poolName = !isBlank(config.getPoolName()) ? config.getPoolName() : "FastPool-" + poolNameIndex.getAndIncrement();
            commonLog.info("BeeCP({})starting....", poolName);
            PoolMaxSize = poolConfig.getMaxActive();
            conFactory = poolConfig.getConnectionFactory();
            conTester = new SQLQueryTester(poolName, poolConfig.getConnectionTestTimeout(), poolConfig.isDefaultAutoCommit(), poolConfig.getConnectionTestSQL());
            MaxWaitNanos = MILLISECONDS.toNanos(poolConfig.getMaxWait());
            DelayTimeForNextClearNanos = MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
            ConTestInterval = poolConfig.getConnectionTestInterval();
            if (poolConfig.isFairMode()) {
                poolMode = "fair";
                transferPolicy = new FairTransferPolicy();
                ConUnCatchStateCode = transferPolicy.getCheckStateCode();
            } else {
                poolMode = "compete";
                transferPolicy = new CompeteTransferPolicy();
                ConUnCatchStateCode = transferPolicy.getCheckStateCode();
            }

            semaphoreSize = poolConfig.getBorrowSemaphoreSize();
            semaphore = new Semaphore(semaphoreSize, poolConfig.isFairMode());
            dynAddPooledConnTask = new DynAddPooledConnTask();
            poolTaskExecutor = new ThreadPoolExecutor(2, 2, 5, SECONDS, new LinkedBlockingQueue<Runnable>(), new PoolThreadThreadFactory("PoolTaskThread"));
            poolTaskExecutor.allowCoreThreadTimeOut(true);
            createInitConnections(poolConfig.getInitialSize());

            exitHook = new ConnectionPoolHook();
            Runtime.getRuntime().addShutdownHook(exitHook);
            registerJmx();
            commonLog.info("BeeCP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms,driver:{}}",
                    poolName,
                    poolMode,
                    conArray.length,
                    config.getMaxActive(),
                    semaphoreSize,
                    poolConfig.getMaxWait(),
                    poolConfig.getDriverClassName());
            poolState.set(POOL_NORMAL);

            this.setName("IdleTimeoutScanThread");
            this.setDaemon(true);
            this.start();
        } else {
            throw new SQLException("Pool has initialized");
        }
    }

    /**
     * check some proxy classes whether exists
     */
    private void checkProxyClasses() throws SQLException {
        try {
            boolean classInitialize = false;
            ClassLoader classLoader = getClass().getClassLoader();
            Class.forName("cn.beecp.pool.Borrower", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.PooledConnection", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyConnection", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyStatement", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyPsStatement", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyCsStatement", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyDatabaseMetaData", classInitialize, classLoader);
            Class.forName("cn.beecp.pool.ProxyResultSet", classInitialize, classLoader);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Jdbc proxy classes missed", e);
        }
    }

    boolean supportIsValid() {
        return supportIsValid;
    }

    boolean supportSchema() {
        return supportSchema;
    }

    boolean supportNetworkTimeout() {
        return supportNetworkTimeout;
    }

    int getNetworkTimeout() {
        return networkTimeout;
    }

    ThreadPoolExecutor getNetworkTimeoutExecutor() {
        return poolTaskExecutor;
    }

    private final boolean existBorrower() {
        return semaphoreSize > semaphore.availablePermits();
    }

    //create Pooled connection
    private final PooledConnection createPooledConn(int conState) throws SQLException {
        synchronized (connArrayLock) {
            int arrayLen = conArray.length;
            if (arrayLen < PoolMaxSize) {
                if (isDebugEnabled)
                    commonLog.debug("BeeCP({}))begin to create a new pooled connection,state:{}", poolName, conState);
                Connection con = null;
                try {
                    con = conFactory.create();
                } catch (Throwable e) {
                    throw new ConnectionCreateFailedException(e);
                }
                try {
                    setDefaultOnRawConn(con);
                } catch (Throwable e) {
                    oclose(con);
                    throw e;
                }
                PooledConnection pCon = new PooledConnection(con, conState, this, poolConfig);// registerStatement
                if (isDebugEnabled)
                    commonLog.debug("BeeCP({}))has created a new pooled connection:{},state:{}", poolName, pCon, conState);
                PooledConnection[] arrayNew = new PooledConnection[arrayLen + 1];
                arraycopy(conArray, 0, arrayNew, 0, arrayLen);
                arrayNew[arrayLen] = pCon;// tail
                conArray = arrayNew;
                return pCon;
            } else {
                return null;
            }
        }
    }

    //remove Pooled connection
    private void removePooledConn(PooledConnection pCon, String removeType) {
        if (isDebugEnabled)
            commonLog.debug("BeeCP({}))begin to remove pooled connection:{},reason:{}", poolName, pCon, removeType);
        pCon.state = CON_CLOSED;
        pCon.closeRawConn();
        synchronized (connArrayLock) {
            int oLen = conArray.length;
            PooledConnection[] arrayNew = new PooledConnection[oLen - 1];
            for (int i = 0; i < oLen; i++) {
                if (conArray[i] == pCon) {
                    arraycopy(conArray, 0, arrayNew, 0, i);
                    int m = oLen - i - 1;
                    if (m > 0) arraycopy(conArray, i + 1, arrayNew, i, m);
                    break;
                }
            }
            if (isDebugEnabled)
                commonLog.debug("BeeCP({}))has removed pooled connection:{},reason:{}", poolName, pCon, removeType);
            conArray = arrayNew;
        }
    }

    private final void setDefaultOnRawConn(Connection rawCon) throws SQLException {
        rawCon.setAutoCommit(poolConfig.isDefaultAutoCommit());
        rawCon.setTransactionIsolation(poolConfig.getDefaultTransactionIsolationCode());
        rawCon.setReadOnly(poolConfig.isDefaultReadOnly());
        if (!isBlank(poolConfig.getDefaultCatalog()))
            rawCon.setCatalog(poolConfig.getDefaultCatalog());
        if (!isBlank(poolConfig.getDefaultSchema()))
            rawCon.setSchema(poolConfig.getDefaultSchema());

        /**************************************test method start ********************************/
        if (!supportNetworkTimeoutTest) {//test networkTimeout
            try {//set networkTimeout
                this.networkTimeout = rawCon.getNetworkTimeout();
                if (networkTimeout < 0) {
                    supportNetworkTimeout = false;
                    commonLog.warn("BeeCP({})driver not support 'networkTimeout'", poolName);
                } else {
                    rawCon.setNetworkTimeout(poolTaskExecutor, networkTimeout);
                }
            } catch (Throwable e) {
                supportNetworkTimeout = false;
                if (isDebugEnabled)
                    commonLog.debug("BeeCP({})driver not support 'networkTimeout',cause:", poolName, e);
                else
                    commonLog.warn("BeeCP({})driver not support 'networkTimeout'", poolName);
            } finally {
                supportNetworkTimeoutTest = true;//remark as tested
            }
        }
        if (!this.supportIsValidTest) {//test 'connection.isValid' method
            try {
                if (rawCon.isValid(poolConfig.getConnectionTestTimeout())) {
                    this.conTester = new ConnValidTester(poolName, poolConfig.getConnectionTestTimeout());
                } else {
                    supportIsValid = false;
                    commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
                }
            } catch (Throwable e) {
                supportIsValid = false;
                if (isDebugEnabled)
                    commonLog.debug("BeeCP({})driver not support 'isValid',cause:", poolName, e);
                else
                    commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
            } finally {
                supportIsValidTest = true;//remark as tested
            }

            if (!supportIsValid) {
                Statement st = null;
                try {
                    st = rawCon.createStatement();
                    testQueryTimeout(st, poolConfig.getConnectionTestTimeout());
                    validTestSql(rawCon, st);
                } finally {
                    if (st != null) oclose(st);
                }
            }
        }
        /**************************************test method end ********************************/
    }

    private void testQueryTimeout(Statement st, int timeoutSeconds) {
        try {
            st.setQueryTimeout(timeoutSeconds);
        } catch (Throwable e) {
            ((SQLQueryTester) conTester).setSupportQueryTimeout(false);
            if (isDebugEnabled)
                commonLog.debug("BeeCP({})driver not support 'queryTimeout',cause:", poolName, e);
            else
                commonLog.warn("BeeCP({})driver not support 'queryTimeout'", poolName);
        }
    }

    private void validTestSql(Connection rawCon, Statement st) throws SQLException {
        boolean changed = false;
        try {
            if (poolConfig.isDefaultAutoCommit()) {
                rawCon.setAutoCommit(false);
                changed = true;
            }
            st.execute(poolConfig.getConnectionTestSQL());
            rawCon.rollback();//why? maybe store procedure in test sql
        } finally {//
            if (changed) {//reset to default
                try {
                    rawCon.setAutoCommit(poolConfig.isDefaultAutoCommit());
                } catch (Throwable e) {
                    throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
                }
            }
        }
    }

    /**
     * check connection state
     *
     * @return if the checked connection is active then return true,otherwise
     * false if false then close it
     */
    private final boolean testOnBorrow(PooledConnection pCon) {
        if (currentTimeMillis() - pCon.lastAccessTime - ConTestInterval >= 0L && !conTester.isAlive(pCon)) {
            removePooledConn(pCon, DESC_RM_BAD);
            tryToCreateNewConnByAsyn();
            return false;
        } else {
            return true;
        }
    }

    /**
     * create initialization connections
     *
     * @throws SQLException error occurred in creating connections
     */
    private void createInitConnections(int initSize) throws SQLException {
        try {
            int size = (initSize > 0) ? initSize : 1;
            for (int i = 0; i < size; i++)
                createPooledConn(CON_IDLE);
        } catch (Throwable e) {
            for (PooledConnection pCon : conArray)
                removePooledConn(pCon, DESC_RM_INIT);
            if (e instanceof ConnectionCreateFailedException) {//may be network bad or database is not ready
                if (initSize > 0) throw e;
            } else {
                throw e;
            }
        }
    }

    /**
     * borrow one connection from pool
     *
     * @return If exists idle connection in pool,then return one;if not, waiting
     * until other borrower release
     * @throws SQLException if pool is closed or waiting timeout,then throw exception
     */
    public final Connection getConnection() throws SQLException {
        if (poolState.get() != POOL_NORMAL) throw PoolCloseException;
        //0:try to get from threadLocal cache
        WeakReference<Borrower> ref = threadLocal.get();
        Borrower borrower = (ref != null) ? ref.get() : null;
        if (borrower != null) {
            PooledConnection pCon = borrower.lastUsedCon;
            if (pCon != null && pCon.state == CON_IDLE && ConStUpd.compareAndSet(pCon, CON_IDLE, CON_USING)) {
                if (testOnBorrow(pCon)) return createProxyConnection(pCon, borrower);
                borrower.lastUsedCon = null;
            }
        } else {
            borrower = new Borrower();
            threadLocal.set(new WeakReference<Borrower>(borrower));
        }

        final long deadlineNanos = nanoTime() + MaxWaitNanos;
        try {
            if (!semaphore.tryAcquire(MaxWaitNanos, NANOSECONDS))
                throw RequestTimeoutException;
        } catch (InterruptedException e) {
            throw RequestInterruptException;
        }
        try {//semaphore acquired
            //1:try to search one from array
            PooledConnection pCon;
            PooledConnection[] array = conArray;
            for (int i = 0, l = array.length; i < l; i++) {
                pCon = array[i];
                if (pCon.state == CON_IDLE && ConStUpd.compareAndSet(pCon, CON_IDLE, CON_USING) && testOnBorrow(pCon))
                    return createProxyConnection(pCon, borrower);
            }
            //2:try to create one directly
            if (conArray.length < PoolMaxSize && (pCon = createPooledConn(CON_USING)) != null)
                return createProxyConnection(pCon, borrower);

            //3:try to get one transferred connection
            boolean failed = false;
            SQLException cause = null;
            Thread cth = borrower.thread;
            borrower.state = BOWER_NORMAL;
            waitQueue.offer(borrower);
            int spinSize = (waitQueue.peek() == borrower) ? maxTimedSpins : 0;
            do {
                Object state = borrower.state;
                if (state instanceof PooledConnection) {
                    pCon = (PooledConnection) state;
                    if (transferPolicy.tryCatch(pCon) && testOnBorrow(pCon)) {
                        waitQueue.remove(borrower);
                        return createProxyConnection(pCon, borrower);
                    }
                } else if (state instanceof SQLException) {
                    waitQueue.remove(borrower);
                    throw (SQLException) state;
                }
                if (failed) {
                    if (borrower.state == state)
                        BorrowStUpd.compareAndSet(borrower, state, cause);
                } else if (state instanceof PooledConnection) {
                    borrower.state = BOWER_NORMAL;
                    yield();
                } else {//here:(state == BOWER_NORMAL)
                    long timeout = deadlineNanos - nanoTime();
                    if (timeout > 0L) {
                        if (spinSize > 0) {
                            --spinSize;
                        } else if (borrower.state == BOWER_NORMAL && timeout > spinForTimeoutThreshold && BorrowStUpd.compareAndSet(borrower, BOWER_NORMAL, BOWER_WAITING)) {
                            parkNanos(timeout);
                            if (cth.isInterrupted()) {
                                failed = true;
                                cause = RequestInterruptException;
                            }
                            if (borrower.state == BOWER_WAITING)
                                BorrowStUpd.compareAndSet(borrower, BOWER_WAITING, failed ? cause : BOWER_NORMAL);//reset to normal
                        }
                    } else {//timeout
                        failed = true;
                        cause = RequestTimeoutException;
                    }
                }//end (state == BOWER_NORMAL)
            } while (true);//while
        } finally {
            semaphore.release();
        }
    }

    /**
     * remove connection
     *
     * @param pCon target connection need release
     */
    final void abandonOnReturn(PooledConnection pCon) {
        removePooledConn(pCon, DESC_RM_BAD);
        tryToCreateNewConnByAsyn();
    }

    /**
     * return connection to pool
     *
     * @param pCon target connection need release
     */
    public final void recycle(PooledConnection pCon) {
        transferPolicy.beforeTransfer(pCon);
        Iterator<Borrower> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            Borrower borrower = iterator.next();
            do {
                //pooledConnection has hold by another thread
                if (pCon.state != ConUnCatchStateCode) return;
                Object state = borrower.state;
                //current waiter has received one pooledConnection or timeout
                if (!(state instanceof BorrowerState)) break;
                if (BorrowStUpd.compareAndSet(borrower, state, pCon)) {//transfer successful
                    if (state == BOWER_WAITING) unpark(borrower.thread);
                    return;
                }
            } while (true);
        }//first while loop
        transferPolicy.onFailedTransfer(pCon);
    }

    /**
     * @param e: transfer Exception to waiter
     */
    private void transferException(SQLException e) {
        Iterator<Borrower> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            Borrower borrower = iterator.next();
            do {
                Object state = borrower.state;
                //current waiter has received one pooledConnection or timeout
                if (!(state instanceof BorrowerState)) break;
                if (BorrowStUpd.compareAndSet(borrower, state, e)) {//transfer successful
                    if (state == BOWER_WAITING) unpark(borrower.thread);
                    return;
                }
            } while (true);
        }//first while loop
    }

    /**
     * inner timer will call the method to clear some idle timeout connections
     * or dead connections,or long time not active connections in using state
     */
    private void closeIdleTimeoutConnection() {
        if (poolState.get() == POOL_NORMAL) {
            PooledConnection[] array = conArray;
            for (int i = 0, len = array.length; i < len; i++) {
                PooledConnection pCon = array[i];
                int state = pCon.state;
                if (state == CON_IDLE && !existBorrower()) {
                    boolean isTimeoutInIdle = (currentTimeMillis() - pCon.lastAccessTime - poolConfig.getIdleTimeout() >= 0);
                    if (isTimeoutInIdle && ConStUpd.compareAndSet(pCon, state, CON_CLOSED)) {//need close idle
                        removePooledConn(pCon, DESC_RM_IDLE);
                        tryToCreateNewConnByAsyn();
                    }
                } else if (state == CON_USING) {
                    ProxyConnectionBase proxyConn = pCon.proxyCon;
                    boolean isHoldTimeoutInNotUsing = currentTimeMillis() - pCon.lastAccessTime - poolConfig.getHoldTimeout() >= 0;
                    if (isHoldTimeoutInNotUsing) {//recycle connection
                        if (proxyConn != null) {
                            proxyConn.trySetAsClosed();
                        } else {
                            removePooledConn(pCon, DESC_RM_BAD);
                            tryToCreateNewConnByAsyn();
                        }
                    }
                } else if (state == CON_CLOSED) {
                    removePooledConn(pCon, DESC_RM_CLOSED);
                    tryToCreateNewConnByAsyn();
                }
            }
            ConnectionPoolMonitorVo vo = this.getMonitorVo();
            if (isDebugEnabled)
                commonLog.debug("BeeCP({})idle:{},using:{},semaphore-waiter:{},wait-transfer:{}", poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaiterSize(), vo.getTransferWaiterSize());
        }
    }

    // shutdown pool
    public void close() throws SQLException {
        do {
            int poolStateCode = poolState.get();
            if ((poolStateCode == POOL_UNINIT || poolStateCode == POOL_NORMAL) && poolState.compareAndSet(poolStateCode, POOL_CLOSED)) {
                commonLog.info("BeeCP({})begin to shutdown", poolName);
                removeAllConnections(poolConfig.isForceCloseUsingOnClear(), DESC_RM_DESTROY);
                unregisterJmx();
                shutdownIdleScanThread();
                poolTaskExecutor.getQueue().clear();
                poolTaskExecutor.shutdownNow();

                try {
                    Runtime.getRuntime().removeShutdownHook(exitHook);
                } catch (Throwable e) {
                }
                commonLog.info("BeeCP({})has shutdown", poolName);
                break;
            } else if (poolState.get() == POOL_CLOSED) {
                break;
            } else {
                parkNanos(DelayTimeForNextClearNanos);// wait 3 seconds
            }
        } while (true);
    }

    public boolean isClosed() {
        return poolState.get() == POOL_CLOSED;
    }

    // remove all connections
    private void removeAllConnections(boolean force, String source) {
        while (existBorrower()) {
            transferException(PoolCloseException);
        }
        while (conArray.length > 0) {
            PooledConnection[] array = conArray;
            for (int i = 0, len = array.length; i < len; i++) {
                PooledConnection pCon = array[i];
                if (ConStUpd.compareAndSet(pCon, CON_IDLE, CON_CLOSED)) {
                    removePooledConn(pCon, source);
                } else if (pCon.state == CON_CLOSED) {
                    removePooledConn(pCon, source);
                } else if (pCon.state == CON_USING) {
                    ProxyConnectionBase proxyConn = pCon.proxyCon;
                    if (proxyConn != null) {
                        if (force) {
                            proxyConn.trySetAsClosed();
                        } else {
                            boolean isTimeout = (currentTimeMillis() - pCon.lastAccessTime - poolConfig.getHoldTimeout() >= 0);
                            if (isTimeout) proxyConn.trySetAsClosed();
                        }
                    } else {
                        removePooledConn(pCon, source);
                    }
                }
            } // for
            if (conArray.length > 0) parkNanos(DelayTimeForNextClearNanos);
        } // while
    }

    // notify to create connections to pool
    private void tryToCreateNewConnByAsyn() {
        do {
            int curAddSize = needAddConSize.get();
            int updAddSize = curAddSize + 1;
            if (conArray.length + updAddSize > PoolMaxSize) return;
            if (needAddConSize.compareAndSet(curAddSize, updAddSize)) {
                poolTaskExecutor.submit(dynAddPooledConnTask);
                return;
            }
        } while (true);
    }

    // close all connections
    public void clearAllConnections() {
        clearAllConnections(false);
    }

    // close all connections
    public void clearAllConnections(boolean force) {
        if (poolState.compareAndSet(POOL_NORMAL, POOL_CLEARING)) {
            commonLog.info("BeeCP({})begin to remove connections", poolName);
            removeAllConnections(force, DESC_RM_CLEAR);
            commonLog.info("BeeCP({})all connections were removed", poolName);
            poolState.set(POOL_NORMAL);// restore state;
            commonLog.info("BeeCP({})restore to accept new requests", poolName);
        }
    }

    public int getConnTotalSize() {
        return conArray.length;
    }

    public int getConnIdleSize() {
        int idleConnections = 0;
        for (PooledConnection pCon : this.conArray) {
            if (pCon.state == CON_IDLE)
                idleConnections++;
        }
        return idleConnections;
    }

    public int getConnUsingSize() {
        int active = conArray.length - getConnIdleSize();
        return (active > 0) ? active : 0;
    }

    public int getSemaphoreAcquiredSize() {
        return poolConfig.getBorrowSemaphoreSize() - semaphore.availablePermits();
    }

    public int getSemaphoreWaitingSize() {
        return semaphore.getQueueLength();
    }

    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : waitQueue) {
            Object state = borrower.state;
            if (state == BOWER_NORMAL || state == BOWER_WAITING)
                size++;
        }
        return size;
    }

    public ConnectionPoolMonitorVo getMonitorVo() {
        int totSize = getConnTotalSize();
        int idleSize = getConnIdleSize();
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolState(poolState.get());
        monitorVo.setMaxActive(PoolMaxSize);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaiterSize(getSemaphoreWaitingSize());
        monitorVo.setTransferWaiterSize(getTransferWaitingSize());
        return monitorVo;
    }

    private void shutdownIdleScanThread() {
        idleThreadState.set(THREAD_EXIT);
        unpark(this);
    }

    public void run() {
        final long CheckTimeIntervalNanos = MILLISECONDS.toNanos(poolConfig.getIdleCheckTimeInterval());
        do {
            if (idleThreadState.get() == THREAD_WORKING) {
                try {
                    parkNanos(CheckTimeIntervalNanos);
                    closeIdleTimeoutConnection();
                } catch (Throwable e) {
                }
            } else {
                break;
            }
        } while (true);
    }

    //******************************** JMX methods ********************************************************************/
    private void registerJmx() {
        if (poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            registerJmxBean(mBeanServer, String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)", poolName), this);
            registerJmxBean(mBeanServer, String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", poolName), poolConfig);
        }
    }

    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Exception e) {
            commonLog.warn("BeeCP({})failed to register jmx-bean:{}", poolName, regName, e);
        }
    }

    private void unregisterJmx() {
        if (poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            unregisterJmxBean(mBeanServer, String.format("cn.beecp.pool.FastConnectionPool:type=BeeCP(%s)", poolName));
            unregisterJmxBean(mBeanServer, String.format("cn.beecp.BeeDataSourceConfig:type=BeeCP(%s)-config", poolName));
        }
    }

    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Exception e) {
            commonLog.warn("BeeCP({})failed to unregister jmx-bean:{}", poolName, regName, e);
        }
    }
    //******************************** JMX methods ********************************************************************/


    /**********************************************Below are some inner classes******************************************************************/

    // Transfer Policy
    private static interface TransferPolicy {
        int getCheckStateCode();

        void beforeTransfer(PooledConnection p);

        boolean tryCatch(PooledConnection p);

        void onFailedTransfer(PooledConnection p);
    }

    private static final class CompeteTransferPolicy implements TransferPolicy {
        public final int getCheckStateCode() {
            return CON_IDLE;
        }

        public final boolean tryCatch(PooledConnection p) {
            return ConStUpd.compareAndSet(p, CON_IDLE, CON_USING);
        }

        public final void onFailedTransfer(PooledConnection p) {
        }

        public final void beforeTransfer(PooledConnection p) {
            p.state = CON_IDLE;
        }
    }

    private static final class FairTransferPolicy implements TransferPolicy {
        public final int getCheckStateCode() {
            return CON_USING;
        }

        public final boolean tryCatch(PooledConnection p) {
            return p.state == CON_USING;
        }

        public final void onFailedTransfer(PooledConnection p) {
            p.state = CON_IDLE;
        }

        public final void beforeTransfer(PooledConnection p) {
        }
    }

    // Connection check Policy
    private static abstract class ConnectionTester {
        protected final String poolName;
        protected final int ConTestTimeout;//seconds

        public ConnectionTester(String poolName, int ConTestTimeout) {
            this.poolName = poolName;
            this.ConTestTimeout = ConTestTimeout;
        }

        abstract boolean isAlive(PooledConnection pCon);
    }

    // SQL tester
    private static final class SQLQueryTester extends ConnectionTester {
        private final String testSql;
        private final boolean autoCommit;//connection default value
        private boolean supportQueryTimeout = true;

        public SQLQueryTester(String poolName, int ConTestTimeout, boolean autoCommit, String testSql) {
            super(poolName, ConTestTimeout);
            this.autoCommit = autoCommit;
            this.testSql = testSql;
        }

        public void setSupportQueryTimeout(boolean supportQueryTimeout) {
            this.supportQueryTimeout = supportQueryTimeout;
        }

        public final boolean isAlive(PooledConnection pCon) {
            boolean autoCommitChged = false;
            Statement st = null;
            Connection con = pCon.rawCon;
            try {
                //may be a store procedure or a function in this test sql,so need rollback finally
                //for example: select xxx() from dual
                if (autoCommit) {
                    con.setAutoCommit(false);
                    autoCommitChged = true;
                }
                st = con.createStatement();
                pCon.lastAccessTime = currentTimeMillis();
                if (supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(ConTestTimeout);
                    } catch (Throwable e) {
                        commonLog.error("BeeCP({})failed to setQueryTimeout", poolName, e);
                    }
                }
                st.execute(testSql);
                con.rollback();//why? maybe store procedure in test sql
                return true;
            } catch (Throwable e) {
                commonLog.error("BeeCP({})failed to test connection", poolName, e);
                return false;
            } finally {
                if (st != null) oclose(st);
                if (autoCommitChged) {
                    try {
                        con.setAutoCommit(autoCommit);//reset to default
                    } catch (Throwable e) {
                        commonLog.error("BeeCP({})failed to setAutoCommit(true) after connection test", poolName, e);
                    }
                }
            }
        }
    }

    //valid tester(call connection.isValid)
    private static final class ConnValidTester extends ConnectionTester {
        public ConnValidTester(String poolName, int ConTestTimeout) {
            super(poolName, ConTestTimeout);
        }

        public final boolean isAlive(PooledConnection pCon) {
            try {
                if (pCon.rawCon.isValid(ConTestTimeout)) {
                    pCon.lastAccessTime = currentTimeMillis();
                    return true;
                }
            } catch (Throwable e) {
                commonLog.error("BeeCP({})failed to test connection", poolName, e);
            }
            return false;
        }
    }

    private static final class PoolThreadThreadFactory implements ThreadFactory {
        private String thName;

        public PoolThreadThreadFactory(String thName) {
            this.thName = thName;
        }

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, thName);
            th.setDaemon(true);
            return th;
        }
    }

    //create pooled connection by asyn
    final class DynAddPooledConnTask implements Runnable {
        // create connection to pool
        public void run() {
            PooledConnection pCon;
            while (needAddConSize.get() > 0) {
                needAddConSize.decrementAndGet();
                if (!waitQueue.isEmpty()) {
                    try {
                        pCon = createPooledConn(CON_USING);
                        if (pCon != null) recycle(pCon);
                    } catch (Throwable e) {
                        transferException((e instanceof SQLException) ? (SQLException) e : new SQLException(e));
                    }
                }
            }
        }
    }

    /**
     * Hook when JVM exit
     */
    private class ConnectionPoolHook extends Thread {
        public void run() {
            try {
                commonLog.info("ConnectionPoolHook Running");
                FastConnectionPool.this.close();
            } catch (Throwable e) {
                commonLog.error("Error at closing connection pool,cause:", e);
            }
        }
    }
}
