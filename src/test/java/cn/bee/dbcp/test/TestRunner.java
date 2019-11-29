package cn.bee.dbcp.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class TestRunner {
	private static String defaultFilename="testCase.properties";
	private static Class[] getTestCaseClasses()throws Exception{
		return getTestCaseClasses(defaultFilename);
	}
	private static Class[] getTestCaseClasses(String caseFile)throws Exception{
		List classList = new ArrayList();
		InputStream propertiesStream = null;
		
		try {
			Properties properties = new SortKeyProperties();
			propertiesStream = TestRunner.class.getResourceAsStream(caseFile);
			if(propertiesStream==null)
				throw new FileNotFoundException(caseFile);
			
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
	
	/**
	 * 运行某个类的测试
	 */
	public static void run(Class testClass)throws Throwable{
		if (testClass != null) {
			((TestCase)testClass.newInstance()).run();
		}
	}
	
	/**
	 * 运行一批测试类
	 */
	public static void run(Class[] testClass)throws Throwable{
		if(testClass!=null){
			for(int i=0;i<testClass.length;i++)
			 run(testClass[i]);
		}
	}
	 
	/**
	 * 测试入口
	 */
	public static void main(String[] ags)throws Throwable{
		long begtinTime =System.currentTimeMillis();
		TestRunner.run(getTestCaseClasses());
		System.out.println("Took time:("+(System.currentTimeMillis()-begtinTime) +")ms");
	}
}
