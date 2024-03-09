package org.stone.beecp.issue.HikariCP.issue2170;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * dirty properties dirty test on a only pooled connection under transaction with real db
 */

class DirtySchemaTest {

    //Recover Test on Schema
    void testSchema(DataSource ds, String dsName, String db, String defaultSchema, String newSchema) throws SQLException {

        //step1: change schema and rollback
        Connection con1 = null;
        try {
            con1 = ds.getConnection();
            if (!defaultSchema.equals(con1.getSchema()))//check schema whether equals to default
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]Schema not equals to default[" + defaultSchema + "]");

            con1.setSchema(newSchema);//try to change schema from default to new
            if (!newSchema.equals(con1.getSchema()))
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]Schema not modified to new [" + newSchema + "]");

            con1.commit();//commit
            con1.setSchema(defaultSchema);//set schema to default
            con1.rollback();//schema maybe rollback to pre-changed value(*** test point ******)
        } finally {
            if (con1 != null)
                con1.close();//dirty properties will be reset to default,if not reset,the next step2 will be failed
        }

        /*
         * step2:re-take out the only connection from pool and test its schema whether reset to default values,
         * if failed then throws exception
         */
        Connection con2 = null;
        try {
            con2 = ds.getConnection();
            if (!defaultSchema.equals(con2.getSchema()))
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]schema not reset to default:" + defaultSchema);
        } finally {
            if (con2 != null) con2.close();
        }
    }

    //Recover Test on Catalog
    void testCatalog(DataSource ds, String dsName, String db, String defaultCatalog, String newCatalog) throws SQLException {
        //step1: change Catalog and rollback
        Connection con1 = null;
        try {
            con1 = ds.getConnection();
            if (!defaultCatalog.equals(con1.getCatalog()))//check catalog whether equals to default
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]Catalog not equals to default[" + defaultCatalog + "]");

            con1.setCatalog(newCatalog);//try to change catalog from default to new
            if (!newCatalog.equals(con1.getCatalog()))
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]Catalog not modified to new [" + newCatalog + "]");

            con1.commit();//commit
            con1.setCatalog(defaultCatalog);//set catalog to default
            con1.rollback();//catalog maybe rollback to pre-changed value(*** test point ******)
        } finally {
            if (con1 != null)
                con1.close();//dirty properties will be reset to default,if not reset,the next step2 will be failed
        }

        /*
         * step2:re-take out the only connection from pool and test its catalog whether reset to default values,
         * if failed then throws exception
         */
        Connection con2 = null;
        try {
            con2 = ds.getConnection();
            if (!defaultCatalog.equals(con2.getCatalog()))
                throw new AssertionError("Ds[" + dsName + "]-DB[" + db + "]Catalog not reset to default:" + defaultCatalog);
        } finally {
            if (con2 != null) con2.close();
        }
    }
}
