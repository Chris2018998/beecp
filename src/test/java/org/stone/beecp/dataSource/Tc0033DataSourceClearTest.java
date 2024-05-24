package org.stone.beecp.dataSource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.config.DsConfigFactory;

public class Tc0033DataSourceClearTest extends TestCase {

    public void testOnClear1() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(DsConfigFactory.createDefault());
            ds.clear(true);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals(0, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }

    public void testOnClear2() throws Exception {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource(DsConfigFactory.createDefault());

            try {
                ds.clear(true, null);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof BeeDataSourceConfigException);
            }

            BeeDataSourceConfig config2 = DsConfigFactory.createDefault();
            config2.setInitialSize(2);
            ds.clear(true, config2);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assert.assertEquals(2, vo.getIdleSize());
        } finally {
            if (ds != null) ds.close();
        }
    }
}
