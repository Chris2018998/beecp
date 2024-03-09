package org.stone.beecp.issue.HikariCP.issue2170;

import org.stone.beecp.BeeDataSource;

/**
 * Jdk:Java8
 * db: mysql-5.6.51-winx64
 * driver:mysql-connector-java-5.1.49.jar
 * pool:beecp-3.5.1/stone-1.2.7
 * <p>
 * Test result:passed
 */
public class MysqlRecoverTest extends DirtySchemaTest {

    public static void main(String[] args) throws Exception {
        BeeDataSource dataSource = new BeeDataSource();
        dataSource.setDefaultAutoCommit(false);
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(1);
        dataSource.setDefaultCatalog("test");
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setJdbcUrl("JDBC:MYSQL://localhost/test");
        dataSource.setUsername("root");

        MysqlRecoverTest test = new MysqlRecoverTest();
        test.testCatalog(dataSource, "Bee", "MySQL", "test", "mysql");
    }
}