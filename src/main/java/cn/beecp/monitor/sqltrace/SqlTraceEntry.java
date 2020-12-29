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

/*
 *  SQL Execute Trace entry
 *
 *  @author Chris.Liao
 */
public class SqlTraceEntry {
    private String sql;
    private String poolName;
    private String statementType;
    private String execStartTime;
    private long execStartTimeMs;

    private String execEndTime;
    private long execTookTimeMs;
    private boolean execSuccess;
    private long traceStartTime;

    private boolean timeAlert;
    private String methodName;
    private Throwable failCause;

    public SqlTraceEntry(String sql, String poolName, String statementType) {
        this.sql = sql;
        this.poolName = poolName;
        this.statementType = statementType;
        this.execStartTimeMs = System.currentTimeMillis();
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getExecStartTime() {
        return execStartTime;
    }

    public void setExecStartTime(String execStartTime) {
        this.execStartTime = execStartTime;
    }

    public long getExecStartTimeMs() {
        return execStartTimeMs;
    }

    public void setExecStartTimeMs(long execStartTimeMs) {
        this.execStartTimeMs = execStartTimeMs;
    }

    public String getExecEndTime() {
        return execEndTime;
    }

    public void setExecEndTime(String execEndTime) {
        this.execEndTime = execEndTime;
    }

    public long getExecTookTimeMs() {
        return execTookTimeMs;
    }

    public void setExecTookTimeMs(long execTookTimeMs) {
        this.execTookTimeMs = execTookTimeMs;
    }

    public boolean isExecSuccess() {
        return execSuccess;
    }

    public void setExecSuccess(boolean execSuccess) {
        this.execSuccess = execSuccess;
    }

    public long getTraceStartTime() {
        return traceStartTime;
    }

    public void setTraceStartTime(long traceStartTime) {
        this.traceStartTime = traceStartTime;
    }

    public boolean isTimeAlert() {
        return timeAlert;
    }

    public void setTimeAlert(boolean timeAlert) {
        this.timeAlert = timeAlert;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Throwable getFailCause() {
        return failCause;
    }

    public void setFailCause(Throwable failCause) {
        this.failCause = failCause;
    }
}
