package org.stone.beecp.issue.HikariCP.issue1819;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

/**
 * 1)Test Env:
 * Java: Java-1.8.0_65-b17
 * DB: mysql-5.6.51-winx64
 * Driver: mysql-connector-java-5.1.49.jar
 * JDBC-Pool: stone-1.2.8
 * <p>
 * 2)Test Machines
 * Server: Os:Win7_64,CPU:2.8Hz*2(PG80),Memory(8G)
 * Client: Os:Win7_64,CPU:2.6Hz*4(I5-4210M),Memory(12G)
 * <p>
 * 3)Test info
 * Chris Liao(Stone project owner)
 * Test Date: 2024/02/25 in China
 * Test Result:Passed
 */

public class MysqlRestartTest extends DBShutDownHelp {

    public static void main(String[] args) throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setDefaultAutoCommit(false);
        config.setInitialSize(5);
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://192.168.0.103/test");//you can replace it with your link and your driver
        config.setUsername("root");

        BeeDataSource ds = new BeeDataSource(config);
        DBShutDownHelp.restartDB(ds);
    }
}
