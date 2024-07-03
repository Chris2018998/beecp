package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

public class Tc0493ProxyResultSetFromDsMetaCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(0);
        config.setAliveTestSql("SELECT 1 from dual");
        ds = new BeeDataSource(config);
    }

    public void tearDown() {
        ds.close();
    }

    public void testMetaData() throws Exception {
        Connection con;
        ResultSet rs;

        con = ds.getConnection();
        DatabaseMetaData dsMeta = con.getMetaData();
        rs = dsMeta.getTableTypes();
        oclose(rs);

        Assert.assertEquals(con, dsMeta.getConnection());
        con.close();
        try {
            dsMeta.getTableTypes();
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("No operations allowed after connection closed"));
        }
    }
}
