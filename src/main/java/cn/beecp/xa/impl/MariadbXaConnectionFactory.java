/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp.xa.impl;

import cn.beecp.xa.RawXaConnectionFactory;
import org.mariadb.jdbc.MariaDbConnection;
import org.mariadb.jdbc.MariaXaConnection;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mariadb XaConnection Factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class MariadbXaConnectionFactory implements RawXaConnectionFactory {

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        return new MariaXaConnection((MariaDbConnection) rawCon);
    }
}