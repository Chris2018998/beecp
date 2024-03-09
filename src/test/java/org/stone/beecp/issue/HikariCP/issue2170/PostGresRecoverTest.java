package org.stone.beecp.issue.HikariCP.issue2170;

import org.stone.beecp.BeeDataSource;

/**
 * 1 Issue
 * Test case for issue #2170 of HikariCP
 * Issue url: https://github.com/brettwooldridge/HikariCP/issues/2170
 * <p>
 * 2 Test info
 * db-installer:PostgreSQL-10.4-1-win64-bigsql.exe
 * jdbc driver: postgresql-42.7.1.jar
 * SQL Execution before test: CREATE SCHEMA schema1;
 * <p>
 * 3 Test Result: passed
 */
public class PostGresRecoverTest extends DirtySchemaTest {

    public static void main(String[] args) throws Exception {
        BeeDataSource dataSource = new BeeDataSource();
        dataSource.setDefaultAutoCommit(false);
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(1);
        dataSource.setDefaultSchema("public");
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUsername("postgres");
        dataSource.setPassword("root");

        //config item added for postgres db,if ignore,test result was failed
        dataSource.setForceDirtyOnSchemaAfterSet(true);
        dataSource.setForceDirtyOnCatalogAfterSet(true);

        PostGresRecoverTest test = new PostGresRecoverTest();
        test.testSchema(dataSource, "Bee", "postgresql", "public", "schema1");
    }
}
