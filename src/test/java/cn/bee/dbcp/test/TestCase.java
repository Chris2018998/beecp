package cn.bee.dbcp.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test case base 
 * 
 * @author chris.liao
 */
public class TestCase {
	public void setUp()throws Throwable{}
	public void tearDown()throws Throwable{}

	/**
	 * Run 
	 */
	void run() throws Throwable {
		try {
			setUp();
			runTest();
		} finally {
			try {
				tearDown();
			} catch (Throwable e) {
			}
		}
	}
	
	/**
	 * Test
	 */
	private void runTest()throws Throwable{
		int successCount =0,failedCount=0;
		long beginTime = System.currentTimeMillis();
		Method[] methods = this.getClass().getDeclaredMethods();
		for(int i=0;i<methods.length;i++){
			if(methods[i].getName().startsWith("test")&& methods[i].getParameterTypes().length==0){
				methods[i].setAccessible(true);
				try {
					methods[i].invoke(this,new Object[0]);
					successCount++;
				}catch(Throwable e){
					failedCount++;
					System.out.println("Failed to run test method:"+methods[i].getName() + " in Class["+ this.getClass().getName()+"]");
					if(e instanceof InvocationTargetException){
						((InvocationTargetException)e).getTargetException().printStackTrace();
					}else{
						e.printStackTrace();
					}
				}
			}
		}
		
		long endTime = System.currentTimeMillis();
		System.out.println("Case[" +  this.getClass().getName() + "]took time:"	+ (endTime - beginTime)+ "ms,sucessed(" + successCount+ "),failed(" + failedCount + ")");
	}
}
