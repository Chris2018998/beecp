package org.stone.beecp.issue.HikariCP.issue1894;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.SQLException;

/**
 * 1)Test Env:
 * Java: Java-1.8.0_65-b17
 * JDBC-Pool: stone-1.2.8
 * DB: xxxxx(My computer is windows7_64 not support mongodb installer,you can test it by yourself,I think the test result should be passed)
 * Driver: mongodb-driver-sync.4.11.1.jar
 * <p>
 * 2)Test Machine
 * Os:Win7_64,CPU:2.6Hz*4(I5-4210M),Memory(12G)
 * <p>
 * 3)Test info
 * Chris Liao(Stone project owner)
 * Test Date: 2024/02/25 in China
 * Test Result:xxx
 */
public class AutoCommitNotSupportedTest {

    public static void main(String[] args) {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setUsername("admin");
        config.setPassword("admin");
        config.setDriverClassName("com.mongodb.jdbc.MongoDriver");
        config.setJdbcUrl("mongodb://localhost/test");

        try {
            new BeeDataSource(config);
        } catch (RuntimeException e) {//an exception will be thrown here,if not handle SQLFeatureNotSupportedException in pool initialization
            SQLException ee = (SQLException) e.getCause();
            System.out.println("SQLFeatureNotSupportedException of autoCommit not be handled on pool initialization");
            throw e;
        }

        System.out.println("Success on test SQLFeatureNotSupportedException for MongoDB");
    }
}
