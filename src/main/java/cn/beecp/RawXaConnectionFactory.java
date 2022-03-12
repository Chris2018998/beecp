/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

import javax.sql.XAConnection;
import java.sql.SQLException;

/**
 * XAConnection Factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
public interface RawXaConnectionFactory {
    //create XAConnection instance
    XAConnection create() throws SQLException;
}
