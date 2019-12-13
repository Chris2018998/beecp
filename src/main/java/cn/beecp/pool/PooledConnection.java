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

import static cn.beecp.pool.PoolObjectsState.CONNECTION_IDLE;
import static cn.beecp.util.BeecpUtil.oclose;
import static cn.beecp.util.BeecpUtil.isNullText;
import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.beecp.BeeDataSourceConfig;

/**
 * Pooled Connection
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class PooledConnection{
	volatile int state;
	boolean stmCacheValid;
	StatementCache stmCache=null;
	BeeDataSourceConfig poolConfig;
	Connection rawConnection;
	ProxyConnectionBase proxyConnection;
	long lastAccessTime;
	boolean rollbackOnReturn;

	private ConnectionPool pool;
	private boolean curAutoCommit;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	//changed indicator
	private boolean[] changedInds=new boolean[4]; //0:autoCommit,1:transactionIsolation,2:readOnly,3:catalog
	private byte changedBitVal =Byte.parseByte("0000",2); //pos:last ---> head;0:unchanged,1:changed
	static short Pos_AutoCommitInd=0;
	static short Pos_TransactionIsolationInd=1;
	static short Pos_ReadOnlyInd=2;
	static short Pos_CatalogInd=3;
	
	public PooledConnection(Connection rawConn,ConnectionPool connPool,BeeDataSourceConfig config)throws SQLException{
		 this(rawConn,connPool,config,CONNECTION_IDLE);
	}
	
	public PooledConnection(Connection rawConn,ConnectionPool connPool,BeeDataSourceConfig config,int connState)throws SQLException{
		pool=connPool;
		this.rawConnection= rawConn;

		state=connState;
		poolConfig=config;
		curAutoCommit=poolConfig.isDefaultAutoCommit();
		rollbackOnReturn=poolConfig.isRollbackOnReturn();
		if(stmCacheValid=poolConfig.getPreparedStatementCacheSize()>0)
		  stmCache = new StatementCache(poolConfig.getPreparedStatementCacheSize());
		setDefault();
		updateAccessTime();
	}
	
	private void setDefault()throws SQLException{
		rawConnection.setAutoCommit(poolConfig.isDefaultAutoCommit());
		rawConnection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
		rawConnection.setReadOnly(poolConfig.isDefaultReadOnly());
		if(!isNullText(poolConfig.getDefaultCatalog()))
			rawConnection.setCatalog(poolConfig.getDefaultCatalog());
	}
	
	public void updateAccessTime() {
	  lastAccessTime=currentTimeMillis();
	}
	public boolean equals(Object obj) {
		return this==obj;
	}
	
	int getChangedInd(int pos){
		return (changedBitVal >>pos)&1 ;
	}
    void setChangedInd(int pos,boolean changed){
		changedInds[pos]=changed;
    	changedBitVal^=(changedBitVal&(1<<pos))^((changed?1:0)<<pos);
    }
	void setCurAutoCommit(boolean curAutoCommit) {
		this.curAutoCommit = curAutoCommit;
	}

	//reset connection on return to pool
	private void resetConnectionBeforeRelease() {

		//rollback
		if(!curAutoCommit && rollbackOnReturn){
			try {
				rawConnection.rollback();
			} catch (SQLException e) {
				log.error("Failed to rollback on return to pool",e);
			}
		}
		
		//reset begin 
		if (changedBitVal > 0) {// exists field changed
			if (changedInds[0]) {
				try {
					rawConnection.setAutoCommit(poolConfig.isDefaultAutoCommit());
					changedInds[0]=false;
					updateAccessTime();
				} catch (SQLException e) {
					log.error("Failed to reset autoCommit to:{}",poolConfig.isDefaultAutoCommit(),e);
				}
			}

			if (changedInds[1]) {
				try {
					rawConnection.setTransactionIsolation(poolConfig.getDefaultTransactionIsolation());
					changedInds[1] = false;
					updateAccessTime();
				} catch (SQLException e) {
					log.error("Failed to reset transactionIsolation to:{}",poolConfig.getDefaultTransactionIsolation(),e);
				}
			}

			if (changedInds[2]) {
				try {
					rawConnection.setReadOnly(poolConfig.isDefaultReadOnly());
					changedInds[2] = false;
					updateAccessTime();
				} catch (SQLException e) {
					log.error("Failed to reset readOnly to:{}",poolConfig.isDefaultReadOnly(),e);
				}
			}

			if (changedInds[3]) {
				try {
					rawConnection.setCatalog(poolConfig.getDefaultCatalog());
					changedInds[3] = false;
					updateAccessTime();
				} catch (SQLException e) {
					log.error("Failed to reset catalog to:{}",poolConfig.getDefaultCatalog(),e);
				}
			}
			
			changedBitVal=0;
		}
		//reset end
		
		try {
			rawConnection.clearWarnings();
		} catch (SQLException e) {
			log.error("Failed to clear warnings",e);
		}
	}
	
	//called for pool
	void closePhysicalConnection() {
		try{
			resetConnectionBeforeRelease();
			if(stmCache!=null){
			  stmCache.clear();
			  stmCache=null;
			}
			
			poolConfig=null;
			oclose(rawConnection);
		}finally{
			if(proxyConnection!=null){
				proxyConnection.setConnectionDataToNull();
				proxyConnection = null;
			}
		}
	}
    void returnToPoolBySelf(){
    	try{
			resetConnectionBeforeRelease();
			pool.release(this,true);
    	}finally{
    		proxyConnection = null;
		}
	} 
}