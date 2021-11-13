/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.xa.impl;

import cn.beecp.xa.RawXaConnectionFactory;
import oracle.jdbc.xa.client.OracleXAConnection;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Oracle XaConnection Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OracleXaConnectionFactory implements RawXaConnectionFactory {

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        try {
            return new OracleXAConnection(rawCon);
        } catch (XAException e) {
            throw new SQLException(e);
        }
    }
}
