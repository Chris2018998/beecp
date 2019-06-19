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

/**
 * Pooled Connection store array
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class PooledConnectionList {
	private volatile PooledConnection[] array = new PooledConnection[0];
	public int size() {
		return array.length;
	}
	public PooledConnection[] getArray() {
		return array;
	}
	void setArray(PooledConnection[] a) {
		array = a;
	}
	
	public synchronized void add(PooledConnection pooledCon,boolean atHead) {
		final PooledConnection[] arrayOld=array;
		int oldLen = arrayOld.length;
		PooledConnection[] arrayNew = new PooledConnection[oldLen + 1];
		if(atHead){
			arrayNew[0] = pooledCon;//add at head
			System.arraycopy(arrayOld, 0, arrayNew, 1, oldLen);
		}else{
			System.arraycopy(arrayOld, 0, arrayNew, 0, oldLen);
			arrayNew[oldLen] = pooledCon;
		}
		setArray(arrayNew);
	}
	
	public synchronized void addAll(List<PooledConnection> col) {
		final PooledConnection[] arrayOld=array;
		int oldLen=arrayOld.length;
		
		int addLen=col.size();
		PooledConnection[] arrayAdd =col.toArray(new PooledConnection[addLen]);
		PooledConnection[] arrayNew = new PooledConnection[oldLen+addLen];
		System.arraycopy(arrayAdd,0,arrayNew,0,addLen);//add at head
		System.arraycopy(arrayOld,0,arrayNew,addLen,oldLen);
		setArray(arrayNew); 
	}
	
	public synchronized void remove(PooledConnection pooledCon){ 
		PooledConnection[] arrayOld=array;
		int index =-1;
		for (int i=0,l=arrayOld.length;i<l;i++) {
			 if(arrayOld[i]==pooledCon){
				 index=i;
				 break;
			 }
		}
		
		if(index >=0){
			PooledConnection[] arrayNew = new PooledConnection[arrayOld.length-1];
			if(index==0){
				System.arraycopy(arrayOld,1,arrayNew,0,arrayNew.length);
			}else if(index==arrayOld.length-1){
				System.arraycopy(arrayOld,0, arrayNew,0, arrayNew.length);
			}else{
				System.arraycopy(arrayOld,0,arrayNew,0,index+1);
				System.arraycopy(arrayOld,index+1, arrayNew,index,(arrayOld.length-index-1));
			}
			setArray(arrayNew);
		}
	}
	
	public synchronized void removeAll(List<PooledConnection> col){ 
		PooledConnection[] arrayOld=array;
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
	}
}
