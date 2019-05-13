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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

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
	
	private final Queue<Borrower> transferQueue;//when waiter chain stable,then replace it
	//private final poolWaiterChain transferQueue;
	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
		if(isCompeteMode){
			transferQueue= new ConcurrentLinkedQueue<Borrower>();
		    //poolWaiterChain = new ConcurrentChain();
		}else{
			transferQueue= new LinkedBlockingQueue<Borrower>();
			//poolWaiterChain = new BlockingChain();
		}
	}
	
	public int getWaiterSize() {
		if(isCompeteMode){
			return waiterSize.get();
		}else{
			return transferQueue.size();
		}
		//return poolWaiterChain.size();
	}

	public PooledConnection takeOneTransferConnection(long timeout,TimeUnit unit,Borrower borrower) {
		int spins = maxTimedSpins;
		borrower.setTransferedConnection(null);
		
		try{
			if(isCompeteMode)waiterSize.incrementAndGet();
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
		}finally{
			if(isCompeteMode)waiterSize.decrementAndGet();
		}
		return borrower.getTransferedConnection();
	}

	public void releasePooledConnection(final PooledConnection pooledConnection) throws SQLException {
		if(isCompeteMode)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);	
		
		Borrower waiter=transferQueue.poll();
		if (waiter != null) {
			waiter.setTransferedConnection(pooledConnection);
			LockSupport.unpark(waiter.getThread());
			return;
		}
		 
		if (!isCompeteMode)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
	}

	/**
	 * waiter chain(Doubly-linked chain similar with LinkedList construction)
	 *
	 * @author Chris.Liao
	 * @version 1.0
	 */
	static interface PoolWaiterChain {
		public int size();
		public Borrower poll();
		public boolean offer(Borrower o);
		public boolean remove(Borrower o);
		public void clear();
	}
	
	/**
	 * Concurrent Chain
	 *
	 * @author Chris.Liao
	 * @version 1.0
	 */
	static class ConcurrentChain implements PoolWaiterChain{
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

	/**
	 * Blocking Chain
	 *
	 * @author Chris.Liao
	 * @version 1.0
	 */
	static class BlockingChain implements PoolWaiterChain{
		private Borrower last;
		private final Borrower head; 
		private final int capacity;
		private final AtomicInteger count = new AtomicInteger();
		private final ReentrantLock takeLock = new ReentrantLock();
		private final Condition notEmpty = takeLock.newCondition();
		private final ReentrantLock putLock = new ReentrantLock();
		private final Condition notFull = putLock.newCondition();

		public BlockingChain() {
			this(Integer.MAX_VALUE);
		}
		public BlockingChain(int capacity) {
			this.capacity=capacity;
			last = head = new Borrower();
		}
		public int size() {
			return count.get();
		}
		private void fullyLock() {
			putLock.lock();
			takeLock.lock();
		}
		private void fullyUnlock() {
			takeLock.unlock();
			putLock.unlock();
		}
		private void signalNotEmpty() {
			final ReentrantLock takeLock = this.takeLock;
			takeLock.lock();
			try {
				notEmpty.signal();
			} finally {
				takeLock.unlock();
			}
		}
		private void signalNotFull() {
			final ReentrantLock putLock = this.putLock;
			putLock.lock();
			try {
				notFull.signal();
			} finally {
				putLock.unlock();
			}
		}

		private void enqueue(Borrower node) {
			if(last==null)last=head;
			
			last.setNext(node);
			node.setPre(last);
			last = node;
		}
		
		private Borrower dequeue() {
			Borrower second=null;
			Borrower first =head.getNext();
			if(first!=null){
				first.setNext(null);
				first.setPre(null);
				second = first.getNext();
			}
			
			if(second!=null){
				head.setNext(second);
				second.setPre(head);
			}
			
			return first;
		}
		
		public boolean offer(Borrower e) {
			final AtomicInteger count = this.count;
			if (count.get() == capacity)
				return false;
			
			int c = -1;
			final ReentrantLock putLock = this.putLock;
			putLock.lock();
			try {
				if (count.get() < capacity) {
					enqueue(e);
					c = count.getAndIncrement();
					if (c + 1 < capacity)
						notFull.signal();
				}
			} finally {
				putLock.unlock();
			}
			if (c == 0)
				signalNotEmpty();
			return c >= 0;
		}

		public Borrower poll() {
			final AtomicInteger count = this.count;
			if (count.get() == 0)
				return null;

			Borrower x = null;
			int c = -1;
			final ReentrantLock takeLock = this.takeLock;
			takeLock.lock();
			try {
				if (count.get() > 0) {
					x = dequeue();
					c = count.getAndDecrement();
					if (c > 1)
						notEmpty.signal();
				}
			} finally {
				takeLock.unlock();
			}
			if (c == capacity)
				signalNotFull();
			return x;
		}
		
		public boolean remove(Borrower o) {
			Borrower pre = o.getPre();
			Borrower next = o.getNext();

			if (pre != null) {
				fullyLock();
				try {
					pre.setNext(next);
					if (next != null)
						next.setPre(pre);
					o.setPre(null);
					o.setNext(null);
					count.decrementAndGet();
					return true;
				} finally {
					fullyUnlock();
				}
			} else {
				return false;
			}
		}

		public void clear() {
			fullyLock();
			
			try {
				Borrower first = head.getNext();
				head.setNext(null);
				last=null;
				
				for(;;){
					if(first!=null){
						first.setNext(null);
						first.setPre(null);
						first =  first.getNext();
					}else{
						break;
					}
				}
				if (count.getAndSet(0) == capacity)
					notFull.signal();
			} finally {
				fullyUnlock();
			}
		}
	}
}