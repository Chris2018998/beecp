package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;
import org.stone.beecp.config.DsConfigFactory;
import org.stone.beecp.pool.exception.PoolCreateFailedException;

import static org.stone.beecp.JdbcConfig.*;

public class Tc0030DataSourceCreateTest extends TestCase {

    public void testWithoutParameter() {
        new BeeDataSource();
    }

    public void testOnConfig() {
        new BeeDataSource(DsConfigFactory.createDefault());
    }

    public void testOnJdbcInfo() {
        String driver = JDBC_DRIVER;
        String url = JDBC_URL;
        String user = JDBC_USER;
        String password = JdbcConfig.JDBC_PASSWORD;
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(driver, url, user, password);
        } finally {
            if (ds != null) ds.close();
        }
    }

    public void testDataSourceCreateFailed() {
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = DsConfigFactory.createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds = new BeeDataSource(config);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof PoolCreateFailedException);

            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assert.assertTrue(poolCause instanceof ClassNotFoundException);
        } finally {
            if (ds != null) ds.close();
        }
    }
}
