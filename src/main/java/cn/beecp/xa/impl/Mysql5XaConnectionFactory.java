/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.xa.impl;

import cn.beecp.xa.RawXaConnectionFactory;
import com.mysql.jdbc.jdbc2.optional.MysqlXAConnection;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mysql XaConnection Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Mysql5XaConnectionFactory implements RawXaConnectionFactory {
    private boolean logXaCommands;

    public Mysql5XaConnectionFactory() {
        MysqlXADataSource ds5 = new MysqlXADataSource();
        logXaCommands = ds5.getLogXaCommands();
    }

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        return new MysqlXAConnection((com.mysql.jdbc.Connection) rawCon, logXaCommands);
    }
}
