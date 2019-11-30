package cn.bee.dbcp.test.base;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cn.bee.dbcp.BeeDataSource;
import cn.bee.dbcp.BeeDataSourceConfig;
import cn.bee.dbcp.test.JdbcConfig;
import cn.bee.dbcp.test.TestCase;

public class ConnectionCloseEffectTest extends TestCase {
	private BeeDataSource ds;

	public void setUp() throws Throwable {
		BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setJdbcUrl(JdbcConfig.JDBC_URL);
		config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
		config.setUsername(JdbcConfig.JDBC_USER);
		config.setPassword(JdbcConfig.JDBC_PASSWORD);
		ds = new BeeDataSource(config);
	}

	public void tearDown() throws Throwable {
		ds.close();
	}

	public void testConnectionClose() throws InterruptedException, Exception {
		Connection con = ds.getConnection();
		Statement st=con.createStatement();
		CallableStatement cs=con.prepareCall("?={call test(}");
		PreparedStatement ps=con.prepareStatement("select 1 from dual");
		DatabaseMetaData dbs=con.getMetaData();
		
		con.close();
		
		try{
			st.getConnection();
			throw new java.lang.AssertionError("st operation after connection close(dbs)");	
		}catch(SQLException e){
		}
		
		try{
			ps.getConnection();
			throw new java.lang.AssertionError("st operation after connection close(ps)");	
		}catch(SQLException e){
		}
		
		try{
			cs.getConnection();
			throw new java.lang.AssertionError("st operation after connection close(cs)");	
		}catch(SQLException e){
		}
		
		try{
			dbs.getConnection();
			throw new java.lang.AssertionError("st operation after connection close(dbs)");	
		}catch(SQLException e){
		}
	}

	public void testStatementClose() throws InterruptedException, Exception {
		Connection con = ds.getConnection();
		Statement st=con.createStatement();
		CallableStatement cs=con.prepareCall("?={call test(}");
		PreparedStatement ps=con.prepareStatement("select 1 from dual");
		try{
			try{
				ResultSet rs=st.getResultSet();
				st.close();
				rs.getStatement();
				throw new java.lang.AssertionError("result operation after connection close(st)");	
			}catch(SQLException e){
			}
			
			try{
				ResultSet rs=ps.getResultSet();
				ps.close();
				rs.getStatement();
				throw new java.lang.AssertionError("result operation after connection close(ps)");	
			}catch(SQLException e){
			}
			
			try{
				ResultSet rs=cs.getResultSet();
				cs.close();
				rs.getStatement();
				throw new java.lang.AssertionError("result operation after connection close(cs)");	
			}catch(SQLException e){
			}
		}finally{
			con.close();
		}
	}
}
