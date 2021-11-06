/*
 * Copyright Chris2018998
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
package cn.beecp.test;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Sorted by key order
 *
 * @author chris.liao
 */
class SortedProperties extends Properties {
    private Vector<Object> keyVector = new Vector<Object>(10);

    public synchronized Enumeration<Object> keys() {
        return keyVector.elements();
    }

    public synchronized Object put(Object key, Object value) {
        Object oldValue = super.put(key, value);
        if (!keyVector.contains(key))
            keyVector.add(key);
        return oldValue;
    }

    public synchronized Object remove(Object key) {
        Object value = super.remove(key);
        if (keyVector.contains(key))
            keyVector.remove(key);
        return value;
    }
}

