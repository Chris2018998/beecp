package cn.bee.dbcp.test.base;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import cn.bee.dbcp.BeeDataSource;
import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.test.JdbcConfig;
import cn.bee.dbcp.test.TestCase;

public class ConnectionWrapperTest extends TestCase {
	
	private BeeDataSource ds;

	public void setUp() throws Throwable {
		BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setJdbcUrl(JdbcConfig.JDBC_URL);
		config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
		config.setUsername(JdbcConfig.JDBC_USER);
		config.setPassword(JdbcConfig.JDBC_PASSWORD);
		config.setInitialSize(5);
		config.setConnectionTestSQL("SELECT 1 from dual");
		config.setIdleTimeout(3000);
		config.setIdleCheckTimeInitDelay(10);
		ds = new BeeDataSource(config);
	}

	public void tearDown() throws Throwable {
		ds.close();
	}
	
	public void test() throws InterruptedException, Exception {
		Connection con = ds.getConnection();
		Statement st=con.createStatement();
		CallableStatement cs=con.prepareCall("?={call test(}");
		PreparedStatement ps=con.prepareStatement("select 1 from dual");
		DatabaseMetaData dbs=con.getMetaData();
		
		if (st.getConnection() != con)
			throw new java.lang.AssertionError("Raw conneciton exposed(st)");
		if (cs.getConnection() != con)
			throw new java.lang.AssertionError("Raw conneciton exposed(cs)");
		if (ps.getConnection() != con)
			throw new java.lang.AssertionError("Raw conneciton exposed(ps)");
		if (dbs.getConnection() != con)
			throw new java.lang.AssertionError("Raw conneciton exposed(dbs)");	
		
		ResultSet re1=st.executeQuery("select 1 from dual");
		if (re1.getStatement() != st)
			throw new java.lang.AssertionError("Raw Statement exposed(st)");	
		ResultSet re2=ps.executeQuery();
		if (re2.getStatement() != ps)
			throw new java.lang.AssertionError("Raw Statement exposed(ps)");	
		ResultSet re3=cs.getResultSet();
		if (re3.getStatement() != cs)
			throw new java.lang.AssertionError("Raw Statement exposed(cs)");
		ResultSet re4=dbs.getTableTypes();
		if (re4.getStatement() != null)
			throw new java.lang.AssertionError("Raw Statement exposed(dbs)");
		
		if (re1.getStatement()!=st)
			throw new java.lang.AssertionError("Raw Result exposed(st)");	
		if (re2.getStatement()!=ps)
			throw new java.lang.AssertionError("Raw Result exposed(ps)");	
		if (re3.getStatement()!=cs)
			throw new java.lang.AssertionError("Raw Result exposed(cs)");
		con.close();
	}
}
