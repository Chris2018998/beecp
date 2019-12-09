package cn.beecp.test.performance;

import java.math.BigDecimal;

public class TestAvg implements Comparable<TestAvg> {
	private String poolName;
	private BigDecimal avgValue;

	public TestAvg(String poolName, BigDecimal avgValue) {
		this.poolName = poolName;
		this.avgValue = avgValue;
	}

	public String getPoolName() {
		return poolName;
	}

	public BigDecimal getAvgValue() {
		return avgValue;
	}

	public int compareTo(TestAvg o) {
		return avgValue.compareTo(o.avgValue);
	}
	
	
	
	
}
