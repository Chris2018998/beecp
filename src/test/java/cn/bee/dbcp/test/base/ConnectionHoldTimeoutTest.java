package cn.bee.dbcp.test.base;

import java.sql.Connection;
import java.sql.SQLException;

import cn.bee.dbcp.BeeDataSource;
import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.pool.FastConnectionPool;
import cn.bee.dbcp.test.JdbcConfig;
import cn.bee.dbcp.test.TestCase;
import cn.bee.dbcp.test.TestUtil;

public class ConnectionHoldTimeoutTest extends TestCase {
	private BeeDataSource ds;

	public void setUp() throws Throwable {
		BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setJdbcUrl(JdbcConfig.JDBC_URL);
		config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
		config.setUsername(JdbcConfig.JDBC_USER);
		config.setPassword(JdbcConfig.JDBC_PASSWORD);
		config.setInitialSize(0);
		config.setConnectionTestSQL("SELECT 1 from dual");

		config.setHoldIdleTimeout(1000);// hold and not using connection;
		config.setIdleCheckTimeInterval(1000L);// two seconds interval
		config.setIdleCheckTimeInitDelay(0);
		config.setWaitTimeToClearPool(0);
		ds = new BeeDataSource(config);
	}

	public void tearDown() throws Throwable {
		ds.close();
	}

	public void test() throws InterruptedException,Exception {
		FastConnectionPool pool = (FastConnectionPool) TestUtil.getPool(ds);
		Connection connection = ds.getConnection();

		if(pool.getConnTotalSize()!=1)throw new java.lang.AssertionError("Total connections not as expected 1");
		if(pool.getConnUsingSize()!=1)throw new java.lang.AssertionError("Using connections not as expected 1");
		
		Thread.sleep(4000);
		if(pool.getConnUsingSize()!=0)throw new java.lang.AssertionError("Using connections not as expected 0 after hold timeout");

		try{
			connection.getCatalog();
			throw new AssertionError("must throw closed exception");
		}catch(SQLException e){}
		
		Thread.sleep(4000);
	}
}
