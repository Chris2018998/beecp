/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package cn.beecp.pool;

import cn.beecp.RawXaConnectionFactory;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.SQLException;

import static cn.beecp.pool.PoolStaticCenter.isBlank;

/**
 * XaConnection Factory implementation by XADataSource
 *
 * @author Chris liao
 * @version 1.0
 */
public class XaConnectionFactoryByDriverDs implements RawXaConnectionFactory {
    //username
    private final String username;
    //password
    private final String password;
    //usernameIsNotNull
    private final boolean useUsername;
    //driverDataSource
    private final XADataSource dataSource;

    //Constructor
    public XaConnectionFactoryByDriverDs(XADataSource dataSource, String username, String password) {
        this.dataSource = dataSource;
        this.username = username;
        this.password = password;
        useUsername = !isBlank(username);
    }

    //create one connection
    public final XAConnection create() throws SQLException {
        return this.useUsername ? this.dataSource.getXAConnection(this.username, this.password) : this.dataSource.getXAConnection();
    }
}