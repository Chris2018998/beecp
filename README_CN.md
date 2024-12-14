<p align="left">
<a><img src="https://img.shields.io/circleci/build/github/Chris2018998/beecp"></a>
<a>
<img src="https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31"/></a>
<a><img src="https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3NP"/></a>
<a><img src="https://img.shields.io/badge/Java-7+-green.svg"></a>
<a><img src="https://img.shields.io/github/license/Chris2018998/BeeCP"></a>
</p>

BeeCP，一款轻量级JDBC连接池，Jar包仅133kB，技术亮点：单点缓存，非移动等待，固定长度数组。

## 
Java7+
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.4</version>
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
## 
**特色功能**

* 连接池清理与重启

* 连接池阻塞中断
  
* 支持连接工厂扩展

* 支持虚拟线程

* [运行时监控Monitor](https://github.com/Chris2018998/beecp-starter)
![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

## 
**JMH性能**

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

_Windows11，Intel (R) Core (TM) i7-14650HX,32G内存，Java-1.8.0_171，连接池配置：初始 32，最大 32_

_测试源码：[HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)_


## 
**配置项**

| 项名                              | 描述                            | 默认值                 |
|---------------------------------|-------------------------------|---------------------|
| username                        | JDBC用户名                       | 空                   |
| password                        | JDBC密码                        | 空                   |
| jdbcUrl                         | JDBC连接URL                     | 空                   |
| driverClassName                 | JDBC驱动类名                      | 空                   |
| poolName	                       | 池名，如果未赋值则会自动产生一个              | 空                   |
| fairMode                        | 是否使用公平模式                      | false（竞争模式）         | 
| initialSize                     | 连接池初始大小                       | 0                   |
| maxActive                       | 连接池最大个数                       | 10                  | 
| borrowSemaphoreSize             | 信号量许可大小                       | min(最大连接数/2,CPU核心数） |
| defaultAutoCommit               | AutoComit默认值,未配置则从第一个连接上读取默认值 | 空                   |
| defaultTransactionIsolationCode | 事物隔离代码，未设置时则从第一个连接上读取默认值      | 空                   |
| defaultCatalog                  | Catalog默认值 ,未配置则从第一个连接上读取默认值  | 空                   |
| defaultSchema                   | Schema默认值,未配置则从第一个连接上读取默认值    | 空                   |
| defaultReadOnly                 | ReadOnly默认值 ,未配置则从第一个连接上读取默认值 | 空                   |
| maxWait                         | 连接借用等待最大时间(毫秒)                | 8000                |
| idleTimeout                     | 连接闲置最大时间(毫秒)                  | 18000               |  
| holdTimeout                     | 连接被持有不用最大允许时间(毫秒)             | 18000               |  
| aliveTestSql                    | 连接有效性测试SQL语句                  | SELECT 1            |  
| aliveTestTimeout                | 连接有效性测试超时时间(秒)                | 3                   |  
| aliveAssumeTime                 | 连接测试的间隔时间(毫秒)                 | 500                 |  
| forceCloseUsingOnClear          | 是否直接关闭使用中连接                   | false               |
| delayTimeForNextClear           | 延迟清理的时候时间（毫秒）                 | 3000                |                   
| timerCheckInterval              | 闲置扫描线程间隔时间(毫秒)                | 18000               |
| connectionFactoryClassName      | 自定义的JDBC连接工作类名                | 空                   |
| enableJmx                       | JMX监控支持开关                     | false               | 
| printConfigInfo                 | 是否打印配置信息                      | false               | 
| printRuntimeLog                 | 是否打印运行时日志                     | false               | 
 
