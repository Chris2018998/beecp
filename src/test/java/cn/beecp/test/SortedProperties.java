/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
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
    private final Vector<Object> keyVector = new Vector<Object>(10);

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
        keyVector.remove(key);
        return value;
    }
}

