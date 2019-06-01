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

@SuppressWarnings("serial")
final class StatementCache extends LinkedHashMap<Object, PreparedStatement> {
	private int maxSize;
	public StatementCache(int maxSize) {
		super((int)Math.ceil(maxSize / 0.75f)+1,0.75f,true);
		this.maxSize=maxSize;
	}
	public int maxSize() {
		return maxSize;
	}
	public PreparedStatement get(Object key) {
		return isEmpty()?null:super.get(key);
	}
	
	public void clear() {
		Iterator<Map.Entry<Object, PreparedStatement>> itor = entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<Object, PreparedStatement> entry = (Map.Entry<Object, PreparedStatement>) itor.next();
			itor.remove();
			this.onRemove(entry.getKey(), entry.getValue());
		}
	}
	protected boolean removeEldestEntry(Map.Entry<Object,PreparedStatement> eldest) {
		if (size() > maxSize) {
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
