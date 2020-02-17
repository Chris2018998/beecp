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
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.TestUtil;
import cn.beecp.util.BeecpUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class PoolDelayInitializeFailedTest extends TestCase {
	private int initSize=5;
	public void setUp() throws Throwable {}
	public void tearDown() throws Throwable {}
	
	public void testPoolInit() throws InterruptedException, Exception {
		Connection con=null;
		BeeDataSource ds=null;
		try{
			ds = new BeeDataSource();
			ds.setJdbcUrl("jdbc:mysql://localhost/test2");//give valid URL
			ds.setDriverClassName(Config.JDBC_DRIVER);
			ds.setUsername(Config.JDBC_USER);
			ds.setPassword(Config.JDBC_PASSWORD);
			ds.setInitialSize(initSize);
			con=ds.getConnection();
			TestUtil.assertError("A pool fail to init e need be thrown,but not"); 
		}catch(SQLException e){
			System.out.println(e.getCause());
		}finally{
			if(con!=null)
			  BeecpUtil.oclose(con);
		}
	}
}
