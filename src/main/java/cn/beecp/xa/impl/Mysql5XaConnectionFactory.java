/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.xa.impl;

import cn.beecp.BeeDataSourceConfigException;
import cn.beecp.xa.RawXaConnectionFactory;

import javax.sql.XAConnection;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
    private Constructor xaConnectionConstructor;

    public Mysql5XaConnectionFactory() {
        try {
            Class dsClass = Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
            Class conClass = Class.forName("com.mysql.jdbc.Connection");
            Class xaConClass = Class.forName("com.mysql.jdbc.jdbc2.optional.JDBC4MysqlXAConnection");

            Object ds = dsClass.newInstance();
            Method logIndMethod = dsClass.getMethod("getLogXaCommands", new Class[0]);
            logXaCommands = (Boolean) logIndMethod.invoke(ds, new Object[0]);
            xaConnectionConstructor = xaConClass.getConstructor(new Class[]{conClass, Boolean.TYPE});
        } catch (Throwable e) {
            throw new BeeDataSourceConfigException(e);
        }
    }

    /**
     * Create XAConnection instance
     *
     * @param rawCon raw connection from pool
     * @return XAConnection
     * @throws SQLException if failed then throw SQLException
     */
    public XAConnection create(Connection rawCon) throws SQLException {
        try {
            return (XAConnection) xaConnectionConstructor.newInstance(new Object[]{rawCon, logXaCommands});
        } catch (Throwable e) {
            throw new SQLException(e);
        }
    }
}
