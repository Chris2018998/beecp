package org.stone.test.beecp.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;

import java.sql.*;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0103ConnectionWrapperTest {

    @Test
    public void testConnectionWrapper() throws Exception {
        BeeDataSourceConfig config = createDefault();
        FastConnectionPool pool = new FastConnectionPool();
        pool.init(config);

        Connection con = pool.getConnection();
        DatabaseMetaData dbs = con.getMetaData();
        Statement st = con.createStatement();
        ResultSet rs1 = st.executeQuery("select * from user");
        ResultSetMetaData rs1Meta = rs1.getMetaData();

        Assertions.assertTrue(rs1Meta.isWrapperFor(ResultSetMetaData.class));
        Assertions.assertEquals(rs1Meta, rs1Meta.unwrap(ResultSetMetaData.class));

        Assertions.assertTrue(rs1.isWrapperFor(ResultSet.class));
        Assertions.assertEquals(rs1, rs1.unwrap(ResultSet.class));

        Assertions.assertTrue(st.isWrapperFor(Statement.class));
        Assertions.assertEquals(st, st.unwrap(Statement.class));

        Assertions.assertTrue(dbs.isWrapperFor(DatabaseMetaData.class));
        Assertions.assertEquals(dbs, dbs.unwrap(DatabaseMetaData.class));

        Assertions.assertTrue(con.isWrapperFor(Connection.class));
        Assertions.assertEquals(con, con.unwrap(Connection.class));

        con.close();
        pool.close();
    }
}
