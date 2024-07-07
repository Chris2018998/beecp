package org.stone.beecp.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.*;

import static org.stone.beecp.config.DsConfigFactory.createDefault;

public class Tc0103ConnectionWrapperTest extends TestCase {

    public void testConnectionWrapper() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con = pool.getConnection();
        DatabaseMetaData dbs = con.getMetaData();
        Statement st = con.createStatement();
        ResultSet rs1 = st.executeQuery("select * from user");
        ResultSetMetaData rs1Meta = rs1.getMetaData();

        Assert.assertTrue(rs1Meta.isWrapperFor(ResultSetMetaData.class));
        Assert.assertEquals(rs1Meta, rs1Meta.unwrap(ResultSetMetaData.class));

        Assert.assertTrue(rs1.isWrapperFor(ResultSet.class));
        Assert.assertEquals(rs1, rs1.unwrap(ResultSet.class));

        Assert.assertTrue(st.isWrapperFor(Statement.class));
        Assert.assertEquals(st, st.unwrap(Statement.class));

        Assert.assertTrue(dbs.isWrapperFor(DatabaseMetaData.class));
        Assert.assertEquals(dbs, dbs.unwrap(DatabaseMetaData.class));

        Assert.assertTrue(con.isWrapperFor(Connection.class));
        Assert.assertEquals(con, con.unwrap(Connection.class));

        con.close();
        pool.close();
    }
}
