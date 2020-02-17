package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.TransactionIsolationLevel;
import cn.beecp.pool.FastConnectionPool;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;
import cn.beecp.util.BeecpUtil;

import java.sql.Connection;

public class ConnectionResetTest  extends TestCase {
    private BeeDataSource ds;
    String catlog="mysql";
    String schema="mysql";
    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        config.setDefaultAutoCommit(false);
        config.setDefaultTransactionIsolation(TransactionIsolationLevel.LEVEL_READ_COMMITTED);
        config.setDefaultReadOnly(true);
        config.setDefaultCatalog(catlog);
        config.setDefaultSchema(schema);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws InterruptedException, Exception {
        Connection con = null;
        try {
            con = ds.getConnection();
            con.setAutoCommit(true);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setReadOnly(false);
            con.setCatalog("test");
        } finally {
        	if(con!=null)
            BeecpUtil.oclose(con);
        }
        try {
            FastConnectionPool pool = (FastConnectionPool) TestUtil.getPool(ds);
            if(pool.getConnTotalSize()!=1)TestUtil.assertError("Total connections not as expected:"+1);

            con = ds.getConnection();
            //if(con.getAutoCommit()!=false)TestUtil.assertError("autoCommit reset fail");
            if(con.getTransactionIsolation()!=Connection.TRANSACTION_READ_COMMITTED)TestUtil.assertError("TransactionIsolation reset fail");
            if(con.isReadOnly()!=true)TestUtil.assertError("readony reset fail");
            if(!catlog.equals(con.getCatalog()))TestUtil.assertError("catalog reset fail,excpect:s%,cuurent is s%",catlog,  con.getCatalog());
        } finally {
        	if(con!=null)
             BeecpUtil.oclose(con);
        }
    }
}
