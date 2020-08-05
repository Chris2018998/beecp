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
package cn.beecp.pool;

import cn.beecp.BeeDataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadPoolExecutor;

import static cn.beecp.util.BeecpUtil.oclose;
import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;

/**
 * Pooled Connection
 *
 * @author Chris.Liao
 * @version 1.0
 */
class PooledConnection extends StatementCache{
	volatile int state;
	boolean stmCacheValid;
	Connection rawConn;
	ProxyConnectionBase proxyConn;

	volatile long lastAccessTime;
	boolean commitDirtyInd;
	boolean curAutoCommit;
	boolean defaultAutoCommit;
	int defaultTransactionIsolationCode;
	boolean defaultReadOnly;
	String defaultCatalog;
	String defaultSchema;
	int defaultNetworkTimeout;
	private ThreadPoolExecutor defaultNetworkTimeoutExecutor;

	private FastConnectionPool pool;
	private short changedCount;
	//changed indicator
	private boolean[] changedInd=new boolean[6];
	private static final boolean[] DEFAULT_IND=new boolean[]{false,false,false,false,false,false};
	private static Logger log = LoggerFactory.getLogger(PooledConnection.class);

	public PooledConnection(Connection rawConn,int connState,FastConnectionPool connPool,BeeDataSourceConfig config)throws SQLException{
		super(config.getPreparedStatementCacheSize());
		pool=connPool;
		state=connState;
		this.rawConn=rawConn;

		//default value
		defaultAutoCommit=config.isDefaultAutoCommit();
		defaultTransactionIsolationCode=config.getDefaultTransactionIsolationCode();
		defaultReadOnly=config.isDefaultReadOnly();
		defaultCatalog=config.getDefaultCatalog();
		defaultSchema=config.getDefaultSchema();
		defaultNetworkTimeout=pool.getNetworkTimeout();
		defaultNetworkTimeoutExecutor=pool.getNetworkTimeoutExecutor();

		curAutoCommit=defaultAutoCommit;
		stmCacheValid = config.getPreparedStatementCacheSize()>0;
		lastAccessTime=currentTimeMillis();
	}
	void closeRawConn() {//called by pool
		try{
			if(proxyConn!=null) {
				proxyConn.setAsClosed();
				proxyConn = null;
			}
			if(stmCacheValid)this.clearStatement();
			resetRawConnOnReturn();
		}catch(SQLException e) {
			log.error("Connection close error", e);
		}finally{
			oclose(rawConn);
		}
	}

	//***************called by connection proxy ********//
	void returnToPoolBySelf()throws SQLException{
		try{
			proxyConn=null;
			resetRawConnOnReturn();
			pool.recycle(this);
		}catch(SQLException e) {
			pool.abandonOnReturn(this);
			throw e;
		}
	}
	void updateAccessTimeWithCommitDirty() {
		commitDirtyInd=!curAutoCommit;
		lastAccessTime=currentTimeMillis();
	}
    void setChangedInd(int pos,boolean changed){
		if(!changedInd[pos] && changed)//false ->true       +1
		   changedCount++;
		else if(changedInd[pos] && !changed)//true-->false  -1
		   changedCount--;
		changedInd[pos]=changed;
		//lastAccessTime=currentTimeMillis();
	}
	boolean isSupportValidTest() {return pool.isSupportValidTest();}
    boolean isSupportSchema() {
		return pool.isSupportSchema();
	}
	boolean isSupportNetworkTimeout() {
		return  pool.isSupportNetworkTimeout();
	}

	void resetRawConnOnReturn()throws SQLException {
		if (!curAutoCommit && commitDirtyInd) {//Roll back when commit dirty
			rawConn.rollback();
			commitDirtyInd = false;
		}
		//reset begin
		if (changedCount > 0) {
			if (changedInd[0]) {//reset autoCommit
				rawConn.setAutoCommit(defaultAutoCommit);
				curAutoCommit = defaultAutoCommit;
			}
			if (changedInd[1])
				rawConn.setTransactionIsolation(defaultTransactionIsolationCode);
			if (changedInd[2]) //reset readonly
				rawConn.setReadOnly(defaultReadOnly);
			if (changedInd[3]) //reset catalog
				rawConn.setCatalog(defaultCatalog);

			//for JDK1.7 begin
			if (changedInd[4]) //reset schema
				rawConn.setSchema(defaultSchema);
			if (changedInd[5]) //reset networkTimeout
				rawConn.setNetworkTimeout(defaultNetworkTimeoutExecutor, defaultNetworkTimeout);
			//for JDK1.7 end

			changedCount = 0;
			arraycopy(DEFAULT_IND, 0, changedInd, 0, 6);
		}//reset end

		//clear warnings
		rawConn.clearWarnings();
	}
}
