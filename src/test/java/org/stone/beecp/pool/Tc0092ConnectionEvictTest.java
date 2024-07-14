package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.objects.MockErrorCodeConnectionFactory;
import org.stone.beecp.objects.MockErrorStateConnectionFactory;
import org.stone.beecp.objects.MockEvictConnectionPredicate;
import org.stone.beecp.objects.MockEvictPredicateConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0092ConnectionEvictTest extends TestCase {

    public void testOnErrorCode() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setConnectionFactory(new MockErrorCodeConnectionFactory(0b010000));
        config.addSqlExceptionCode(0b010000);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.getSchema();
        } catch (SQLException e) {
            Assert.assertEquals(0b010000, e.getErrorCode());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    public void testOnErrorState() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setConnectionFactory(new MockErrorStateConnectionFactory("57P02"));
        config.addSqlExceptionState("57P02");
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.getSchema();
        } catch (SQLException e) {
            Assert.assertEquals("57P02", e.getSQLState());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();
    }

    public void testOnPredicate() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setConnectionFactory(new MockEvictPredicateConnectionFactory(0b010000, "57P02"));
        config.setEvictPredicate(new MockEvictConnectionPredicate(0b010000, "57P02"));
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);
        Connection con = pool.getConnection();

        try {
            con.getSchema();
        } catch (SQLException e) {
            Assert.assertEquals(0b010000, e.getErrorCode());
            Assert.assertEquals("57P02", e.getSQLState());
        }
        Assert.assertEquals(1, pool.getTotalSize());
        pool.close();

    }

    public void testOnAbort() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(4);
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        BeeConnectionPoolMonitorVo vo = pool.getPoolMonitorVo();
        Assert.assertEquals(4, vo.getIdleSize());
        Connection con = pool.getConnection();
        con.abort(null);

        vo = pool.getPoolMonitorVo();
        Assert.assertEquals(3, vo.getIdleSize());
        pool.close();
    }
}
