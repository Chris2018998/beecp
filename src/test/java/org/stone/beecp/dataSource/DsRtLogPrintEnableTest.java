package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.stone.base.TestUtil;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.JdbcConfig;

public class DsRtLogPrintEnableTest extends TestCase {

    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);// give valid URL
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testLogEnableInd() throws Exception {
        Object pool = TestUtil.getFieldValue(ds, "pool");
        Boolean ind = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
        if (Boolean.TRUE.equals(ind)) throw new Exception();

        ds.setPrintRuntimeLog(true);
        ind = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
        if (!Boolean.TRUE.equals(ind)) throw new Exception();
    }
}
