/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC Connection factory
 *
 * @author Chris
 * @version 1.0
 */
public interface ConnectionFactory {

    //create raw connection
    Connection create() throws SQLException;

}
