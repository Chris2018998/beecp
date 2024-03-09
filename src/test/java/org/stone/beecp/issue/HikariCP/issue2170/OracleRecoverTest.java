package org.stone.beecp.issue.HikariCP.issue2170;

import org.stone.beecp.BeeDataSource;

/**
 * Jdk:Java8
 * db:oracle12g
 * driver:ojdbc7-12.1.0.2.jar
 * pool:beecp-3.5.1/stone-1.2.7
 * <p>
 * test Result: Passed
 */

public class OracleRecoverTest extends DirtySchemaTest {

    public static void main(String[] args) throws Exception {
        BeeDataSource dataSource = new BeeDataSource();
        dataSource.setDefaultAutoCommit(false);
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(1);
        dataSource.setDefaultSchema("SYSTEM");
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setJdbcUrl("jdbc:oracle:thin:@localhost:1521:orcl");
        dataSource.setUsername("system");
        dataSource.setPassword("root");

        OracleRecoverTest test = new OracleRecoverTest();
        test.testSchema(dataSource, "Bee", "ORACLE", "SYSTEM", "SYS");
    }

}
