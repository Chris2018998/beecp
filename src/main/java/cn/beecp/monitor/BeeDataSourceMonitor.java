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
package cn.beecp.boot.monitor;

import cn.beecp.boot.monitor.sqltrace.SqlTracePool;
import cn.beecp.pool.ConnectionPoolMonitorVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;

/**
 * Restful Controller
 *
 * @author Chris.Liao
 */

@RestController
@RequestMapping("/dsMonitor")
public class BeeDataSourceMonitor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private List<ConnectionPoolMonitorVo> poolInfoList = new LinkedList<ConnectionPoolMonitorVo>();

    @RequestMapping("/getPoolList")
    public List<ConnectionPoolMonitorVo> getJson() {
        return getPoolInfoList();
    }

    @RequestMapping("/getSqlExecTraceList")
    public Object getSQLExecutionListJson() {
        return SqlTracePool.getInstance().getTraceQueue();
    }

    private List<ConnectionPoolMonitorVo> getPoolInfoList() {
        poolInfoList.clear();
        BeeDataSourceCollector collector = BeeDataSourceCollector.getInstance();
        BeeDataSourceWrapper[] dsArray = collector.getAllDataSource();
        for (BeeDataSourceWrapper ds : dsArray) {
            try {
                ConnectionPoolMonitorVo vo = ds.getMonitorVo();
                if (vo.getPoolState() == 3) {//POOL_CLOSED
                    collector.removeDataSource(ds);
                } else {
                    poolInfoList.add(vo);
                }
            } catch (Exception e) {
                log.info("Failed to get dataSource monitor info", e);
            }
        }
        return poolInfoList;
    }

}
