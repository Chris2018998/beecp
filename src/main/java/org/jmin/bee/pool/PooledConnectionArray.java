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

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
/**
 * Pooled Connection store array
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class PooledConnectionArray {
	private final ReentrantLock lock = new ReentrantLock();
	private volatile PooledConnection[] array = new PooledConnection[0];
	final int size() {
		return array.length;
	}
	final PooledConnection[] getArray() {
		return array;
	}
	final void setArray(PooledConnection[] a) {
		array = a;
	}
	void add(PooledConnection pooledCon) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			final PooledConnection[] arrayOld=getArray();
			int oldLen = arrayOld.length;
			PooledConnection[] arrayNew = new PooledConnection[oldLen + 1];
			System.arraycopy(arrayOld, 0, arrayNew, 0, oldLen);
			arrayNew[oldLen] = pooledCon;
			setArray(arrayNew);
		} finally {
			lock.unlock();
		}
	}
	void addAll(List<PooledConnection> col) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try{
			final PooledConnection[] arrayOld=getArray();
			int oldLen=arrayOld.length;
			
			int addLen=col.size();
			PooledConnection[] arrayAdd =col.toArray(new PooledConnection[addLen]);
			PooledConnection[] arrayNew = new PooledConnection[oldLen+addLen];
			System.arraycopy(arrayOld,0,arrayNew,0,oldLen);
			//fix issue:#2 There are a problem in class. Chris-2019-05-01 begin
			//System.arraycopy(arrayAdd,0,arrayNew,0,addLen);
			System.arraycopy(arrayAdd,0,arrayNew,oldLen,addLen);
			//fix issue:#2 There are a problem in class. Chris-2019-05-01 end
			
			setArray(arrayNew);
		}finally{
			lock.unlock();
		}
	}
	void removeAll(List<PooledConnection> col){ 
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try{
			PooledConnection[] arrayOld=getArray();
			PooledConnection[] tempNew = new PooledConnection[arrayOld.length];
			PooledConnection[] arrayRemove = col.toArray(new PooledConnection[col.size()]);
			
			int tempIndex = 0;
			boolean needRemove=false;
			for (PooledConnection p1:arrayOld) {
				needRemove=false;
				for (PooledConnection p2:arrayRemove) {
					if (p1 == p2) {
						needRemove=true;
 						break;
 					}
				}
				if (!needRemove)tempNew[tempIndex++]=p1;
			}
			
			PooledConnection[] arrayNew = new PooledConnection[tempIndex];
			System.arraycopy(tempNew,0,arrayNew, 0, tempIndex);
			setArray(arrayNew);
		}finally{
			lock.unlock();
		}
	}
}
