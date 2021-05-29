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
import java.util.ArrayList;
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
    private final Object connArrayLock = new Object();
    private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
    private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
    private final ConnectionPoolMonitorVo monitorVo = new ConnectionPoolMonitorVo();
    private int poolMaxSize;
    private long maxWaitNanos;//nanoseconds
    private int conUnCatchStateCode;
    private long conTestInterval;//milliseconds
    private long delayTimeForNextClearNanos;//nanoseconds
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
    private String poolName = "";
    private String poolMode = "";
    private AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
    private AtomicInteger needAddConSize = new AtomicInteger(0);
    private AtomicInteger idleThreadState = new AtomicInteger(THREAD_WORKING);

    private boolean isFirstValidConnection = true;
    private PooledConnection clonePooledConn;
    /******************************************************************************************
     *                                                                                        *
     *                 1: Pool initialize and Pooled connection create/remove methods         *
     *                                                                                        *
     ******************************************************************************************/

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
            poolName = poolConfig.getPoolName();
            commonLog.info("BeeCP({})starting....", poolName);
            poolMaxSize = poolConfig.getMaxActive();
            conFactory = poolConfig.getConnectionFactory();

            maxWaitNanos = MILLISECONDS.toNanos(poolConfig.getMaxWait());
            delayTimeForNextClearNanos = MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
            conTestInterval = poolConfig.getConnectionTestInterval();
            if (poolConfig.isFairMode()) {
                poolMode = "fair";
                transferPolicy = new FairTransferPolicy();
            } else {
                poolMode = "compete";
                transferPolicy = new CompeteTransferPolicy();
            }
            conUnCatchStateCode = transferPolicy.getCheckStateCode();
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
            ClassLoader classLoader = getClass().getClassLoader();
            ArrayList<String> classNameList = new ArrayList<String>(10);
            classNameList.add("cn.beecp.pool.Borrower");
            classNameList.add("cn.beecp.pool.PooledConnection");
            classNameList.add("cn.beecp.pool.ProxyConnection");
            classNameList.add("cn.beecp.pool.ProxyStatement");
            classNameList.add("cn.beecp.pool.ProxyPsStatement");
            classNameList.add("cn.beecp.pool.ProxyCsStatement");
            classNameList.add("cn.beecp.pool.ProxyDatabaseMetaData");
            classNameList.add("cn.beecp.pool.ProxyResultSet");
            for (int i = 0, l = classNameList.size(); i < l; i++)
                Class.forName(classNameList.get(i), false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Jdbc proxy classes missed", e);
        }
    }

    /**
     * create specified size connections at pool initialization,
     * if zero,then try to create one
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

    //create one pooled connection
    private final PooledConnection createPooledConn(int state) throws SQLException {
        synchronized (connArrayLock) {
            int arrayLen = conArray.length;
            if (arrayLen < poolMaxSize) {
                if (isDebugEnabled)
                    commonLog.debug("BeeCP({}))begin to create a new pooled connection,state:{}", poolName, state);
                Connection con;
                try {
                    con = conFactory.create();
                } catch (Throwable e) {
                    throw new ConnectionCreateFailedException(e);
                }

                try {
                    if (isFirstValidConnection) testFirstConnection(con);
                    PooledConnection pCon = clonePooledConn.clone(con, state);
                    if (isDebugEnabled)
                        commonLog.debug("BeeCP({}))has created a new pooled connection:{},state:{}", poolName, pCon, state);
                    PooledConnection[] arrayNew = new PooledConnection[arrayLen + 1];
                    arraycopy(conArray, 0, arrayNew, 0, arrayLen);
                    arrayNew[arrayLen] = pCon;// tail
                    conArray = arrayNew;
                    return pCon;
                } catch (Throwable e) {
                    oclose(con);
                    throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
                }
            } else {
                return null;
            }
        }
    }

    //remove one pooled connection
    private void removePooledConn(PooledConnection pCon, String removeType) {
        if (isDebugEnabled)
            commonLog.debug("BeeCP({}))begin to remove pooled connection:{},reason:{}", poolName, pCon, removeType);

        pCon.onBeforeRemove();
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

    private void testFirstConnection(Connection rawCon) throws SQLException {
        int defaultNetworkTimeout = 0;
        boolean supportNetworkTimeout = true;
        try {//test networkTimeout
            defaultNetworkTimeout = rawCon.getNetworkTimeout();
            if (defaultNetworkTimeout < 0) {
                supportNetworkTimeout = false;
                commonLog.warn("BeeCP({})driver not support 'networkTimeout'", poolName);
            } else {
                rawCon.setNetworkTimeout(poolTaskExecutor, defaultNetworkTimeout);
            }
        } catch (Throwable e) {
            supportNetworkTimeout = false;
            if (isDebugEnabled)
                commonLog.debug("BeeCP({})driver not support 'networkTimeout',cause:", poolName, e);
            else
                commonLog.warn("BeeCP({})driver not support 'networkTimeout'", poolName);
        }

        int defaultTransactionIsolation = poolConfig.getDefaultTransactionIsolationCode();
        if (defaultTransactionIsolation == -999) defaultTransactionIsolation = rawCon.getTransactionIsolation();
        this.clonePooledConn = new PooledConnection(this,
                poolConfig.isDefaultAutoCommit(),
                poolConfig.isDefaultReadOnly(),
                poolConfig.getDefaultCatalog(),
                poolConfig.getDefaultSchema(),
                defaultTransactionIsolation,
                supportNetworkTimeout,
                defaultNetworkTimeout,
                poolTaskExecutor);

        boolean validTestFailed;
        int connectionTestTimeout=poolConfig.getConnectionTestTimeout();
        try {//test isValid Method
            if (rawCon.isValid(connectionTestTimeout)) {
                this.conTester = new ConnValidTester(poolName, connectionTestTimeout);
                return;
            } else {
                validTestFailed = true;
                commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
            }
        } catch (Throwable e) {
            validTestFailed = true;
            if (isDebugEnabled)
                commonLog.debug("BeeCP({})driver not support 'isValid',cause:", poolName, e);
            else
                commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
        } finally {
            this.isFirstValidConnection = false;//remark as tested
        }

        if (validTestFailed) {
            Statement st = null;
            this.conTester = new SqlQueryTester(poolName, connectionTestTimeout,
                    poolConfig.isDefaultAutoCommit(), poolConfig.getConnectionTestSql());
            try {
                st = rawCon.createStatement();
                testQueryTimeout(st, connectionTestTimeout);
                validateTestSql(rawCon, st);
            } finally {
                if (st != null) oclose(st);
            }
        }
    }

    private void testQueryTimeout(Statement st, int timeoutSeconds) {
        try {
            st.setQueryTimeout(timeoutSeconds);
        } catch (Throwable e) {
            ((SqlQueryTester) conTester).setSupportQueryTimeout(false);
            if (isDebugEnabled)
                commonLog.debug("BeeCP({})driver not support 'queryTimeout',cause:", poolName, e);
            else
                commonLog.warn("BeeCP({})driver not support 'queryTimeout'", poolName);
        }
    }

    private void validateTestSql(Connection rawCon, Statement st) throws SQLException {
        boolean changed = false;
        try {
            if (poolConfig.isDefaultAutoCommit()) {
                rawCon.setAutoCommit(false);
                changed = true;
            }
            st.execute(poolConfig.getConnectionTestSql());
        } finally {
            try {
                rawCon.rollback();//why? maybe store procedure in test sql
                if (changed) rawCon.setAutoCommit(poolConfig.isDefaultAutoCommit());//reset to default
            } catch (Throwable e) {
                throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
            }
        }
    }

    /**
     * notify creator(asyn)to add one connection to pool
     */
    private void tryToCreateNewConnByAsyn() {
        int curAddSize, updAddSize;
        do {
            curAddSize = needAddConSize.get();
            updAddSize = curAddSize + 1;
            if (conArray.length + updAddSize > poolMaxSize) return;
            if (needAddConSize.compareAndSet(curAddSize, updAddSize)) {
                poolTaskExecutor.execute(dynAddPooledConnTask);
                return;
            }
        } while (true);
    }

    /******************************************************************************************
     *                                                                                        *
     *                 2: Pooled connection borrow and release methods                        *
     *                                                                                        *
     ******************************************************************************************/

    /**
     * Get one idle connection from pool,if not found,then wait util other borrower release one or wait timeout
     *
     * @return pooled connection
     * @throws SQLException if failed(create failed,interrupt,wait timeout),then throw failed cause exception
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

        final long deadlineNanos = nanoTime() + maxWaitNanos;
        try {
            if (!semaphore.tryAcquire(maxWaitNanos, NANOSECONDS))
                throw RequestTimeoutException;
        } catch (InterruptedException e) {
            throw RequestInterruptException;
        }
        try {//semaphore acquired
            //1:try to search one from array
            PooledConnection[] array = conArray;
            int i = 0, l = array.length;
            PooledConnection pCon;
            while (i < l) {
                pCon = array[i++];
                if (pCon.state == CON_IDLE && ConStUpd.compareAndSet(pCon, CON_IDLE, CON_USING) && testOnBorrow(pCon))
                    return createProxyConnection(pCon, borrower);
            }
            //2:try to create one directly
            if (conArray.length < poolMaxSize && (pCon = createPooledConn(CON_USING)) != null)
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
     * Connection return to pool after it end use,if exist waiter in pool,
     * then try to transfer the connection to one waiting borrower
     *
     * @param pCon target connection need release
     */
    public final void recycle(PooledConnection pCon) {
        transferPolicy.beforeTransfer(pCon);
        Iterator<Borrower> iterator = waitQueue.iterator();
        W:
        while (iterator.hasNext()) {
            Borrower borrower = iterator.next();
            Object state;
            do {
                if (pCon.state != conUnCatchStateCode) return;
                state = borrower.state;
                if (!(state instanceof BorrowerState)) continue W;
            } while (!BorrowStUpd.compareAndSet(borrower, state, pCon));
            if (state == BOWER_WAITING) unpark(borrower.thread);
            return;
        }//first while loop
        transferPolicy.onFailedTransfer(pCon);
    }

    /**
     * Connection create failed by creator,then transfer the failed cause exception to one waiting borrower,
     * which will end wait and throw the exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(SQLException e) {
        Iterator<Borrower> iterator = waitQueue.iterator();
        W:
        while (iterator.hasNext()) {
            Borrower borrower = iterator.next();
            Object state;
            do {
                state = borrower.state;
                if (!(state instanceof BorrowerState)) continue W;
            } while (!BorrowStUpd.compareAndSet(borrower, state, e));
            if (state == BOWER_WAITING) unpark(borrower.thread);
            return;
        }//first while loop
    }

    /**
     * When exception occur on return,then remove it from pool
     *
     * @param pCon target connection need release
     */
    final void abandonOnReturn(PooledConnection pCon) {
        removePooledConn(pCon, DESC_RM_BAD);
        tryToCreateNewConnByAsyn();
    }

    /**
     * Check one borrowed connection alive state,if not alive,then remove it from pool
     *
     * @return boolean, true:alive
     */
    private final boolean testOnBorrow(PooledConnection pCon) {
        if (currentTimeMillis() - pCon.lastAccessTime - conTestInterval >= 0L && !conTester.isAlive(pCon)) {
            removePooledConn(pCon, DESC_RM_BAD);
            tryToCreateNewConnByAsyn();
            return false;
        } else {
            return true;
        }
    }


    /******************************************************************************************
     *                                                                                        *
     *              3: Pooled connection idle-timeout/hold-timeout scan methods               *
     *                                                                                        *
     ******************************************************************************************/
    private final boolean existBorrower() {
        return semaphoreSize > semaphore.availablePermits();
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
                    if (currentTimeMillis() - pCon.lastAccessTime - poolConfig.getHoldTimeout() >= 0L) {//hold timeout
                        ProxyConnectionBase proxyConn = pCon.proxyCon;
                        if (proxyConn != null) {
                            oclose(proxyConn);
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
                commonLog.debug("BeeCP({})-{idle:{},using:{},semaphore-waiter:{},wait-transfer:{}}", poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaiterSize(), vo.getTransferWaiterSize());
        }
    }

    /******************************************************************************************
     *                                                                                        *
     *                       4: Pool clear/close methods                                      *
     *                                                                                        *
     ******************************************************************************************/

    // remove all connections from pool
    public void clearAllConnections() {
        clearAllConnections(false);
    }

    //remove all connections from pool
    public void clearAllConnections(boolean force) {
        if (poolState.compareAndSet(POOL_NORMAL, POOL_CLEARING)) {
            commonLog.info("BeeCP({})begin to remove connections", poolName);
            removeAllConnections(force, DESC_RM_CLEAR);
            commonLog.info("BeeCP({})all connections were removed", poolName);
            poolState.set(POOL_NORMAL);// restore state;
            commonLog.info("BeeCP({})restore to accept new requests", poolName);
        }
    }

    //remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        while (existBorrower()) transferException(PoolCloseException);

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
                        if (force || currentTimeMillis() - pCon.lastAccessTime - poolConfig.getHoldTimeout() >= 0L)//force close or hold timeout
                            oclose(proxyConn);
                    } else {
                        removePooledConn(pCon, source);
                    }
                }
            } // for
            if (conArray.length > 0) parkNanos(delayTimeForNextClearNanos);
        } // while
    }

    public boolean isClosed() {
        return poolState.get() == POOL_CLOSED;
    }

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
                parkNanos(delayTimeForNextClearNanos);// default wait 3 seconds
            }
        } while (true);
    }


    /******************************************************************************************
     *                                                                                        *
     *                        5: Pool monitor/jmx methods                                     *
     *                                                                                        *
     ******************************************************************************************/

    public ConnectionPoolMonitorVo getMonitorVo() {
        int totSize = getConnTotalSize();
        int idleSize = getConnIdleSize();
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolState(poolState.get());
        monitorVo.setMaxActive(poolMaxSize);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaiterSize(getSemaphoreWaitingSize());
        monitorVo.setTransferWaiterSize(getTransferWaitingSize());
        return monitorVo;
    }

    public int getConnTotalSize() {
        return conArray.length;
    }

    public int getConnIdleSize() {
        int idleSize = 0;
        PooledConnection[] array = conArray;
        for (int i = 0, l = array.length; i < l; i++)
            if (array[i].state == CON_IDLE) idleSize++;
        return idleSize;
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
        Iterator<Borrower> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            Borrower borrower = iterator.next();
            if (borrower.state instanceof BorrowerState) size++;
        }
        return size;
    }

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


    /******************************************************************************************
     *                                                                                        *
     *                        6: Pool some inner classes                                      *
     *                                                                                        *
     ******************************************************************************************/

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

        public final void beforeTransfer(PooledConnection p) {
            p.state = CON_IDLE;
        }

        public final boolean tryCatch(PooledConnection p) {
            return ConStUpd.compareAndSet(p, CON_IDLE, CON_USING);
        }

        public final void onFailedTransfer(PooledConnection p) {
        }
    }

    private static final class FairTransferPolicy implements TransferPolicy {
        public final int getCheckStateCode() {
            return CON_USING;
        }

        public final void beforeTransfer(PooledConnection p) {
        }

        public final boolean tryCatch(PooledConnection p) {
            return p.state == CON_USING;
        }

        public final void onFailedTransfer(PooledConnection p) {
            p.state = CON_IDLE;
        }
    }

    //Connection alive tester
    private static abstract class ConnectionTester {
        protected final String poolName;
        protected final int ConTestTimeout;//seconds

        public ConnectionTester(String poolName, int ConTestTimeout) {
            this.poolName = poolName;
            this.ConTestTimeout = ConTestTimeout;
        }

        abstract boolean isAlive(PooledConnection pCon);
    }

    private static final class SqlQueryTester extends ConnectionTester {
        private final String testSql;
        private final boolean autoCommit;//connection default value
        private boolean supportQueryTimeout = true;

        public SqlQueryTester(String poolName, int ConTestTimeout, boolean autoCommit, String testSql) {
            super(poolName, ConTestTimeout);
            this.autoCommit = autoCommit;
            this.testSql = testSql;
        }

        public void setSupportQueryTimeout(boolean supportQueryTimeout) {
            this.supportQueryTimeout = supportQueryTimeout;
        }

        public final boolean isAlive(PooledConnection pCon) {
            Statement st = null;
            boolean changed = false;
            Connection con = pCon.rawCon;
            try {
                if (autoCommit) {
                    con.setAutoCommit(false);
                    changed = true;
                }
                st = con.createStatement();
                if (supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(ConTestTimeout);
                    } catch (Throwable e) {
                        commonLog.error("BeeCP({})failed to setQueryTimeout", poolName, e);
                    }
                }
                st.execute(testSql);
                pCon.lastAccessTime = currentTimeMillis();
                return true;
            } catch (Throwable e) {
                commonLog.error("BeeCP({})failed to test connection", poolName, e);
                return false;
            } finally {
                try {
                    /**
                     *  for example: select xxx() from dual
                     *  a store procedure (insert 100 records to db and failed on 99), if not rollback,what will happen?
                     */
                    con.rollback();
                    if (st != null) oclose(st);
                    if (changed) con.setAutoCommit(autoCommit);//reset to default
                } catch (Throwable e) {
                    commonLog.error("BeeCP({})failed to rest connection after sql test", poolName, e);
                    return false;
                }
            }
        }
    }

    //test alive with Connection.isValid(xxx) method
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
