package cn.bee.dbcp.test;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

@SuppressWarnings("serial")
public class SortKeyProperties extends Properties {
	private Vector keyVector = new Vector();
	public synchronized Enumeration keys() {
		return keyVector.elements();
	}
	public synchronized Object put(Object key, Object value) {
		Object oldValue = super.put(key,value);
		if(!keyVector.contains(key))
			keyVector.add(key);
		return oldValue;
	}
	public synchronized Object remove(Object key) {
		Object value = super.remove(key);
		if(keyVector.contains(key))
			keyVector.remove(key);
		return value;
	}
}
