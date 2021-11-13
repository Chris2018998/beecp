/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC raw connection factory
 *
 * @author Chris
 * @version 1.0
 */
public interface RawConnectionFactory {

    //create raw connection
    Connection create() throws SQLException;

}
