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

import static cn.beecp.util.BeecpUtil.oclose;
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
	BeeDataSourceConfig pConfig;
	Connection rawConn;

	ProxyConnectionBase proxyConn;
	volatile long lastAccessTime;
	boolean commitDirtyInd;
	boolean curAutoCommit;
	private FastConnectionPool pool;
	private static Logger log = LoggerFactory.getLogger(PooledConnection.class);

	//changed indicator
	private boolean[] changedInds=new boolean[6]; //0:autoCommit,1:transactionIsolation,2:readOnly,3:catalog,4:schema,5:networkTimeout
	private short changedCount=0;

	public PooledConnection(Connection rawConn,int connState,FastConnectionPool connPool,BeeDataSourceConfig config)throws SQLException{
		super(config.getPreparedStatementCacheSize());
		pool=connPool;
		state=connState;
		this.rawConn=rawConn;

		pConfig=config;
		curAutoCommit=pConfig.isDefaultAutoCommit();
		stmCacheValid = pConfig.getPreparedStatementCacheSize()>0;
		updateAccessTime();
	}
	public String toString() { return rawConn.toString();}
	public boolean equals(Object obj) { return this==obj;}

	void closeRawConn() {//called by pool
		if(proxyConn!=null){
			proxyConn.setAsClosed();
			proxyConn=null;
		}

		resetRawConnOnReturn();
		if(stmCacheValid)
			this.clearStatement();
		oclose(rawConn);
	}

	//***************called by connection proxy ********//
	void returnToPoolBySelf(){
		proxyConn=null;
		if(resetRawConnOnReturn())
			pool.recycle(this);
		else
		    pool.abandonOnReturn(this);
	}
	void setCurAutoCommit(boolean curAutoCommit) {
		this.curAutoCommit = curAutoCommit;
	}
	void updateAccessTime() {
		lastAccessTime = currentTimeMillis();
	}
	void updateAccessTimeWithCommitDirty() {
		commitDirtyInd=!curAutoCommit;
		lastAccessTime=currentTimeMillis();
	}
    void setChangedInd(int pos,boolean changed){
		if(!changedInds[pos] && changed)//false ->true      + 1
		   changedCount++;
		else if(changedInds[pos] && !changed)//true-->false  -1
		   changedCount--;
		changedInds[pos]=changed;
		updateAccessTime();
    }

    boolean isSupportSchema() {
		return pool.isSupportSchema();
	}
	boolean isSupportNetworkTimeout() {
		return  pool.isSupportNetworkTimeout();
	}
	//reset connection on return to pool
	private boolean updTimeInReset;
	private boolean resetRawConnOnReturn() {
		updTimeInReset=false;
		try {
			if (!curAutoCommit && commitDirtyInd) {//Roll back when commit dirty
				rawConn.rollback();
				commitDirtyInd = false;
				updTimeInReset=true;
			}
			//reset begin
			if (changedCount > 0) {
				if (changedInds[0]) {//reset autoCommit
					rawConn.setAutoCommit(pConfig.isDefaultAutoCommit());
					curAutoCommit = pConfig.isDefaultAutoCommit();
                    changedInds[0] = false;
				}
				if (changedInds[1]) {
					rawConn.setTransactionIsolation(pConfig.getDefaultTransactionIsolationCode());
					changedInds[1] = false;
				}
				if (changedInds[2]) {//reset readonly
					rawConn.setReadOnly(pConfig.isDefaultReadOnly());
					changedInds[2] = false;
				}
				if (changedInds[3]) {//reset catalog
					rawConn.setCatalog(pConfig.getDefaultCatalog());
					changedInds[3] = false;
				}
				//for JDK1.7 begin
				if (changedInds[4] && isSupportSchema()) {//reset shema
					rawConn.setSchema(pConfig.getDefaultSchema());
					changedInds[4] = false;
				}
				if (changedInds[5] && isSupportNetworkTimeout()) {//reset networkTimeout
					rawConn.setNetworkTimeout(pool.getNetworkTimeoutExecutor(), pool.getNetworkTimeout());
					changedInds[5] = false;
				}
				//for JDK1.7 end
				changedCount=0;
				updTimeInReset=true;
			}//reset end
			if(updTimeInReset)updateAccessTime();

			//clear warnings
			rawConn.clearWarnings();
			return true;
		} catch (Throwable e) {
			log.error("Connection reset error", e);
			return false;
		}
	}
}
