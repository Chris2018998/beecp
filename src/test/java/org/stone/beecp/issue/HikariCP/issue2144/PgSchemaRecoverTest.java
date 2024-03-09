package org.stone.beecp.issue.HikariCP.issue2144;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.pool.ProxyConnectionBase;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 1)Test Env:
 * Java: Java-1.8.0_65-b17
 * DB: PostgreSQL-10.4-1-win64-bigsql.exe
 * Driver: postgresql-42.7.1.jar
 * JDBC-Pool: stone-1.2.8
 * <p>
 * 2)Test Machines
 * Server: Os:Win7_64,CPU:2.8Hz*2(PG80),Memory(8G)
 * Client: Os:Win7_64,CPU:2.6Hz*4(I5-4210M),Memory(12G)
 * <p>
 * 3)Test info
 * Chris Liao(Stone project owner)
 * Test Date: 2024/02/27 in China
 * <p>
 * Test Result:Passed
 */
public class PgSchemaRecoverTest {
    public static void main(String[] args) throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setInitialSize(1);//only one
        ds.setMaxActive(1);//only one
        ds.setDefaultSchema("public");
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        ds.setUsername("postgres");
        ds.setPassword("root");
        ds.setDefaultAutoCommit(false);
        ds.setDefaultSchema("public");
        ds.setForceDirtyOnSchemaAfterSet(true);//add to support postgres

        Field rawField = ProxyConnectionBase.class.getDeclaredField("raw");
        rawField.setAccessible(true);

        Connection connection = null;
        Connection raw1;
        try {
            connection = ds.getConnection();
            raw1 = (Connection) rawField.get(connection);

            //logger.info("Started SetSchema");
            // Set to a different schema to dirty bits on Hikari
            //execution sql before test:  CREATE SCHEMA another_schema;
            connection.setSchema("another_schema");
            connection.commit();
        } finally {
            if (connection != null) connection.close();
        }

        Connection connection2 = null;
        Connection raw2;
        try {
            connection2 = ds.getConnection();
            raw2 = (Connection) rawField.get(connection2);

            String schema = connection2.getSchema();
            if (raw1 != raw2) throw new SQLException("RawConneciton ");
            if (!"public".equals(schema)) throw new SQLException("schema not reset default");
        } finally {
            if (connection2 != null) connection2.close();
        }

        System.out.println("Test case passed,schema has reset to default");
    }
}
