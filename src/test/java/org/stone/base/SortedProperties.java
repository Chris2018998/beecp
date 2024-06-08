/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.base;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Sorted by key order
 *
 * @author chris liao
 */
class SortedProperties extends Properties {
    private final Vector<Object> keyVector = new Vector<>(10);

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

