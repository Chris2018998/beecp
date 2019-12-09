package cn.beecp.test.performance;

public interface TestResult {

	public int getFailedCount();

	public int getSuccessCount();
	
	public long[] getStartTime() ;
	
	public long[] getEndTime();
	 
}
