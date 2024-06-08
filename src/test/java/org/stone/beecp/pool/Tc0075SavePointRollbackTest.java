package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.config.DsConfigFactory;

import java.sql.Connection;
import java.sql.Savepoint;

//test case for issue #2142 of HikariCP
public class Tc0075SavePointRollbackTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = DsConfigFactory.createDefault();
        config.setDefaultAutoCommit(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        ds = new BeeDataSource(config);
    }

    public void test() throws Exception {

        try (Connection conn1 = ds.getConnection()) {//
            conn1.setAutoCommit(false);//
            conn1.createStatement().execute(
                    "INSERT INTO data (type, payload) VALUES ('a', '{}'::jsonb)");

            Savepoint point1 = conn1.setSavepoint();
            conn1.rollback(point1);

            /*
             * Key info
             * 1: rollback to the point1,it is no effect on the previous executing statement.
             * 2: terminate a traction, need call <method>commit</method> or <method>rollback</method>
             */
        }
        //I think that bee connection will reset to default and rollback

        try (Connection conn2 = ds.getConnection()) {
            Assert.assertTrue(conn2.getAutoCommit());
        }
    }

    public void tearDown() {
        ds.close();
    }
}