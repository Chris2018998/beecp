# 🌿 BeeCP

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

一款JDBC连接池，具有代码少，依赖少，性能高，覆盖率高等特点；技术优点：单连接缓存，固定长度数组，非移动等待，异步加法等.


## 🌼 特色功能
 
* 支持阻塞中断操作
* 支持重启和配置重载
* 提供接口支持扩展
* 支持虚拟线程应用
* 内置监控功能

## 🍁 坐标

Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>5.1.3</version>
</dependency>
```

Java6(deprecated)

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.10</version>
</dependency>
```

## 📊 性能对比

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>


## 🍒 差异对比

| 对比项               | HikariCP               | BeeCP                   |
|---------------------|-------------------------|-------------------------|
| 连接缓存             | 多个                    | 单个                    |
| 连接存储             | CopyOnWriteArrayList   | 固定长度数组              |
| 等待队列             | SynchronousQueue       | ConcurrentLinkedQueue   |
| 连接补充             | 线程池                  | 单线程                   |
| 并行创建             | 不支持                  | 支持                    |
| 重启与重载           | 不支持                  | 支持                    |
| 提供中断             | 未提供                  | 提供                    |
| 扩展接口             | 1                      | 6                       |
| 可禁用ThreadLocal   | 不可                    | 可                       |
| 支持XAConnection    | 不支持                  | 支持                     |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP)是一款非常优秀的开源作品，它由美国资深专家brettwooldridge开发_


## ⏰ 数据库Down机测试

Brettwooldridge曾在HikariCP项目的WiKi上发布过一篇文章：[《Bad Behavior: Handling Database Down》](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down) ，该文主要讲述了一次数据库Down机模拟测试，
用于验证4个知名连接池在该场景下反应情况，测试结果：只有HikariCP在5秒做出反应。那么我们同样对BeeCP也做一次这样的测试。

|     环境或参数项          | 环境或参数值                                                   |  备注                                                                                           |
|--------------------------|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------- |
| database                 | mysql-8.4.3                                                    |                                                                                                      |
| driver                   | mysql-connector-j-8.3.0.jar                                    |                                                                                                      |
| url                      | jdbc:mysql://hostIP/test?connectTimeout=50&socketTimeout=100   |connectTimeout，socketTimeout是mySQL JDBC驱动参数                                                      |
| timeout                  | **5000** 毫秒                                                   |HikariConfig.setConnectionTimeout(5000); BeeDataSourceConfig.setMaxWait(5000);                       |
| Pool version             | HikariCP-6.2.1, stone-1.4.6                                    |                                                                                                      |
| Java version             | Java-22.0.2                                                    |                                                                                                      |

测试原图如下

![image](https://github.com/user-attachments/assets/4cca47e0-04d2-4792-a070-1bf9f1bd0306)

**18000毫秒重测**
|     环境或参数项          | 环境或参数值                                                   |  备注                                                                                            |
|--------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------- |
| timeout                  | **18000** 毫秒                                                 |HikariConfig.setConnectionTimeout(18000); BeeDataSourceConfig.setMaxWait(18000);                     |
| 其他配置                  | 无变化                                                         |                                                                                                     |
 
![image](https://github.com/user-attachments/assets/4e0d70b4-e68a-4b28-b1c8-bfb0a949e401)

*^-^ 如果设置一个更大的时间，会怎么样?*

**连接池评级**

| Pool	        |等级    | 评级描述                                     |
|--------------|--------|---------------------------------------------|
| HikariCP     | A      |由超时参数决定                                |
| BeeCP        | A+     |Socket级反应                                 |

<br/>

## ✈️ 连接关闭性测试

我们在使用JDBC时，通常会接触到三类对象：Connection、PreparedStatement、ResultSet，它们之间通常存在依存级关系，即关闭一个Connection时，那么由该它打开的PreparedStatement也应该自动关闭，为了避免资源泄露，连接池实现时通常是需要考虑这点，
但是在使用MySQL的时候，我们发现一个例外情况，即在cachePrepStmts=true & useServerPrepStmts=true时，在Connection已关闭的情况下PreparedStatement依然可继续使用，[查看测试源代码](../beecp/test/src/main/java/org/stone/beecp/other/MysqlClosedPreparedStatementTest.java).

![image](https://github.com/user-attachments/assets/f75d5684-ff4f-4ad9-b88e-f453e833ea69)

_*测试结论：BeeCP在Connection关闭后，PreparedStatement是不可用的_

_*感谢JetBrains公司提供的社区版IDEA工具_

## 🔌 扩展接口

BeeCP预留一些接口可供外部扩展

| 接口类                                                 | 作用                  |      备注                                                                         
|-------------------------------------------------------|-----------------------|--------------------------|
|   org.stone.beecp.BeeConnectionFactory                |创建Connetion          | |
|   org.stone.beecp.BeeXaConnectionFactory              |创建XAConnetion        | |
|   org.stone.beecp.BeeConnectionPredicate              |驱逐测试               | |                 
|   org.stone.beecp.BeeJdbcLinkInfoDecoder              |连接信息解码器          | |                      
|   org.stone.beecp.BeeMethodExecutionListener          |方法执行监听器          | |                   
|   org.stone.beecp.BeeMethodExecutionListenerFactory   |方法执行监听器工厂      |用于支持监听器的灵活性创建，比如工厂读取外部参数等 |   

## ⚙️ 运行时设置

| 方法                                                                    | 作用                  |      备注                                                                         
|------------------------------------------------------------------------|-----------------------|--------------------------|
|   BeeDataSource.enableLogPrint(boolean)                                |连接池工作日志输出开关   | |
|   BeeDataSource.enableMethodExecutionLogCache(boolean)                 |方法执行日志缓存开关     | |
|   BeeDataSource.setMethodExecutionListener(BeeMethodExecutionListener) |方法执行监听器设置      |日志缓存开关打开时才有效|                 
|   BeeDataSource.setUsername(String)                                    |数据库连接用户名        | |                      
|   BeeDataSource.setPassword(String)                                    |数据库连接用户密码      | |                   
|   BeeDataSource.setJdbcUrl(String)                                     |数据库连接Url          |  |  
|   BeeDataSource.setUrl(String)                                         |数据库连接Url          |  |   

##  🔚 连接驱逐

在用连接的时候，可能会因为某些意外情况或严重性错误时，需要彻底物理性关闭连接，BeeCP连接池提供了两类驱逐性功能

1. 手工驱逐，调用连接上的abort方法（connecton.abort(null)），连接池立即对它们进行物理关闭，并从池中移除

2. 配置驱逐，用于帮助连接池识别需要驱逐发生SQL异常的连接，三种配置

* A. 异常代码配置：``` BeeDataSourceConfig.addSqlExceptionCode(int code)；//对应SQLException.vendorCode ```

* B. 异常状态配置：``` BeeDataSourceConfig.addSqlExceptionState(String state)；/对应SQLException.SQLState ```

* C. 异常断言配置：``` BeeDataSourceConfig.setPredicate(BeeConnectionPredicate p);BeeDataSourceConfig.setPredicateClass(Clas c); BeeDataSourceConfig.setPredicateClassName(String n); ```

## 🏭 工厂属性

BeeCP可通过以下方法设置连接工厂属性，在连接池初始化的时候，这些属性将会被注入到连接工厂中

| 方法                                                                          | 作用                  |      备注                                                                         
|-------------------------------------------------------------------------------|-----------------------|--------------------------|
|  BeeDataSourceConfig.addConnectionFactoryProperty(String name,Object value)   |增加属性值              ||
|  BeeDataSourceConfig.addConnectionFactoryProperty(String name);               |增加字符性属性值，支持=和:作为分割符号|p1=v1&p2=v2&p3=v3 或 p1:v1&p2:v2&p3:v3|

_*以上两个方法也适用于动态增加驱动扩展性参数_


## 📜 文件配置
 BeeCP支持从属性文件（*.properities）或属性对象（java.util.properities）中读取参数信息到配置对象上，参考例子如下

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.loadFromPropertiesFile("D:\beecp\config.properties");
```

config.properties

```properties
username=root
password=root
jdbcUrl=jdbc:mysql://localhost/test
driverClassName=com.mysql.cj.jdbc.Driver

initial-size=1
max-active=10

#连接工厂实现类名
connectionFactoryClassName=x1
#jdbc link信息的解码器实现类名
linkInfoDecoderClassName=x2
#测试性断言实现类名
predicateClassName=x3
#方法执行监听器类名
methodExecutionListenerClassName=x4
#方法执行监听器工厂类名
methodExecutionListenerFactoryClassName=x5

#驱逐测试异常代码
sqlExceptionCodeList=500150,2399
#驱逐测试异常状态
sqlExceptionStateList=0A000,57P01

#工厂属性配置1(参数不多时推荐使用)
connectionFactoryProperties=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true&prepStmtCacheSize=50

#工厂属性配置2(参数个数比较多时)
connectionFactoryProperties.size=2
connectionFactoryProperties.1=prepStmtCacheSize=50
connectionFactoryProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true

```
*_温馨提示：属性名配置支持：驼峰，中划线，下划线_


## 💻 监控UI

![image](https://github.com/user-attachments/assets/e0684ff2-8a7e-4a20-ab68-69c7b2f30bfa)<br/>

![image](https://github.com/user-attachments/assets/b59dbac9-a3b3-4173-9ff5-845783691e0d)

_*温馨提示：如果您的项目是基于springboot框架构建，且有意向使用BeeCP连接池，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter)_


## 🛠️ 属性列表

| 属性                             | 描述                                                                 | 默认值                    |
|----------------------------------|----------------------------------------------------------------------|--------------------------|
| username                         | 连接数据库的用户名                                                     |空                         |
| password                         | 连接数据库的密码                                                       |空                         |
| jdbcUrl                          | 连接数据库的url                                                        |空                        |
| driverClassName                  | 连接数据库的Jdbc驱动类名                                                |空                        |
| poolName	                        | 连接池名，若未设置，则自动产生                                           |空                        |
| fairMode                         | 连接池是否使用公平模式                                                  |false（非公平模式）         | 
| initialSize                      | 池初始化的连接数                                                       |0                         |
| maxActive                        | 池内最大允许连接数                                                     |10                        | 
| semaphoreSize                    | 池内信号量最大许可数                                                   |min(最大连接数/2,CPU核心数） |
| defaultAutoCommit                | autoCommit默认值                                                     |空                          |
| defaultTransactionIsolation      | transactionIsolation默认值                                           |空                          |
| defaultCatalog                   | catalog默认值                                                        |空                          |
| defaultSchema                    | schema默认值                                                        |空                          |
| defaultReadOnly                  | readOnly默认值                                                      |空                          |
| maxWait                          | 借用连接时的最大等待时间(毫秒)                                         |8000                |
| idleTimeout                      | 未借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |18000               |  
| holdTimeout                      | 已借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |0                   |  
| aliveTestSql                     | 连接活性检查sql                                                      |SELECT 1            |  
| aliveTestTimeout                 | 连接存活检测结果的等待最大时间(秒)                                      |3                   |  
| aliveAssumeTime                  | 存活检测阈值时间差，小于则假定为活动连接，大于则检测                       |500                 |  
| forceRecycleBorrowedOnClose      | 清理时，是否强制回收已借连接                                            |false               |
| parkTimeForRetry                 | 清理时，等待已借连接返回池中的时间(毫秒)                                 |3000                |             
| intervalOfClearTimeout           | 池内定时线程工作隔时间(毫秒)                                            |18000               |
| forceDirtyWhenSetSchema          | schema属性是否强制重置标记(PG可设置）                                   |false               |
| forceDirtyWhenSetCatalog         | catalog属性是否强制重置标记(PG可设置）                                  |false               |
| useThreadLocal                   | ThreadLocal是否启用（false时可支持虚拟线程）                             |true                | 
| registerMbeans                   | JMX监控支持开关                                                           |false            | 
| printConfiguration               | 是否打印配置信息                                                           |false               | 
| printRuntimeLogs                 | 是否打印运行时日志                                                         |false               | 
| **connectionFactory**            | 连接工厂实例                                                              |空                   |
| **connectionFactoryClass**       | 连接工厂类                                                               |空                   |
| **connectionFactoryClassName**   | 连接工厂类名                                                              |空                   |
| **predicate**                    | 异常断言实例                                                              |空                   |
| **predicateClass**               | 异常断言类                                                                |空                   |
| **predicateClassName**           | 异常断言类名                                                              |空                   |
| **linkInfoDecoder**              | 连接信息解码器                                                             |空                   |
| **linkInfoDecoderClass**         | 连接信息解码器类                                                            |空                   |
| **linkInfoDecoderClassName**     | 连接信息解码器类名                                                           |空                   |
| enableMethodExecutionLogCache    | 方法执行日志缓存开关,默认不打开                                               |false                 |
| methodExecutionLogCacheSize      | 方法执行日志缓存大小                                                         |1000                   |
| methodExecutionLogTimeout        | 方法执行日志缓存超时时间(毫秒)                                                |180000             |
| intervalOfClearTimeoutExecutionLogs | 方法执行日志缓存清理间隔时间(毫秒)                                         |180000               |
| slowConnectionThreshold             | 慢连接的阈值(毫秒)                                                        |30000                 |
| slowSQLThreshold                    | 慢SQL的阈值(毫秒)                                                        |30000                 |
| **methodExecutionListener**         | 方法执行监听器                                                            | 空                      |
| **methodExecutionListenerClass**    | 方法执行监听器类                                                           | 空                      |
| **methodExecutionListenerClassName** | 方法执行监听器类名                                                         | 空                      |
| **methodExecutionListenerFactory**          |方法执行监听器工厂                                                   | 空                      |
| **methodExecutionListenerFactoryClass**     |方法执行监听器工厂类                                                  | 空                      |
| **methodExecutionListenerFactoryClassName** | 方法执行监听器工厂类名                                               | 空                      |

*_**对象级属性**，设置的是类或类名时，须存在无参构造器，生效选择次序：实例 > 类 > 类名_  

## 🐝 关于BeeCP

BeeCP发布支持两条路线

* 独立版: 用于支持低版本Java，如Java7,Java8等，当前高版本Java也可使用
  
* 组合版: 组合连接池，对象池，任务池，适用Java17起步的高版本，使用时VM参数需要引入: --add-exports java.base/jdk.internal.misc=ALL-UNNAMED

## 👦 关于作者

Chris2018998，一名中国Java技术爱好者



