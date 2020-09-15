package cn.beecp.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;

public class TestResultSet {

	private static String driver = "com.mysql.jdbc.Driver";
	private static String url = "jdbc:mysql://localhost/test";
	private static String user = "root";
	private static String password = "";
	private static String SQL = "select 1 from dual";

	public static void main(String[] args) throws Exception {
		testBeeCP();
		testHikariCP();
		testTomcatJdbc();
		testDruid();
	}

	private static void testBeeCP() throws Exception {
		try {
			BeeDataSourceConfig config = new BeeDataSourceConfig();
			config.setDriverClassName(driver);
			config.setJdbcUrl(url);
			config.setUsername(user);
			config.setPassword(password);
			config.setConnectionTestSQL("select 1 from dual");
			config.setDefaultAutoCommit(false);
			config.setTraceStatement(true);
			BeeDataSource ds = new BeeDataSource(config);

			Connection con = null;
			try {
				con = ds.getConnection();
				testResultSet("BeeCP", con);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (con != null)
					con.close();
			}
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testHikariCP() throws Exception {
		try {
			HikariConfig config2 = new HikariConfig();
			config2.setJdbcUrl(url);
			config2.setDriverClassName(driver);
			config2.setUsername(user);
			config2.setPassword(password);
			config2.setAutoCommit(false);
			HikariDataSource ds = new HikariDataSource(config2);

			Connection con = null;
			try {
				con = ds.getConnection();
				testResultSet("HikariCP", con);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (con != null)
					con.close();
			}
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testTomcatJdbc() throws Exception {
		try {

			PoolProperties p = new PoolProperties();
			p.setUrl(url);
			p.setDriverClassName(driver);
			p.setUsername(user);
			p.setPassword(password);

			DataSource datasource = new DataSource();
			datasource.setPoolProperties(p);
			DataSource ds = new DataSource();
			ds.setPoolProperties(p);

			Connection con = null;
			try {
				con = ds.getConnection();
				testResultSet("TomcatJdbc", con);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (con != null)
					con.close();
			}
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testDruid() throws Exception {
		try {

			DruidDataSource ds = new DruidDataSource();
			ds.setUrl(url);
			ds.setUsername(user);
			ds.setPassword(password);
			ds.setDriverClassName(driver);

			Connection con = null;
			try {
				con = ds.getConnection();
				testResultSet("Druid", con);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (con != null)
					con.close();
			}
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * JDBC ResultSet测试
	 */
	private static void testResultSet(String dsName, Connection con) throws Exception {
		System.out.println("*******************开始测试数据源(" + dsName + ")*******************");

		Statement st = con.createStatement();

		// 2：打开一个ResultSet
		ResultSet rs1 = st.executeQuery(SQL);

		// 3:检查由executeQuery执行的结果值必须等于Statement.getResultSet()
		String testName = "<检查由executeQuery执行的结果值必须等于Statement.getResultSet()>";
		if (st.getResultSet() == rs1) {
			System.out.println(dsName + "-通过," + testName);
		} else {
			System.out.println(dsName + "-失败," + testName);
		}

		// 4：关闭结果集合
		rs1.close();

		// 5：
		testName = "<结果集合关闭结果后，statement.getResultSet()不能再获取这个结果集合对象>";
		if (st.getResultSet() != rs1) {
			System.out.println(dsName + "-通过," + testName);
		} else {
			System.out.println(dsName + "-失败," + testName);
		}

		// 6:unwrap测试结果集对象否存在对象泄露（原生态对象不允许暴露给使用者）
		testName = "<unwrap测试ResultSet是否存在暴露问题（原生态对象不允许暴露给使用者)>";
		if (rs1.unwrap(ResultSet.class) == rs1) {
			System.out.println(dsName + "-通过," + testName);
		} else {
			System.out.println(dsName + "-失败," + testName);
		}

		// 6:unwrap测试Connection是否存在对象泄露（原生态对象不允许暴露给使用者）
		testName = "<unwrap测试Connection是否存在是否存在暴露问题（原生态对象不允许暴露给使用者)>";
		if (con.unwrap(Connection.class) == con) {
			System.out.println(dsName + "-通过," + testName);
		} else {
			System.out.println(dsName + "-失败," + testName);
		}

		// 7:再次打开一个新的结果集合
		ResultSet rs2 = st.executeQuery(SQL);

		// 8:关闭Statement是否检查由它打开的结果集合是否关闭
		st.close();

		// 9:检查关闭的这个结果集合isClosed()的返回值是否为True
		testName = "<检查关闭的这个结果集合isClosed()的返回值必须等于True>";
		if (rs2.isClosed()) {
			System.out.println(dsName + "-通过," + testName);
		} else {
			System.out.println(dsName + "-失败," + testName);
		}

		// 10:检查从一个已经关闭的Statement获取曾经打开过的结果集合，检查是否抛出异常
		testName = "<检查从一个已经关闭的Statement获取曾经打开过的结果集合要抛出异常>";
		try {
			st.getResultSet();
			System.out.println(dsName + "-失败," + testName);
		} catch (SQLException e) {
			System.out.println(dsName + "-通过," + testName);
		}
	}
}
