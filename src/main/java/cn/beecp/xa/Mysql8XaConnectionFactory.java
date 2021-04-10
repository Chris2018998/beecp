/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.xa;


import com.mysql.cj.conf.PropertyKey;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.jdbc.MysqlXAConnection;
import com.mysql.cj.jdbc.MysqlXADataSource;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mysql XaConnection Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Mysql8XaConnectionFactory implements XaConnectionFactory {
    private boolean logXaCommands;

    public Mysql8XaConnectionFactory() {
        MysqlXADataSource ds8 = new MysqlXADataSource();
        logXaCommands = ds8.getBooleanProperty(PropertyKey.logXaCommands).getValue();
    }

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        return new MysqlXAConnection((JdbcConnection) rawCon, logXaCommands);
    }
}
