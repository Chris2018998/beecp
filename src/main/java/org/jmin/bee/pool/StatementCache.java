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

import org.jmin.bee.pool.util.ConnectionUtil;

/**
 * Statement cache
 *
 * @author Chris.liao
 * @version 1.0
 */

final class StatementCache extends LinkedHashMap<StatementCacheKey, PreparedStatement> {
	private int maxSize;
	private boolean isValid;
	public StatementCache(int maxSize) {
		super((maxSize==0)?0:(int)Math.ceil(maxSize / 0.75f)+1,0.75f,true);
		this.maxSize = maxSize;
		this.isValid = maxSize > 0;
	}
	public int maxSize() {
		return maxSize;
	}
	public boolean isValid() {
		return isValid;
	}
	public PreparedStatement get(StatementCacheKey key) {
		return (isEmpty()) ? null : super.get(key);
	}
	public PreparedStatement put(StatementCacheKey key, PreparedStatement ps) {
		return super.put(key, ps);
	}
	public void clear() {
		Iterator<Map.Entry<StatementCacheKey, PreparedStatement>> itor = entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<StatementCacheKey, PreparedStatement> entry = (Map.Entry<StatementCacheKey, PreparedStatement>) itor.next();
			itor.remove();
			this.onRemove(entry.getKey(), entry.getValue());
		}
	}
	protected boolean removeEldestEntry(Map.Entry<StatementCacheKey,PreparedStatement> eldest) {
		if (size() > StatementCache.this.maxSize) {
			onRemove(eldest.getKey(), eldest.getValue());
			return true;
		} else {
			return false;
		}
	}
	void onRemove(Object key, PreparedStatement obj) {
		ConnectionUtil.close(obj);
	}
}
