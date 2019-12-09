package cn.beecp.test.performance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestResultPrint {
	static Logger log = LoggerFactory.getLogger(TestResultPrint.class);
	public static List<Object> printSummary(String datasource, String testName, TestResult[] threads, int threadCount,
			int loopCount, int scale) {

		BigDecimal totalCount = new BigDecimal(threadCount * loopCount);
		long minTime = -1, maxTime = -1;
		BigDecimal gapTime = new BigDecimal(0);
		BigDecimal totalSuccessTime = new BigDecimal(0);
		BigDecimal failCount = new BigDecimal(0);
		BigDecimal successCount = new BigDecimal(0);

		int mill0 = 0, mill10 = 0, mill50 = 0, mill100 = 0;
		int mill200 = 0, mill500 = 0, mill1000 = 0,mill2000 = 0,mill8000 = 0,mill8000M=0;
		
		for (int i = 0; i < threadCount; i++) {
			failCount = failCount.add(new BigDecimal(threads[i].getFailedCount()));
			successCount = successCount.add(new BigDecimal(threads[i].getSuccessCount()));
			
			long[] startTime = threads[i].getStartTime();
			long[] endTime = threads[i].getEndTime();
			
			for (int j = 0; j < loopCount; j++) {
				if (endTime[j] == 0)continue;

				gapTime = new BigDecimal(endTime[j]).subtract(new BigDecimal(startTime[j]));
				totalSuccessTime = totalSuccessTime.add(gapTime);
				
				gapTime = new BigDecimal(TimeUnit.NANOSECONDS.toMillis(gapTime.longValue()));//nano to millis
				long tempTime = gapTime.longValue();
				
				if (tempTime == 0) {
					mill0++;
				} else if (1 <= tempTime && tempTime <= 10) {
					mill10++;
				} else if (10 < tempTime && tempTime <= 50) {
					mill50++;
				} else if (50 < tempTime && tempTime <= 100) {
					mill100++;	
				} else if (100 < tempTime && tempTime <= 200) {
					mill200++;
				} else if (200 < tempTime && tempTime <= 500) {
					mill500++;
				} else if (500 < tempTime && tempTime <= 1000) {
					mill1000++;
				} else if (1000 < tempTime && tempTime <= 2000) {
					mill2000++;
				} else if (2000 < tempTime && tempTime <= 8000) {
					mill8000++;
				} else {
					mill8000M++;
				}

				if (minTime == -1)
					minTime = tempTime;
				if (maxTime == -1)
					maxTime = tempTime;
				
				if (minTime > tempTime)
					minTime = tempTime;
				if (maxTime < tempTime)
					maxTime = tempTime;
			}
		}
		totalSuccessTime =new BigDecimal(TimeUnit.NANOSECONDS.toMillis(totalSuccessTime.longValue()));
		BigDecimal minTimeDecimal = new BigDecimal(minTime);
		BigDecimal maxTimeDecimal = new BigDecimal(maxTime);
		List<Object> resultList = new ArrayList<Object>();
		BigDecimal avgTime=new BigDecimal("999999999999999999");
		if(successCount.intValue()>0)
		 avgTime= totalSuccessTime.divide(successCount,scale,BigDecimal.ROUND_HALF_UP);
		
		resultList.add(successCount.longValue());// 0
		resultList.add(failCount.longValue());// 1
		resultList.add(avgTime);// 2
		resultList.add(minTime);// 3
		resultList.add(maxTime);// 4
		resultList.add(mill0);//5
		resultList.add(mill10);//6
		resultList.add(mill50);//7
		resultList.add(mill100);//8
		resultList.add(mill200);//9
		resultList.add(mill500);//10
		resultList.add(mill1000);//11  
		resultList.add(mill2000);//12
		resultList.add(mill8000);//13
		resultList.add(mill8000M);//14
	
		log.info("Pool["+datasource+" -- "+testName+"] -- Execution count:"+ totalCount.toPlainString() + ",total time:"
				+ totalSuccessTime.toPlainString() + "ms,avg time:"
				+ (avgTime.toPlainString())+"ms"
				+ ",min time:" + minTimeDecimal.toPlainString() + "ms,max time:" + maxTimeDecimal.toPlainString()+"ms");

		log.info("success count       :" + successCount);//0
		log.info("fail count          :" + failCount);//1
		log.info("avr(ms)             :"+ (avgTime.toPlainString()));//2
		log.info("min(ms)             :" + minTime);//3
		log.info("max(ms)             :" + maxTime);//4
		log.info("time==0ms           :" + mill0);//5
		log.info("1ms=<time<=10ms     :" + mill10);//6
		log.info("10ms<time<=50ms     :" + mill50);//7
		log.info("50ms<time<=100ms    :" + mill100);//8
		log.info("100ms<time<=200ms   :" + mill200);//9
		log.info("200ms<time<=500ms   :" + mill500);//10
		log.info("500ms<time<=1000ms  :" + mill1000);//11
		log.info("1000ms<time<=2000ms :" + mill2000);//12
		log.info("2000ms<time<=8000ms :" + mill8000);//13
		log.info("time>8000ms         :" + mill8000M);//14
		return resultList;
	}
	
	public static void printAnalysis(String allPoolNames,String testName,List<TestAvg> arvgList,List<List<Object>> allPoolResultList) {
		StringBuffer buf = new StringBuffer();
		buf.append("["+testName+"]Performance analysis:");
		Collections.sort(arvgList);
		for (int i = 0; i < arvgList.size(); i++) {
			if (i > 0)
				buf.append(" > ");
			TestAvg avg = arvgList.get(i);
			buf.append(avg.getPoolName() + "(" + avg.getAvgValue().toPlainString() + ")");
		}
		log.info(buf.toString());
		
//		StringBuffer headBuffer = new StringBuffer();
//		StringBuffer headBuffer2 = new StringBuffer();
//		headBuffer.append("|Sumary|").append(allPoolNames.replaceAll(",", "|")).append("|");
//		String head = headBuffer.toString();
//		headBuffer2.append("|");
//		int index =1;
//		while((index=head.indexOf("|",index+1))!=-1 ){
//			headBuffer2.append(" --- ").append("|");
//		}
//		
//		List<StringBuffer> tabelList= new ArrayList<StringBuffer>(19);
//		for(int i=0;i<15;i++){
//			tabelList.add(new StringBuffer());
//		}
//		tabelList.get(0).append("|success Count");
//		tabelList.get(1).append("|fail count");
//		tabelList.get(2).append("|avr(ms)");
//		tabelList.get(3).append("|min(ms)");
//		tabelList.get(4).append("|max(ms)");
//		tabelList.get(5).append("|time=0ms");
//		tabelList.get(6).append("|1ms=<time<=10ms");
//		tabelList.get(7).append("|10ms<time<=50ms");
//		tabelList.get(8).append("|50ms<time<=100ms");
//		tabelList.get(9).append("|100ms<time<=200ms");
//		tabelList.get(10).append("|200ms<time<=500ms");
//		tabelList.get(11).append("|500ms<time<=1000ms");
//		tabelList.get(12).append("|1000ms<time<=2000ms");
//		tabelList.get(13).append("|2000ms<time<=800ms");
//		tabelList.get(14).append("|8000ms<time");
//	
//		int pos=0;
//		for(int j=0;j<15;j++){
//		    StringBuffer tempBuf = tabelList.get(j);
//		    tempBuf.append("|");
//		    for(int n=0;n<allPoolResultList.size();n++){
//		    	List<Object> resultList = allPoolResultList.get(n);
//		    	tempBuf.append(resultList.get(pos)).append("|");
//		    }  
//		    pos++;
//		    if(j==14)tempBuf.append("|");
//		}
//		
//		log.info(headBuffer.toString());
//		log.info(headBuffer2.toString());
//		for(int j=0;j<15;j++){
//			log.info(tabelList.get(j).toString());
//		}
		log.info("\n");
	}
}
