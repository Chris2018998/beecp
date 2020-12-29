/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.boot.monitor.sqltrace;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 *  Sql trace pool
 *
 *  @author Chris.Liao
 *
 *  spring.datasource.sql-trace=true
 *  spring.datasource.sql-show=true
 *  spring.datasource.sql-trace-size=1000
 *  spring.datasource.sql-trace-timeout=18000
 *  spring.datasource.sql-exec-alert-time=5000
 *  spring.datasource.sql-exec-alert-action=xxxxx
 */
public class SqlTracePool {
    private static final SqlTracePool instance = new SqlTracePool();
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<Field> configFields = new ArrayList<Field>();
    private final LinkedList<SqlTraceEntry> alertList = new LinkedList();
    private final AtomicInteger tracedQueueSize = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<SqlTraceEntry> traceQueue = new ConcurrentLinkedQueue<SqlTraceEntry>();
    private final ScheduledThreadPoolExecutor timeoutSchExecutor = new ScheduledThreadPoolExecutor(1, new TimeoutScanThreadThreadFactory());

    private boolean sqlTrace = true;
    private boolean sqlShow = false;
    private int sqlTraceSize = 1000;
    private long sqlTraceTimeout = TimeUnit.MINUTES.toMillis(3);
    private long sqlTraceAlertTime = TimeUnit.SECONDS.toMillis(6);
    private SqlTraceAlert sqlTraceAlert = new SqlTraceAlert();

    private SqlTracePool() {
        timeoutSchExecutor.setKeepAliveTime(15, TimeUnit.SECONDS);
        timeoutSchExecutor.allowCoreThreadTimeOut(true);
        timeoutSchExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {// check idle connection
                removeTimeoutTrace();
            }
        }, 1, 3, TimeUnit.SECONDS);

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isFinal(field.getModifiers())) {
                configFields.add(field);
            }
        }
    }

    public static final SqlTracePool getInstance() {
        return instance;
    }

    public boolean isSqlTrace() {
        return this.sqlTrace;
    }

    public void setSqlTrace(boolean sqlTrace) {
        this.sqlTrace = sqlTrace;
    }

    public void setSqlShow(boolean sqlShow) {
        this.sqlShow = sqlShow;
    }

    public void setSqlTraceSize(int sqlTraceSize) {
        if (sqlTraceSize > 0 && sqlTraceSize < 1000)
            this.sqlTraceSize = sqlTraceSize;
    }

    public void setSqlTraceTimeout(long sqlTraceTimeout) {
        if (sqlTraceTimeout > 0)
            this.sqlTraceTimeout = sqlTraceTimeout;
    }

    public void setSqlTraceAlertTime(long sqlTraceAlertTime) {
        if (sqlTraceAlertTime > 0)
            this.sqlTraceAlertTime = sqlTraceAlertTime;
    }

    public void setSqlTraceAlert(SqlTraceAlert sqlTraceAlert) {
        if (sqlTraceAlert != null)
            this.sqlTraceAlert = sqlTraceAlert;
    }

    public List<Field> getConfigFields() {
        return configFields;
    }

    public final int getTraceQueueSize() {
        return tracedQueueSize.get();
    }

    public final ConcurrentLinkedQueue getTraceQueue() {
        return traceQueue;
    }

    Object executeStatement(SqlTraceEntry vo, Statement statement, Method method, Object[] args, String poolName) throws Throwable {
        vo.setMethodName(method.getName());
        int size = tracedQueueSize.incrementAndGet();
        traceQueue.offer(vo);
        vo.setTraceStartTime(System.currentTimeMillis());
        if (sqlTrace) log.info("Begin running sql:{}", vo.getSql());

        if (size > sqlTraceSize) {
            traceQueue.poll();
            tracedQueueSize.decrementAndGet();
        }

        try {
            Date startDate = new Date();
            vo.setExecStartTime(formatter.format(startDate));
            vo.setExecStartTimeMs(startDate.getTime());
            Object re = method.invoke(statement, args);
            vo.setExecSuccess(true);
            return re;
        } catch (Throwable e) {
            vo.setExecSuccess(false);
            if (e instanceof InvocationTargetException) {
                InvocationTargetException ee = (InvocationTargetException) e;
                if (ee.getCause() != null) {
                    e = ee.getCause();
                }
            }
            vo.setFailCause(e);
            throw e;
        } finally {
            Date endDate = new Date();
            vo.setExecEndTime(formatter.format(endDate));
            vo.setExecTookTimeMs(endDate.getTime() - vo.getExecStartTimeMs());
        }
    }

    private void removeTimeoutTrace() {
        alertList.clear();
        Iterator<SqlTraceEntry> itor = traceQueue.iterator();
        while (itor.hasNext()) {
            SqlTraceEntry vo = itor.next();
            if (vo.getExecTookTimeMs() >= sqlTraceAlertTime) {
                vo.setTimeAlert(true);
                alertList.add(vo);
            }

            if (System.currentTimeMillis() - vo.getTraceStartTime() > sqlTraceTimeout) {
                tracedQueueSize.decrementAndGet();
                traceQueue.remove(vo);
            }
        }

        if (!alertList.isEmpty()) {
            sqlTraceAlert.alert(alertList);
        }
    }

    private static final class TimeoutScanThreadThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, "SqlTrace-TimeoutScan");
            th.setDaemon(true);
            return th;
        }
    }
}
