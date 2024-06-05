package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

public class Tc0066PoolInitializeFailedTest extends TestCase {
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
            config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
            config.setUsername(JdbcConfig.JDBC_USER);
            config.setPassword(JdbcConfig.JDBC_PASSWORD);
            config.setInitialSize(initSize);
            new BeeDataSource(config);
            fail("A initializerError need be thrown,but not");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RuntimeException);
        }
    }
}
