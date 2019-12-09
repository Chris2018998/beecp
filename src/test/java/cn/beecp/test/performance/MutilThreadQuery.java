package cn.beecp.test.performance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.beecp.BeeDataSource;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;
import cn.beecp.test.performance.type.BeeCP_C;
import cn.beecp.test.performance.type.BeeCP_F;

/**
 * Performance of multiple thread execute SQL query
 * 
 * @author Chris
 */
public class MutilThreadQuery extends TestCase {
	static final int scale = 4;
	static String testName = "Multiple thread query";
	static Logger log = LoggerFactory.getLogger(MutilThreadQuery.class);

	public void test() throws Exception {
		String sql = "select * from "+Config.TEST_TABLE;
		int threadCount = 100;
		int executeCount = 100;
		String poolName = "Bee_F,Bee_C";
		String[] pools = poolName.split(",");

		List<TestAvg> arvgList = new ArrayList<TestAvg>();
		List<List<Object>> allPoolResultList = new ArrayList<List<Object>>();
		for (int i = 0; i < pools.length; i++) {
			String testPoolName = pools[i];
			testPoolName = testPoolName.trim();
			List<Object> poolResultList = null;

			if (testPoolName.equalsIgnoreCase("Bee_C")) {
				poolResultList = beeCP_Compete(sql, threadCount, executeCount);
			} else if (testPoolName.equalsIgnoreCase("Bee_F")) {
				poolResultList = beeCP_Fair(sql, threadCount, executeCount);
			} else {
				log.info("unkown pool type : " + testPoolName);
			}
			if(i==0)log.info("\n");
			
			if (poolResultList != null) {
				allPoolResultList.add(poolResultList);
				arvgList.add(new TestAvg(testPoolName, (BigDecimal) poolResultList.get(2)));
			}
			if (pools.length > 1)
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
		}

		if (allPoolResultList.size() > 1)
			TestResultPrint.printAnalysis(poolName, testName, arvgList, allPoolResultList);
	}

	private List<Object> beeCP_Compete(String sql, int threadCount, int executeCount) throws Exception {
		BeeDataSource dataource = BeeCP_C.createDataSource();
		try {
			return runPool(sql, threadCount, executeCount, dataource, "Compete");
		} finally {
			dataource.close();
		}
	}

	private List<Object> beeCP_Fair(String sql, int threadCount, int executeCount) throws Exception {
		BeeDataSource dataource = BeeCP_F.createDataSource();
		try {
			return runPool(sql, threadCount, executeCount, dataource, "Fair");
		} finally {
			dataource.close();
		}
	}

	private List<Object> runPool(String sql, int threadCount, int loopCount, DataSource dataSource, String sourceName)
			throws Exception {

		log.info("Pool[" + sourceName + " -- " + testName + "] -- Begin{" + threadCount + "threads X " + loopCount
				+ "iterate}");
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 5);
		long concurrentTime = calendar.getTimeInMillis();

		CountDownLatch latch = new CountDownLatch(threadCount);
		TestQueryThread[] threads = new TestQueryThread[threadCount];
		for (int i = 0; i < threadCount; i++) {
			threads[i] = new TestQueryThread(dataSource, sql, loopCount, latch, concurrentTime);
			threads[i].start();
		}

		latch.await();// wait all thread done
		List<Object> summaryList = TestResultPrint.printSummary(sourceName, testName, threads, threadCount, loopCount,
				scale);
		return summaryList;
	}
}
