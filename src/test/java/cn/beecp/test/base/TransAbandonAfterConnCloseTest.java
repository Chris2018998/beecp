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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class TransAbandonAfterConnCloseTest extends TestCase {
	private BeeDataSource ds;
	public void setUp() throws Throwable {
		BeeDataSourceConfig config = new BeeDataSourceConfig();
		config.setJdbcUrl(Config.JDBC_URL);
		config.setDriverClassName(Config.JDBC_DRIVER);
		config.setUsername(Config.JDBC_USER);
		config.setPassword(Config.JDBC_PASSWORD);
		ds = new BeeDataSource(config);
	}

	public void tearDown() throws Throwable {
		ds.close();
	}

	public void test() throws InterruptedException, Exception {
		Connection con1 = null;
		PreparedStatement ps1=null;
		ResultSet re1=null;
		PreparedStatement ps2=null;
		String userId= String.valueOf(new Random(Long.MAX_VALUE).nextLong());

		try {
			con1 = ds.getConnection();
			con1.setAutoCommit(false);
			ps1=con1.prepareStatement("select count(*) from "+ Config.TEST_TABLE+" where TEST_ID='"+userId+"'");
			re1=ps1.executeQuery();
			if(re1.next()){
				 int size=re1.getInt(1);
				 if (size !=0)
					 TestUtil.assertError("record size error");
			}
			
			ps2=con1.prepareStatement("insert into "+ Config.TEST_TABLE+"(TEST_ID,TEST_NAME)values(?,?)");
			ps2.setString(1, userId);
			ps2.setString(2, userId);
			int rows=ps2.executeUpdate();
			if (rows !=1)
				TestUtil.assertError("Failed to insert");
		} finally {
			BeecpUtil.oclose(re1);
			BeecpUtil.oclose(ps1);
			BeecpUtil.oclose(ps2);
			BeecpUtil.oclose(con1);
		}
		
		Connection con2 = null;
		PreparedStatement ps3=null;
		ResultSet re3=null;
		
		try {
			con2 = ds.getConnection();
			ps3=con2.prepareStatement("select count(*) from "+ Config.TEST_TABLE+" where TEST_ID='"+userId+"'");
			re3=ps3.executeQuery();
			if(re3.next()){
				 int size=re3.getInt(1);
				 if (size !=0)
					 TestUtil.assertError("rollback failed");
			}
		} finally {
			BeecpUtil.oclose(re3);
			BeecpUtil.oclose(ps3);
			BeecpUtil.oclose(con2);
		}
	}
}
