/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class ConnectionPool2 extends ConnectionPool {
	private ConcurrentLinkedQueue<Borrower> transferQueue= new ConcurrentLinkedQueue<Borrower>();
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
	}
	
	protected int getWaiterSize() {
		return transferQueue.size();
	}
	
	public PooledConnection waitForOne(long timeout, TimeUnit unit, Borrower waiter) {
		waiter.setTransferedConnection(null);

		if (transferQueue.offer(waiter)) {
			if (waiter.getTransferedConnection() == null)
				LockSupport.parkNanos(waiter,unit.toNanos(timeout));
			if (waiter.getTransferedConnection() == null)
				transferQueue.remove(waiter);  //<--- if poll out from queue by releaser,how to break loop immediately?
		}
		
		return waiter.getTransferedConnection();
	}

	public void release(final PooledConnection pConn) throws SQLException {
		if(isCompete)
			pConn.setConnectionState(PooledConnectionState.IDLE);	
		
		Borrower waiter = null;
		if ((waiter = transferQueue.poll()) != null) {
			waiter.setTransferedConnection(pConn);
			LockSupport.unpark(waiter.getThread());
			return;
		}
		
		if (!isCompete)
			pConn.setConnectionState(PooledConnectionState.IDLE);
	}
	
//	
//	/**
//	 * Concurrent Chain
//	 *
//	 * @author Chris.Liao
//	 * @version 1.0
//	 */
//	static class PoolWaiterChain{
//		private final AtomicInteger waiterSize = new AtomicInteger(0);
//		private final AtomicReference<Borrower> headRef= new AtomicReference(null);
//		private final AtomicReference<Borrower> tailRef= new AtomicReference(null);
//		
//		public int size(){
//			return waiterSize.get();
//		}
//		
//		public boolean offer(Borrower waiter) {//operation on tail
//			Borrower t=null;
//			waiter.setPre(null);
//			waiter.setNext(null);
//			
//			for (;;) {
//				t=tailRef.get();
//				if (t == null) {
//					if (tailRef.compareAndSet(null, waiter)) {
//						headRef.compareAndSet(null, waiter);
//						waiterSize.incrementAndGet();
//						return true;
//					} else {
//						continue;
//					}
//				} else {//tail is not null					
//					if(t.isOffChain()){ //has removed as head,so clean
//					  tailRef.compareAndSet(t, null);
//					  continue;
//					}
//					
//					if(t.getNext() != null) {
//						tailRef.compareAndSet(t, t.getNext());
//						continue;
//					}else if(t.compareAndSetNext(null, waiter)){//successful into chain
//						waiter.setPre(t);
//						waiterSize.incrementAndGet();
//						
//						tailRef.compareAndSet(t,waiter);
//						return true;
//					}else{
//						 continue;
//					}
//				}
//			}
//		}
//		
//		public Borrower poll() {//operation on head
//			Borrower h=null;
//			Borrower t=null;
//			Borrower n=null;
//			
//			for (;;) {
//				h=headRef.get();t=tailRef.get();
//				if(h==null && t==null)return null;//empty chain
//				if(h==null && t!=null){//need set tail as a new head
//					if(t.isOffChain()){
//						tailRef.compareAndSet(t,null);//clean tail
//						continue;
//					}else{
//						headRef.compareAndSet(null,t);
//						continue;	
//					}
//				}
//				
//				if(h!=null){
//					n=h.getNext();
//					if(headRef.compareAndSet(h,n)){//remove head 
//						
//						
//						
//					}
//				}
//				
//				
//				if(h!=null && headRef.compareAndSet(h,h.getNext())){//head has changed
//					waiterSize.decrementAndGet();
//					h.setNext(h);h.setPre(h);
//					
//	 				if(h==tailRef.get())
//	 					tailRef.compareAndSet(h, null);
//					 return h;
//				}
//			}
//		}
//		
//		//remove from chain
//		public boolean remove(Borrower o) {
//			Borrower h=null;
//			Borrower t=null;
//			Borrower pre=null;
//		
//			while (!o.isOffChain()) {//in chain
//				h=headRef.get();t=tailRef.get();
//				if(h==null && t==null)return true;//empty chain
//				if(h==null && t!=null){//need set tail as a new head
//					if(t.isOffChain()){
//						tailRef.compareAndSet(t,null);//clean tail
//						continue;
//					}else{
//						headRef.compareAndSet(null,t);
//						continue;	
//					}
//				}
//				
//				if (o == h) {// at head
//					if(o.isOffChain()){
//						headRef.compareAndSet(o,null);
//						return true;
//					}else if(headRef.compareAndSet(o,o.getNext())){
//						if(o==t)tailRef.compareAndSet(o,o.getNext());
//						waiterSize.decrementAndGet();
//						
//						o.setNext(o);
//						o.setPre(o);
//						return true;
//					}
//				}
//					 
//				pre=o.getPre();
//				if(pre!=null && pre.getNext()==o){
//					if (pre.compareAndSetNext(o, o.getNext())) {
//						if(o==t)tailRef.compareAndSet(o,o.getNext());
//						waiterSize.decrementAndGet();
//						
//						o.setNext(o);
//						o.setPre(o);
//						return true;
//					}
//				}else{
//					o.setNext(o);
//					o.setPre(o);
//					return true;
//				}
//			}
//			
//			return true;
//		}
//		
//		public void clear() {
//			headRef.set(null);
//			tailRef.set(null);
//		}
//	}
}