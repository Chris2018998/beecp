/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.test.base;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConnectionSafeCloseTest extends TestCase {
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(Config.JDBC_URL);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        int threadSize=5;
        Connection con = ds.getConnection();
        long startTime=System.nanoTime()+ TimeUnit.SECONDS.toNanos(3);
        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        CloseThread[] testThreads=new  CloseThread[threadSize];
        for(int i=0;i<threadSize;i++){
            testThreads[i]=new CloseThread(startTime,con,countDownLatch);
            testThreads[i].start();
        }
        countDownLatch.await();
        int successSize=0,failedSize=0;
        for(int i=0;i<threadSize;i++){
            if(testThreads[i].closedInd) {
                successSize++;
            }else{
                failedSize++;
            }
        }

        if(!(successSize==1 && failedSize==4))throw new Exception("Connection Safe Close Failed");
    }

    //thread to close connection
    class CloseThread extends Thread {
        private long startTime;
        private Connection con;
        private CountDownLatch countDownLatch;
        boolean closedInd;

        public CloseThread(long startTime,Connection con,CountDownLatch countDownLatch) {
            this.con = con;
            this.startTime=startTime;
            this.countDownLatch = countDownLatch;
        }
        public void run() {
            try{
                LockSupport.parkNanos(startTime-System.nanoTime());
                con.close();
                closedInd=true;
            }catch(Exception e){
                closedInd=false;
            }
            countDownLatch.countDown();
        }
    }
}