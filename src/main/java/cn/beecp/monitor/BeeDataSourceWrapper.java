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
package cn.beecp.boot.monitor;

import cn.beecp.BeeDataSource;
import cn.beecp.boot.SystemUtil;
import cn.beecp.boot.monitor.sqltrace.ProxyFactory;
import cn.beecp.pool.ConnectionPoolMonitorVo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/*
 *  Bee Data Source Wrapper
 *
 *  @author Chris.Liao
 */
public class BeeDataSourceWrapper implements DataSource {
    private String poolName;
    private boolean traceSQL;
    private BeeDataSource delegete;

    public BeeDataSourceWrapper(BeeDataSource delegete, boolean traceSQL) {
        this.delegete = delegete;
        this.traceSQL = traceSQL;
    }

    public ConnectionPoolMonitorVo getMonitorVo() throws Exception {
        return delegete.getMonitorVo();
    }

    public Connection getConnection() throws SQLException {
        Connection con = delegete.getConnection();
        if (traceSQL) {
            if (SystemUtil.isBlank(poolName)) {
                ConnectionPoolMonitorVo vo = delegete.getMonitorVo();
                poolName = vo.getPoolName();
            }
            return ProxyFactory.createConnection(con, poolName);
        } else {
            return con;
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return delegete.getConnection(username, password);
    }

    public java.io.PrintWriter getLogWriter() throws SQLException {
        return delegete.getLogWriter();
    }

    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        delegete.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return delegete.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        delegete.setLoginTimeout(seconds);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegete.getParentLogger();
    }

    public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
        return delegete.unwrap(iface);
    }

    public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
        return delegete.isWrapperFor(iface);
    }
}
