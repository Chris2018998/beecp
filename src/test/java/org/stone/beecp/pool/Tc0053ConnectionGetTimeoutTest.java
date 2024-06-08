/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0053ConnectionGetTimeoutTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setMaxWait(1000);
        config.setMaxActive(1);
        config.setBorrowSemaphoreSize(1);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void test() throws Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            GetMockThread mockThread = new GetMockThread();
            mockThread.start();

            mockThread.join();
            Assert.assertNotNull(mockThread.e);
        } finally {
            if (con != null)
                oclose(con);
        }
    }

    class GetMockThread extends Thread {
        SQLException e = null;

        public void run() {
            Connection con2 = null;
            try {
                con2 = ds.getConnection();
            } catch (SQLException e) {
                this.e = e;
            } finally {
                if (con2 != null)
                    oclose(con2);
            }
        }
    }
}
