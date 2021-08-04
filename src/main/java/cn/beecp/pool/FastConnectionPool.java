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
import static java.util.concurrent.locks.LockSupport.*;

/**
 * JDBC Connection Pool Implementation
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class FastConnectionPool extends Thread implements ConnectionPool, ConnectionPoolJmxBean, PooledConnectionTransferPolicy, PooledConnectionTester {
    private static final long spinForTimeoutThreshold = 1000L;
    private static final AtomicIntegerFieldUpdater<PooledConnection> ConStUpd = AtomicIntegerFieldUpdater.newUpdater(PooledConnection.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = AtomicReferenceFieldUpdater.newUpdater(Borrower.class, Object.class, "state");
    private static final String DESC_RM_INIT = "init";
    private static final String DESC_RM_BAD = "bad";
    private static final String DESC_RM_IDLE = "idle";
    private static final String DESC_RM_CLOSED = "closed";
    private static final String DESC_RM_CLEAR = "clear";
    private static final String DESC_RM_DESTROY = "destroy";
    private final ConcurrentLinkedQueue<Borrower> waitQueue = new ConcurrentLinkedQueue<Borrower>();
    private final ThreadLocal<WeakReference<Borrower>> threadLocal = new ThreadLocal<WeakReference<Borrower>>();
    private final ConnectionPoolMonitorVo monitorVo = new ConnectionPoolMonitorVo();
    private final AtomicInteger poolState = new AtomicInteger(POOL_UNINIT);
    private final AtomicInteger servantState = new AtomicInteger(THREAD_WORKING);
    private final AtomicInteger servantTryCount = new AtomicInteger(0);
    private final AtomicInteger idleScanState = new AtomicInteger(THREAD_WORKING);
    private final IdleTimeoutScanThread idleScanThread = new IdleTimeoutScanThread(this);
    private boolean printRuntimeLog;

    private int poolMaxSize;
    private long maxWaitNs;//nanoseconds
    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds
    private int unCatchStateCode;
    private long conTestInterval;//milliseconds
    private int connectionTestTimeout;//seconds
    private long delayTimeForNextClearNs;//nanoseconds
    private PooledConnectionTester conTester;
    private PooledConnectionTransferPolicy transferPolicy;
    private ConnectionPoolHook exitHook;
    private BeeDataSourceConfig poolConfig;
    private int semaphoreSize;
    private PoolSemaphore semaphore;
    private ConnectionFactory conFactory;
    private volatile PooledConnection[] conArray = new PooledConnection[0];

    private String poolName;
    private String poolMode;
    private CountDownLatch poolThreadLatch = new CountDownLatch(2);
    private ThreadPoolExecutor networkTimeoutExecutor;
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

            idleTimeoutMs = poolConfig.getIdleTimeout();
            holdTimeoutMs = poolConfig.getHoldTimeout();
            maxWaitNs = MILLISECONDS.toNanos(poolConfig.getMaxWait());
            delayTimeForNextClearNs = MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
            conTestInterval = poolConfig.getConnectionTestInterval();
            connectionTestTimeout = poolConfig.getConnectionTestTimeout();
            if (poolConfig.isFairMode()) {
                poolMode = "fair";
                transferPolicy = new FairTransferPolicy();
            } else {
                poolMode = "compete";
                transferPolicy = this;
            }

            printRuntimeLog = poolConfig.isEnableRuntimeLog();
            unCatchStateCode = transferPolicy.getCheckStateCode();
            semaphoreSize = poolConfig.getBorrowSemaphoreSize();
            semaphore = new PoolSemaphore(semaphoreSize, poolConfig.isFairMode());
            networkTimeoutExecutor = new ThreadPoolExecutor(1, 1, 10, SECONDS,
                    new LinkedBlockingQueue<Runnable>(), new PoolThreadThreadFactory("networkTimeoutRestThread"));
            networkTimeoutExecutor.allowCoreThreadTimeOut(true);
            createInitConnections(poolConfig.getInitialSize());

            exitHook = new ConnectionPoolHook(this);
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

            idleScanThread.setDaemon(true);
            idleScanThread.setName(poolName + "-idleCheck");
            idleScanThread.start();
            this.setDaemon(true);
            this.setName(poolName + "-workServant");
            this.start();
            poolState.set(POOL_NORMAL);
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
            String[] classNames = new String[]{
                    "cn.beecp.pool.Borrower",
                    "cn.beecp.pool.PooledConnection",
                    "cn.beecp.pool.ProxyConnection",
                    "cn.beecp.pool.ProxyStatement",
                    "cn.beecp.pool.ProxyPsStatement",
                    "cn.beecp.pool.ProxyCsStatement",
                    "cn.beecp.pool.ProxyDatabaseMetaData",
                    "cn.beecp.pool.ProxyResultSet"};
            for (String className : classNames)
                Class.forName(className, false, classLoader);
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
            for (PooledConnection p : conArray)
                removePooledConn(p, DESC_RM_INIT);
            if (e instanceof ConnectionCreateFailedException) {//may be network bad or database is not ready
                if (initSize > 0) throw e;
            } else {
                throw e;
            }
        }
    }

    //create one pooled connection
    private synchronized final PooledConnection createPooledConn(final int state) throws SQLException {
        int l = conArray.length;
        if (l < poolMaxSize) {
            if (printRuntimeLog)
                commonLog.info("BeeCP({}))begin to create a new pooled connection,state:{}", poolName, state);
            Connection con;
            try {
                con = conFactory.create();
            } catch (Throwable e) {
                throw new ConnectionCreateFailedException(e);
            }
            try {
                if (isFirstValidConnection) testFirstConnection(con);
                PooledConnection p = clonePooledConn.copy(con, state);
                if (printRuntimeLog)
                    commonLog.info("BeeCP({}))has created a new pooled connection:{},state:{}", poolName, p, state);
                PooledConnection[] arrayNew = new PooledConnection[l + 1];
                arraycopy(conArray, 0, arrayNew, 0, l);
                arrayNew[l] = p;// tail
                conArray = arrayNew;
                return p;
            } catch (Throwable e) {
                oclose(con);
                throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
            }
        } else {
            return null;
        }
    }

    //remove one pooled connection
    private synchronized void removePooledConn(final PooledConnection p, final String removeType) {
        if (printRuntimeLog)
            commonLog.info("BeeCP({}))begin to remove pooled connection:{},reason:{}", poolName, p, removeType);
        p.onBeforeRemove();
        int l = conArray.length;
        PooledConnection[] arrayNew = new PooledConnection[l - 1];
        for (int i = 0; i < l; i++) {
            if (conArray[i] == p) {
                arraycopy(conArray, 0, arrayNew, 0, i);
                int m = l - i - 1;
                if (m > 0) arraycopy(conArray, i + 1, arrayNew, i, m);
                break;
            }
        }
        if (printRuntimeLog)
            commonLog.info("BeeCP({}))has removed pooled connection:{},reason:{}", poolName, p, removeType);
        conArray = arrayNew;
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
                rawCon.setNetworkTimeout(networkTimeoutExecutor, defaultNetworkTimeout);
            }
        } catch (Throwable e) {
            supportNetworkTimeout = false;
            if (printRuntimeLog)
                commonLog.info("BeeCP({})driver not support 'networkTimeout',cause:", poolName, e);
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
                networkTimeoutExecutor);

        boolean validTestFailed;
        this.isFirstValidConnection = false;//remark as tested
        try {//test isValid Method
            if (rawCon.isValid(connectionTestTimeout)) {
                this.conTester = this;
                return;
            } else {
                validTestFailed = true;
                commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
            }
        } catch (Throwable e) {
            validTestFailed = true;
            if (printRuntimeLog)
                commonLog.info("BeeCP({})driver not support 'isValid',cause:", poolName, e);
            else
                commonLog.warn("BeeCP({})driver not support 'isValid'", poolName);
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
            if (printRuntimeLog)
                commonLog.info("BeeCP({})driver not support 'queryTimeout',cause:", poolName, e);
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
        WeakReference<Borrower> r = threadLocal.get();
        Borrower b = (r != null) ? r.get() : null;
        if (b != null) {
            PooledConnection p = b.lastUsed;
            if (p != null && p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING)) {
                if (testOnBorrow(p)) return createProxyConnection(p, b);
                b.lastUsed = null;
            }
        } else {
            b = new Borrower();
            threadLocal.set(new WeakReference<Borrower>(b));
        }

        long deadline = nanoTime();
        try {
            if (!semaphore.tryAcquire(maxWaitNs, NANOSECONDS))
                throw RequestTimeoutException;
        } catch (InterruptedException e) {
            throw RequestInterruptException;
        }
        try {//semaphore acquired
            //2:try search one or create one
            PooledConnection p = searchOrCreate();
            if (p != null) return createProxyConnection(p, b);

            //3:try to get one transferred connection
            b.state = BOWER_NORMAL;
            waitQueue.offer(b);
            boolean failed = false;
            Throwable cause = null;
            deadline += maxWaitNs;
            final Thread bth = b.thread;

            do {
                final Object s = b.state;
                if (s instanceof PooledConnection) {
                    p = (PooledConnection) s;
                    if (transferPolicy.tryCatch(p) && testOnBorrow(p)) {
                        waitQueue.remove(b);
                        return createProxyConnection(p, b);
                    }
                } else if (s instanceof Throwable) {
                    waitQueue.remove(b);
                    throw s instanceof SQLException ? (SQLException) s : new SQLException((Throwable) s);
                }
                if (failed) {
                    BorrowStUpd.compareAndSet(b, s, cause);
                } else if (s instanceof PooledConnection) {
                    b.state = BOWER_NORMAL;
                    yield();
                } else {//here:(state == BOWER_NORMAL)
                    final long t = deadline - nanoTime();
                    if (t > 0L) {
                        if (t > spinForTimeoutThreshold && BorrowStUpd.compareAndSet(b, BOWER_NORMAL, BOWER_WAITING)) {
                            if (servantTryCount.get() > 0 && servantState.get() == THREAD_WAITING && servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                                unpark(this);
                            parkNanos(t);
                            if (bth.isInterrupted()) {
                                failed = true;
                                cause = RequestInterruptException;
                            }
                            if (b.state == BOWER_WAITING)
                                BorrowStUpd.compareAndSet(b, BOWER_WAITING, failed ? cause : BOWER_NORMAL);//reset to normal
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

    private final PooledConnection searchOrCreate() throws SQLException {
        final PooledConnection[] array = conArray;
        final int l = array.length;
        for (int i = 0; i < l; ++i) {
            PooledConnection p = array[i];
            if (p.state == CON_IDLE && ConStUpd.compareAndSet(p, CON_IDLE, CON_USING) && testOnBorrow(p))
                return p;
        }
        if (conArray.length < poolMaxSize)
            return createPooledConn(CON_USING);
        return null;
    }

    private final void tryWakeupServantThread() {
        int c;
        do {
            c = servantTryCount.get();
            if (c >= poolMaxSize) return;
        } while (!servantTryCount.compareAndSet(c, c + 1));
        if (!waitQueue.isEmpty() && servantState.get() == THREAD_WAITING && servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
            unpark(this);
    }

    /**
     * Connection return to pool after it end use,if exist waiter in pool,
     * then try to transfer the connection to one waiting borrower
     *
     * @param p target connection need release
     */
    public final void recycle(final PooledConnection p) {
        final Iterator<Borrower> it = waitQueue.iterator();
        transferPolicy.beforeTransfer(p);
        W:
        while (it.hasNext()) {
            final Borrower b = it.next();
            Object s;
            do {
                s = b.state;
                if (!(s instanceof BorrowerState)) continue W;
                if (p.state != unCatchStateCode) return;
            } while (!BorrowStUpd.compareAndSet(b, s, p));
            if (s == BOWER_WAITING) unpark(b.thread);
            return;
        }
        transferPolicy.onFailedTransfer(p);
        tryWakeupServantThread();
    }

    /**
     * Connection create failed by creator,then transfer the failed cause exception to one waiting borrower,
     * which will end wait and throw the exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(final Throwable e) {
        final Iterator<Borrower> it = waitQueue.iterator();
        W:
        while (it.hasNext()) {
            final Borrower b = it.next();
            Object s;
            do {
                s = b.state;
                if (!(s instanceof BorrowerState)) continue W;
                if (BorrowStUpd.compareAndSet(b, s, e)) {
                    if (s == BOWER_WAITING) unpark(b.thread);
                    return;
                } else {//why?
                    yield();
                }
            } while (true);
        }
    }

    /**
     * When exception occur on return,then remove it from pool
     *
     * @param p target connection need release
     */
    final void abandonOnReturn(final PooledConnection p) {
        removePooledConn(p, DESC_RM_BAD);
        tryWakeupServantThread();
    }

    /**
     * Check one borrowed connection alive state,if not alive,then remove it from pool
     *
     * @return boolean, true:alive
     */
    private final boolean testOnBorrow(final PooledConnection p) {
        if (currentTimeMillis() - p.lastAccessTime - conTestInterval > 0L && !conTester.isAlive(p)) {
            removePooledConn(p, DESC_RM_BAD);
            tryWakeupServantThread();
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

    private void shutdownPoolThread() {
        int curState = idleScanState.get();
        idleScanState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) unpark(idleScanThread);

        curState = servantState.get();
        servantState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) unpark(this);
    }

    public void run() {
        poolThreadLatch.countDown();
        while (poolState.get() != POOL_CLOSED) {
            while (servantState.get() == THREAD_WORKING && servantTryCount.get() > 0) {
                try {
                    servantTryCount.decrementAndGet();
                    PooledConnection p = searchOrCreate();
                    if (p != null) recycle(p);
                } catch (Throwable e) {
                    transferException(e);
                }
            }

            servantTryCount.set(0);
            if (servantState.get() == THREAD_EXIT)
                break;
            if (servantState.compareAndSet(THREAD_WORKING, THREAD_WAITING))
                park();
        }
    }

    /**
     * inner timer will call the method to clear some idle timeout connections
     * or dead connections,or long time not active connections in using state
     */
    private void closeIdleTimeoutConnection() {
        if (poolState.get() == POOL_NORMAL) {
            PooledConnection[] array = conArray;
            for (int i = 0, l = array.length; i < l; i++) {
                PooledConnection p = array[i];
                int state = p.state;
                if (state == CON_IDLE && !existBorrower()) {
                    boolean isTimeoutInIdle = currentTimeMillis() - p.lastAccessTime - idleTimeoutMs >= 0L;
                    if (isTimeoutInIdle && ConStUpd.compareAndSet(p, state, CON_CLOSED)) {//need close idle
                        removePooledConn(p, DESC_RM_IDLE);
                        tryWakeupServantThread();
                    }
                } else if (state == CON_USING) {
                    if (currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                        ProxyConnectionBase proxyConn = p.proxyCon;
                        if (proxyConn != null) {
                            oclose(proxyConn);
                        } else {
                            removePooledConn(p, DESC_RM_BAD);
                            tryWakeupServantThread();
                        }
                    }
                } else if (state == CON_CLOSED) {
                    removePooledConn(p, DESC_RM_CLOSED);
                    tryWakeupServantThread();
                }
            }
            ConnectionPoolMonitorVo vo = this.getMonitorVo();
            if (printRuntimeLog)
                commonLog.info("BeeCP({})-{idle:{},using:{},semaphore-waiter:{},wait-transfer:{}}", poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaiterSize(), vo.getTransferWaiterSize());
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
            poolState.set(POOL_NORMAL);// restore state;
            commonLog.info("BeeCP({})all connections were removed and restored to accept new requests", poolName);
        }
    }

    //remove all connections from pool
    private void removeAllConnections(boolean force, String source) {
        semaphore.interruptWaitingThreads();
        while (!waitQueue.isEmpty()) transferException(PoolCloseException);

        while (conArray.length > 0) {
            PooledConnection[] array = conArray;
            for (int i = 0, len = array.length; i < len; i++) {
                PooledConnection p = array[i];
                if (ConStUpd.compareAndSet(p, CON_IDLE, CON_CLOSED)) {
                    removePooledConn(p, source);
                } else if (p.state == CON_CLOSED) {
                    removePooledConn(p, source);
                } else if (p.state == CON_USING) {
                    ProxyConnectionBase proxyConn = p.proxyCon;
                    if (proxyConn != null) {
                        if (force || currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L)//force close or hold timeout
                            oclose(proxyConn);
                    } else {
                        removePooledConn(p, source);
                    }
                }
            } // for
            if (conArray.length > 0) parkNanos(delayTimeForNextClearNs);
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
                shutdownPoolThread();
                unregisterJmx();
                removeAllConnections(poolConfig.isForceCloseUsingOnClear(), DESC_RM_DESTROY);
                networkTimeoutExecutor.getQueue().clear();
                networkTimeoutExecutor.shutdownNow();

                try {
                    Runtime.getRuntime().removeShutdownHook(exitHook);
                } catch (Throwable e) {
                }
                commonLog.info("BeeCP({})has shutdown", poolName);
                break;
            } else if (poolState.get() == POOL_CLOSED) {
                break;
            } else {
                parkNanos(delayTimeForNextClearNs);// default wait 3 seconds
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

    //set pool info debug switch
    public void setEnableRuntimeLog(boolean indicator) {
        this.printRuntimeLog = indicator;
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

    //private static final class CompeteTransferPolicy implements TransferPolicy {
    public final int getCheckStateCode() {
        return CON_IDLE;
    }

    public final void beforeTransfer(final PooledConnection p) {
        p.state = CON_IDLE;
    }

    public final boolean tryCatch(final PooledConnection p) {
        return ConStUpd.compareAndSet(p, CON_IDLE, CON_USING);
    }

    public final void onFailedTransfer(final PooledConnection p) {
    }

    public final boolean isAlive(final PooledConnection p) {
        try {
            if (p.raw.isValid(connectionTestTimeout)) {
                p.lastAccessTime = currentTimeMillis();
                return true;
            }
        } catch (Throwable e) {
            commonLog.error("BeeCP({})failed to test connection", poolName, e);
        }
        return false;
    }

    private static final class SqlQueryTester implements PooledConnectionTester {
        private final String poolName;
        private final int conTestTimeout;//seconds
        private final String testSql;
        private final boolean autoCommit;//connection default value
        private boolean supportQueryTimeout = true;

        public SqlQueryTester(String poolName, int ConTestTimeout, boolean autoCommit, String testSql) {
            this.poolName = poolName;
            this.conTestTimeout = ConTestTimeout;
            this.autoCommit = autoCommit;
            this.testSql = testSql;
        }

        public void setSupportQueryTimeout(boolean supportQueryTimeout) {
            this.supportQueryTimeout = supportQueryTimeout;
        }

        public final boolean isAlive(final PooledConnection p) {
            Statement st = null;
            boolean changed = false;
            final Connection con = p.raw;
            try {
                if (autoCommit) {
                    con.setAutoCommit(false);
                    changed = true;
                }
                st = con.createStatement();
                if (supportQueryTimeout) {
                    try {
                        st.setQueryTimeout(conTestTimeout);
                    } catch (Throwable e) {
                        commonLog.error("BeeCP({})failed to setQueryTimeout", poolName, e);
                    }
                }
                st.execute(testSql);
                p.lastAccessTime = currentTimeMillis();
                return true;
            } catch (Throwable e) {
                commonLog.error("BeeCP({})failed to test connection", poolName, e);
                return false;
            } finally {
                if (st != null) oclose(st);
                try {
                    con.rollback();
                    if (changed) con.setAutoCommit(autoCommit);//reset to default
                } catch (Throwable e) {
                    commonLog.error("BeeCP({})failed to rest connection after sql test", poolName, e);
                    return false;
                }
            }
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

    private static final class PoolSemaphore extends Semaphore {
        public PoolSemaphore(int permits, boolean fair) {
            super(permits, fair);
        }

        public void interruptWaitingThreads() {
            Iterator<Thread> iterator = super.getQueuedThreads().iterator();
            while (iterator.hasNext()) {
                Thread thread = iterator.next();
                State state = thread.getState();
                if (state == State.WAITING || state == State.TIMED_WAITING) {
                    thread.interrupt();
                }
            }
        }
    }

    private static final class IdleTimeoutScanThread extends Thread {
        private FastConnectionPool pool;

        public IdleTimeoutScanThread(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            pool.poolThreadLatch.countDown();
            final long checkTimeIntervalNanos = MILLISECONDS.toNanos(pool.poolConfig.getIdleCheckTimeInterval());
            final AtomicInteger idleScanThreadState = pool.idleScanState;
            while (idleScanThreadState.get() == THREAD_WORKING) {
                parkNanos(checkTimeIntervalNanos);
                try {
                    pool.closeIdleTimeoutConnection();
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
     * Hook when JVM exit
     */
    private static class ConnectionPoolHook extends Thread {
        private FastConnectionPool pool;

        public ConnectionPoolHook(FastConnectionPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                commonLog.info("BeeCP({})ConnectionPoolHook Running", pool.poolName);
                pool.close();
            } catch (Throwable e) {
                commonLog.error("BeeCP({})Error at closing connection pool,cause:", pool.poolName, e);
            }
        }
    }

    private static final class FairTransferPolicy implements PooledConnectionTransferPolicy {
        public final int getCheckStateCode() {
            return CON_USING;
        }

        public final void beforeTransfer(final PooledConnection p) {
        }

        public final boolean tryCatch(final PooledConnection p) {
            return p.state == CON_USING;
        }

        public final void onFailedTransfer(final PooledConnection p) {
            p.state = CON_IDLE;
        }
    }
}
