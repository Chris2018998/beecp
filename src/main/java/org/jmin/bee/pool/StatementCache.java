/*
 * Copyright (C) Chris Liao
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

public class StatementCache {
	private int maxSize;
	private LinkedHashMap<Object, PreparedStatement> objectMap;
	
	@SuppressWarnings("serial")
	public StatementCache(final int maxSize) {
		this.maxSize = maxSize;
		this.objectMap = new LinkedHashMap<Object, PreparedStatement>(maxSize, 0.75F, true) {
			protected boolean removeEldestEntry(Map.Entry<Object, PreparedStatement> eldest) {
				if (size() > maxSize) {
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
	public int size() {
		return this.objectMap.size();
	}
	public PreparedStatement getStatement(Object key) {
		return (this.objectMap.size()== 0)?null:this.objectMap.get(key) ;
	}
	public void putStatement(Object key, PreparedStatement value) {
		if(maxSize>0)this.objectMap.put(key, value);
	}
	public void clearAllStatement() {
		Iterator<Map.Entry<Object, PreparedStatement>> itor = this.objectMap.entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<Object, PreparedStatement> entry = (Map.Entry<Object, PreparedStatement>) itor.next();
			itor.remove();
			this.onRemove(entry.getKey(), entry.getValue());
		}
	}
	void onRemove(Object key, PreparedStatement obj) {
		try {
			((PreparedStatement) obj).close();
		} catch (Throwable e) {
		}
	}
}
