/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;
import cn.beecp.util.BeecpUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

public class ConnectionGetTimeoutTest extends TestCase {
	private BeeDataSource ds;

	public void setUp() throws Throwable {
		BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setJdbcUrl(Config.JDBC_URL);// give valid URL
		config.setDriverClassName(Config.JDBC_DRIVER);
		config.setUsername(Config.JDBC_USER);
		config.setPassword(Config.JDBC_PASSWORD);
		config.setMaxWait(3000);
		config.setMaxActive(1);
		config.setBorrowConcurrentSize(1);
		ds = new BeeDataSource(config);
	}

	public void tearDown() throws Throwable {
		ds.close();
	}

	class TestThread extends Thread {
		SQLException e = null;
		CountDownLatch lacth;

		TestThread(CountDownLatch lacth) {
			this.lacth = lacth;
		}

		public void run() {
			Connection con2 = null;
			try {
				con2 = ds.getConnection();
			} catch (SQLException e) {
				this.e = e;
			} finally {
				if(con2!=null)
				  BeecpUtil.oclose(con2);
			}
			lacth.countDown();
		}
	}

	public void test() throws InterruptedException, Exception {
		Connection con = null;
		try {
			con = ds.getConnection();
			CountDownLatch lacth = new CountDownLatch(1);
			TestThread testTh = new TestThread(lacth);
			testTh.start();
			
			lacth.await();
			if (testTh.e == null)
				TestUtil.assertError("Connect timeout test failed");
			else
				System.out.println(testTh.e);
		} finally {
			if(con!=null)
			  BeecpUtil.oclose(con);
		}
	}
}
