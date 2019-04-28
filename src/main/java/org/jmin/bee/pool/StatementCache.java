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

import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Statement cache
 *
 * @author Chris.liao
 * @version 1.0
 */

public final class StatementCache {
	private int maxSize;
	private boolean isValid;
	private LinkedHashMap<Object, PreparedStatement> cacheMap;
	
	@SuppressWarnings("serial")
	public StatementCache(int maxSize) {
		this.maxSize=maxSize;
		this.isValid=maxSize>0;
		float mapLoadFactor = 0.75f; 
		int mapInitialCapacity = (int)Math.ceil(maxSize/mapLoadFactor)+1;
		this.cacheMap = new LinkedHashMap<Object, PreparedStatement>(mapInitialCapacity, mapLoadFactor, true) {
			protected boolean removeEldestEntry(Map.Entry<Object,PreparedStatement> eldest) {
				if (this.size() > StatementCache.this.maxSize) {
					onRemove(eldest.getKey(), eldest.getValue());
					return true;
				} else {
					return false;
				}
			}
		};
	}
	public int maxSize() {
		return this.maxSize;
	}
	public boolean isValid() {
		return isValid;
	}
	public int size() {
		return this.cacheMap.size();
	}
	public PreparedStatement getStatement(Object key) {
		return (this.cacheMap.size()== 0)?null:this.cacheMap.get(key) ;
	}
	public void putStatement(Object key, PreparedStatement value) {
		if(maxSize>0){this.cacheMap.put(key, value);}
	}
	public void clearAllStatement() {
		Iterator<Map.Entry<Object, PreparedStatement>> itor = this.cacheMap.entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<Object, PreparedStatement> entry = (Map.Entry<Object, PreparedStatement>) itor.next();
			itor.remove();
			this.onRemove(entry.getKey(), entry.getValue());
		}
	}
	void onRemove(Object key, PreparedStatement obj) {
		try {
			 ((PreparedStatement) obj).close();
		} catch (Throwable e) {}
	}
}
