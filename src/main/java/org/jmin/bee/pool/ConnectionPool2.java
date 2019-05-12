/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool;

import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public final class ConnectionPool2 extends ConnectionPool {
	private final int NCPUS=Runtime.getRuntime().availableProcessors();
	private final int maxTimedSpins = (NCPUS<2)?0:32;
	private LinkedBlockingQueue<Borrower> transferQueue= new LinkedBlockingQueue<Borrower>();
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
	}
	
	public int getWaiterSize() {
		return transferQueue.size();
	}

	public PooledConnection takeOneTransferConnection(long timeout,TimeUnit unit,Borrower borrower) {
		int spins = maxTimedSpins;
		borrower.setTransferedConnection(null);
		
		if (transferQueue.offer(borrower)) {
			timeout = unit.toNanos(timeout);
			final long deadline = System.nanoTime() + timeout;

			for (;;) {// spin
				if (borrower.getTransferedConnection() != null)
					break;
				if ((timeout = deadline - System.nanoTime()) <= 0L)
					break;
		
				if (spins > 0)spins--;
				if (spins== 0)LockSupport.parkNanos(timeout);
			}
			
			if (borrower.getTransferedConnection()==null)
				transferQueue.remove(borrower);
		}
		
		return borrower.getTransferedConnection();
	}

	public void releasePooledConnection(final PooledConnection pooledConnection) throws SQLException {
		boolean isFairMode=poolInfo.isFairMode();
				
		if(!isFairMode)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);	
		
		Borrower waiter=transferQueue.poll();
		if (waiter != null) {
			waiter.setTransferedConnection(pooledConnection);
			LockSupport.unpark(waiter.getThread());
			return;
		}
		 
		if (isFairMode)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
	}
}


/**
 * waiter chain
 *
 * @author Chris.Liao
 * @version 1.0
 */
final class PoolWaiterChain {
	private final AtomicInteger waiterSize = new AtomicInteger(0);
	private final AtomicReference<Borrower> headRef= new AtomicReference(null);
	private final AtomicReference<Borrower> tailRef= new AtomicReference(null);
	
	public int size(){
		return waiterSize.get();
	}
	
	public boolean offer(Borrower waiter) {//operation on tail
		Borrower h=null;
		Borrower t=null;
		waiter.setPre(null);
		waiter.setNext(null);
		
		for (;;) {
			h=headRef.get();t=tailRef.get();
			if(h==null && t==null){//empty chain
				if(tailRef.compareAndSet(null, waiter)){
					waiterSize.incrementAndGet();
					headRef.compareAndSet(null, waiter);
					return true;
				}else{
					continue;
				}
			}else if(h!=null && t==null){//tail is null,but head is not null
				tailRef.compareAndSet(null, h);
				continue;
			}else if(h!=null && t!=null){//
				if(t.isOffChain()){ //has removed as head,so clean
				  tailRef.compareAndSet(t, null);
				  continue;
				}
				
				if(t.getNext() != null) {
					tailRef.compareAndSet(t, t.getNext());
					continue;
				}else if(t.compareAndSetNext(null, waiter)){//successful into chain
					waiter.setPre(t);
					waiterSize.incrementAndGet();
					
					tailRef.compareAndSet(tailRef.get(),waiter);
					return true;
				}else{
					 continue;
				}
			}
		}
	}
	
	public Borrower poll() {//operation on head
		Borrower h=null;
		Borrower t=null;
		
		for (;;) {
			h=headRef.get();t=tailRef.get();
			if(h==null && t==null)return null;//empty chain
			if(h==null && t!=null){//need set tail as a new head
				if(t.isOffChain()){
					tailRef.compareAndSet(t,null);//clean tail
					continue;
				}else{
					headRef.compareAndSet(null,t);
					continue;	
				}
			}
			
			if(h!=null && headRef.compareAndSet(h,h.getNext())){//head has changed
				waiterSize.decrementAndGet();
				h.setNext(h);h.setPre(h);
 				if(h==tailRef.get())
 					tailRef.compareAndSet(h, null);
				 return h;
			}
		}
	}
	
	//remove from chain
	public boolean remove(Borrower o) {
		Borrower h=null;
		Borrower t=null;
		Borrower pre=null;
	
		while (!o.isOffChain()) {//in chain
			h=headRef.get();t=tailRef.get();
			if(h==null && t==null)return true;//empty chain
			if(h==null && t!=null){//need set tail as a new head
				if(t.isOffChain()){
					tailRef.compareAndSet(t,null);//clean tail
					continue;
				}else{
					headRef.compareAndSet(null,t);
					continue;	
				}
			}
			
			if (o == h) {// at head
				if(o.isOffChain()){
					headRef.compareAndSet(o,null);
					return true;
				}else if(headRef.compareAndSet(o,o.getNext())){
					if(o==t)tailRef.compareAndSet(o,o.getNext());
					waiterSize.decrementAndGet();
					
					o.setNext(o);
					o.setPre(o);
					return true;
				}
			}
				 
			pre=o.getPre();
			if(pre!=null && pre.getNext()==o){
				if (pre.compareAndSetNext(o, o.getNext())) {
					if(o==t)tailRef.compareAndSet(o,o.getNext());
					waiterSize.decrementAndGet();
					
					o.setNext(o);
					o.setPre(o);
					return true;
				}
			}else{
				o.setNext(o);
				o.setPre(o);
				return true;
			}
		}
		
		return true;
	}
	
	public void clear() {
		headRef.set(null);
		tailRef.set(null);
	}
}


