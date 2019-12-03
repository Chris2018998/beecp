package cn.bee.dbcp.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class TestRunner {
	private static String defaultFilename="testCase.properties";
	@SuppressWarnings("rawtypes")
	private static Class[] getTestCaseClasses()throws Exception{
		return getTestCaseClasses(defaultFilename);
	}
	public void testRun()throws Throwable{
		long begtinTime =System.currentTimeMillis();
		TestRunner.run(getTestCaseClasses());
		System.out.println("Took time:("+(System.currentTimeMillis()-begtinTime) +")ms");
	}
	
	public static void main(String[] ags)throws Throwable{
		long begtinTime =System.currentTimeMillis();
		TestRunner.run(getTestCaseClasses());
		System.out.println("Took time:("+(System.currentTimeMillis()-begtinTime) +")ms");
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Class[] getTestCaseClasses(String caseFile)throws Exception{
		List classList = new ArrayList();
		InputStream propertiesStream = null;
		
		try {
			Properties properties = new SortKeyProperties();
			propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
			propertiesStream = TestRunner.class.getClassLoader().getResourceAsStream(defaultFilename);
			if (propertiesStream == null)propertiesStream = TestRunner.class.getResourceAsStream(defaultFilename);
			if(propertiesStream==null)throw new IOException("Can't find file:'testCase.properties' in classpath");
	
			properties.load(propertiesStream);
			Enumeration enumtion = properties.keys();
			while(enumtion.hasMoreElements()){
				Class clazz = Class.forName((String)enumtion.nextElement());
				classList.add(clazz);
			}
			
			return (Class[])classList.toArray(new Class[0]);

		} finally {
		  if(propertiesStream !=null)
				try {
					propertiesStream.close();
				} catch (IOException e) {
				}
		}
	}
	
	 
	@SuppressWarnings("rawtypes")
	public static void run(Class testClass)throws Throwable{
		if (testClass != null) {
			((TestCase)testClass.newInstance()).run();
		}
	}
	 
	@SuppressWarnings("rawtypes")
	public static void run(Class[] testClass)throws Throwable{
		if(testClass!=null){
			for(int i=0;i<testClass.length;i++)
			 run(testClass[i]);
		}
	}
}

@SuppressWarnings("serial")
class SortKeyProperties extends Properties {
	private Vector<Object> keyVector = new Vector<Object>();
	public synchronized Enumeration<Object> keys() {
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
