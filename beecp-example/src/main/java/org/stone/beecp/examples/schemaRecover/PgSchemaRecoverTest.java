/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.examples.schemaRecover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.pool.ProxyConnectionBase;
import org.stone.beecp.util.TestUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Schema recover test on postgres
 *
 * @author chris liao
 */
public class PgSchemaRecoverTest {
    private static Logger logger = LoggerFactory.getLogger(PgSchemaRecoverTest.class);

    public static void main(String[] args) throws Exception {
        BeeDataSource ds = new BeeDataSource();
        ds.setInitialSize(1);//only one
        ds.setMaxActive(1);//only one
        ds.setDefaultSchema("public");
        ds.setDriverClassName("org.postgresql.Driver");//postgresql-42.7.2.jar
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        ds.setUsername("postgres");
        ds.setPassword("root");
        ds.setDefaultAutoCommit(false);
        ds.setForceDirtyOnSchemaAfterSet(true);//enable indicator to true

        Field rawField = ProxyConnectionBase.class.getDeclaredField("raw");
        rawField.setAccessible(true);

        Connection raw1;
        Connection connection1 = null;
        try {
            connection1 = ds.getConnection();
            raw1 = (Connection) rawField.get(connection1);

            //a sql to create test schema: CREATE SCHEMA another_schema;
            connection1.setSchema("another_schema");//change schema to another
            connection1.commit();
        } finally {
            TestUtil.oclose(connection1);//schema recovered under this step
        }


        Connection raw2;
        String connection2_schema;
        Connection connection2 = null;
        try {
            connection2 = ds.getConnection();
            raw2 = (Connection) rawField.get(connection2);
            if (raw1 != raw2) throw new SQLException("Schema recover test must be on a same connection");
            connection2_schema = connection2.getSchema();
        } finally {
            if (connection2 != null) connection2.close();
        }

        boolean recovered = "public".equals(connection2_schema);
        if (recovered) {
            logger.info("Passed! Connection2.Schema[" + connection2_schema + "]was recovered to default:[public]");
        } else {
            logger.error("Failed! Connection2.Schema[" + connection2_schema + "]was not recovered to default:[public]");
        }
    }
}
