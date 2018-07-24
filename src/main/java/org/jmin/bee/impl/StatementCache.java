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
package org.jmin.bee.impl;

import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * preparedStatement cache
 *
 * @author Chris liao
 * @version 1.0
 */

public final class StatementCache extends LinkedHashMap<Object, PreparedStatement> {
	private int maxSize;

	public StatementCache(int maxSize) {
		super((int) Math.ceil(maxSize / 0.75f) + 1, 0.75f, true);
		this.maxSize = maxSize;
	}

	protected boolean removeEldestEntry(Map.Entry<Object, PreparedStatement> eldest) {
		if (size() > maxSize) {
			this.onRemove(eldest.getKey(), eldest.getValue());
			return true;
		} else {
			return false;
		}
	}

	public int maxSize() {
		return this.maxSize;
	}

	public PreparedStatement remove(Object key) {
		PreparedStatement ps = super.remove(key);
		if(ps!=null)onRemove(key, ps);
		return ps;
	}

	public void remove(Object key, PreparedStatement ps) {
		super.remove(key);
		onRemove(key, ps);
	}

	public void clear() {
		Iterator<Map.Entry<Object, PreparedStatement>> itor = super.entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<Object, PreparedStatement> entry = itor.next();
			this.onRemove(entry.getKey(), entry.getValue());
		}
		super.clear();
	}

	final void onRemove(Object key, PreparedStatement obj) {
		if (obj != null) {
			try {
				obj.close();
			} catch (Throwable e) {
			}
		}
	}
}
