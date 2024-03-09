package org.stone.beecp.issue.HikariCP.issue1819;

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class DBShutDownHelp {

    public static void restartDB(BeeDataSource ds) throws Exception {
        //step1:get alive count in pool
        BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
        if (vo.getIdleSize() != 5) throw new SQLException("Pool initial connections count was not expected number(5)");

        //step2: block the current thread 5 seconds and your need shutdown your database server(**** import step *****)
        System.out.println("....................Shutdown your database within 5 seconds...............................");
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));


        //step3: try to get a connection from pool
        Connection con = null;
        try {
            con = ds.getConnection();
        } catch (SQLException e) {//creation failure exception,which is cause by network communication failed
            vo = ds.getPoolMonitorVo();
            if (vo.getIdleSize() != 0) throw new SQLException("Test failed,Idle connections still exists in pool");
            if (vo.getUsingSize() != 0)
                throw new SQLException("Test failed,Using connections still exists in pool");
        } finally {
            if (con != null) con.close();
        }

        //step4: restart your database server(**** import step *****)
        System.out.println("....................Startup your database within 5 seconds...............................");
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));


        //step5: try to get a connection from pool,success means that pool has restore to re-create new connections
        Connection con2 = null;
        try {
            con2 = ds.getConnection();
        } finally {
            if (con2 != null) con2.close();
        }

        //step6: print success words at end
        System.out.println("Test case passed for database server restart");
    }
}
