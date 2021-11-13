/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.xa.impl;

import cn.beecp.xa.RawXaConnectionFactory;
import org.postgresql.xa.PGXAConnection;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Postgres XaConnectionFactory
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PostgresXaConnectionFactory implements RawXaConnectionFactory {

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        return new PGXAConnection((org.postgresql.core.BaseConnection) rawCon);
    }
}
