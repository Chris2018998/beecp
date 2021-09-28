<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
<img height="20px" width="20px" align="bottom" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>

<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 <a><img src="https://img.shields.io/github/v/release/Chris2018998/beecp.svg"></a> 
</p> 

## 简介

BeeCP是一款高性能JDBC连接池

## 由来

BeeCP源自Jmin项目（Java工具套件集，04年创建）的子模块改造而来

## 下载 

Java7或更高
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.2.7</version>
</dependency>
```
Java6
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.9</version>
</dependency>
```

## 使用

使用方式与一般池大致相似，下面有两个参考例子

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">例子1</a>

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">例子2</a>

如果项目为Springboot类型，推荐使用 <a href="https://github.com/Chris2018998/BeeCP-Starter"> BeeCP-Starter</a>


## 优点

1：ThreadLocal连接单缓存，提高池化性能

2：借用者非移动等待，节约队列出入开销

3：传递管道复用，既可以传递连接，也可传递异常

4：双向异步候补，消除等待者与传送者的时间差错位


## 比较HikariCP池

|     比较项    |     BeeCP                                                   |      HikariCP                                             |  
| -----------  |----------------------------------------------------------   | ----------------------------------------------------------|          
|关键技术       |ThreadLocal + 信号量 + ConcurrentLinkedQueue +Thread           |FastList + ConcurrentBag + ThreadPoolExecutor             | 
|相似点         |CAS使用,代理预生成,使用驱动的Statement缓存                           |-                                                         |
|文件           |32个源码文件,Jar包93KB                                         |44个源码文件,Jar包158KB                                    | 
|性能           |总体性能高40%以上（光连接池基准）                                 |                                                         |

HikariCP有哪些缺陷？

1：<a href="https://my.oschina.net/u/3918073/blog/4645061">MySQL应用下,已经关闭的PreparedStatement居然可以复活？</a> 

2：<a href="https://my.oschina.net/u/3918073/blog/5053082">数据库Down机或网络问题，反应迟缓(俗称等你一万年)</a>

3：<a href="https://my.oschina.net/u/3918073/blog/5171229">事务性漏洞问题</a>

.....


## 配置项列表

|  配置项          |   描述                        |   备注                            |
| ----------------| ---------------------------  | ------------------------          |
| username        | JDBC用户名                    |                                   |
| password        | JDBC密码                      |                                   |
| jdbcUrl         | JDBC连接URL                   |                                   |
| driverClassName | JDBC驱动类名                   |                                   |
| poolName	      |池名                            |如果未赋值则会自动产生一个            |
| fairMode        | 连接池是否公平模式               | 默认false,竞争模式                 | 
| initialSize     | 连接池初始大小                  |                                   |
| maxActive       | 连接池最大个数                  |                                   | 
| borrowSemaphoreSize  | 信号量请求并发数（借用者线程数）| 不允许大于连接最大数               |
| defaultAutoCommit|连接是否为自动提交              | 默认true                            |
| defaultTransactionIsolationCode|事物等级          | 默认不未设置                         |
| defaultCatalog    |                             |                                     |
| defaultSchema     |                             |                                     |
| defaultReadOnly   |                             | 默认false                            |
| maxWait           |连接借用等待最大时间(毫秒)       | 默认8秒，连接请求最大等待时间           |
| idleTimeout       |连接闲置最大时间(毫秒)          | 默认3分钟，超时会被清理                 |  
| holdTimeout       |连接被持有不用的最大时间(毫秒)    | 默认5分钟，超时会被清理                 |  
| connectionTestSql |连接有效性测试SQL语句           | 一条 select 语句，不建议放入存储过程     |  
| connectionTestTimeout |连接有效性测试超时时间(秒)   |默认5秒 执行查询测试语句时间，在指定时间范围内等待反应|  
| connectionTestInterval |连接测试的间隔时间(毫秒)     |默认500毫秒 连接上次活动时间点与当前时间时间差值小于它，则假定连接是有效的|  
| forceCloseUsingOnClear|是否直接关闭使用中转连接       |默认false;true:直接关闭使用中连接，false:等待处于使用中归还后再关闭|
| delayTimeForNextClear  |延迟清理的时候时间（毫秒）    |默认3000毫秒,还存在使用中的连接，延迟等待时间再清理|                   
| idleCheckTimeInterval  |闲置扫描线程间隔时间(毫秒)     |   默认5分钟                                 |
| connectionFactoryClassName|自定义的JDBC连接工作类名            | 默认为空                  |
| enableJmx                 |JMX监控支持开关                    | 默认false                | 


## 捐助

如果您觉得此作品不错，可以捐赠请我们喝杯咖啡吧，在此表示感谢^_^。

<img height="50%" width="50%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/donate.png"> 
