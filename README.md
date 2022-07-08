<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|
<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)
<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
</p> 

## :coffee: Introduction 

BeeCP: a small JDBC connection pool 

## :arrow_down: Download 

Java7 or higher
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.7</version>
</dependency>
```

Java6
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.10</version>
</dependency>
```

## :book: Configuration item 

|**Item Name**                     |**Desc**                                |**Default**                      |
| ------------------------------|--------------------------------------- |----------------------------------- |
|username                       |jdbc username                           |null                                |
|password                       |jdbc password                           |null                                |
|jdbcUrl                        |jdbc url                                |null                                |
|driverClassName                |jdbc driver class name                  |null                                |
|poolName	                      |pool name;auto generated when not set   |null                                |
|fairMode                       |indicator,true-fair semaphore and fair transfer policy|false                 | 
|initialSize                    |size of connections on pool starting      |0                                 |
|maxActive                      |max reachable size of connections in pool |10                                | 
|borrowSemaphoreSize            |max permit size of pool semaphore         |min(maxActive/2,CPU core size）   |
|defaultAutoCommit              |'autoCommit' property default value       |null                              |
|defaultTransactionIsolationCode|'transactionIsolation'property default value,if not set,then read out from first connection|null|
|defaultCatalog                 |'catalog' property default value        |null                                 |
|defaultSchema                  |'schema' property default value         |null                                 |
|defaultReadOnly                |'readOnly' property default value       |null                                 |
|maxWait                        |milliseconds:max wait time to get one connection from pool|8000               |
|idleTimeout                    |milliseconds:max idle time of connections,when reach,then close them and remove from pool|18000|                             
|holdTimeout                    |milliseconds:max no-use time of borrowed connections,when reach,then return them to pool by forced close|18000|  
|validTestSql                   |connection valid test sql on borrowed              |SELECT 1                            |  
|validTestTimeout               |seconds:max time to get valid test result          |3                                   |  
|validAssumeTime                |milliseconds:connections valid assume time after last activity,if borrowed,not need test during the duration|500|  
|forceCloseUsingOnClear         |using connections forced close indicator on pool clear|false                            |
|delayTimeForNextClear          |milliseconds:delay time for next loop to clear,when<code>forceCloseUsingOnClear</code> is false and exists using connections|3000|   |timerCheckInterval             |milliseconds:interval time to run timer check task|18000                               |
|connectionFactoryClassName     |raw JDBC connection factory class name            |null                                |
|enableJmx                      |boolean indicator,true:register dataSource to jmx |false                               | 
|printConfigInfo                |boolean indicator,true:print config item info on pool starting|false                   | 
|printRuntimeLog                |boolean indicator,true:print runtime log                      |false                   | 
