package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.*;

public class Tc0487PoolInitializeFailedTest extends TestCase {
    public void setUp() {
        //do nothing
    }

    public void tearDown() {
        //do nothing
    }

    public void testPoolInit() {
        try {
            final int initSize = 5;
            BeeDataSourceConfig config = new BeeDataSourceConfig();
            config.setJdbcUrl("jdbc:beecp://localhost/testdb2");//give valid URL
            config.setDriverClassName(JDBC_DRIVER);
            config.setUsername(JDBC_USER);
            config.setPassword(JDBC_PASSWORD);
            config.setInitialSize(initSize);
            new BeeDataSource(config);
            fail("A initializerError need be thrown,but not");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof SQLException);
            Assert.assertTrue(e.getCause().getMessage().contains("db not found"));
        }
    }
}
