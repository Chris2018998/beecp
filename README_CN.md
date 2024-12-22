[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP是一款轻量级JDBC连接池，Jar包仅133kB，其技术亮点：单连接缓存，非移动等待，固定长度数组。

---
Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.5</version>
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

------
**亮点feature**

* 连接池内阻塞与中断
* 连接池清理与配置重载
* 连接池配置从文件载入
* 提供扩展性接口
* 支持虚拟线程应用
* [提供Web监控页面](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

------
***性能对比***

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>


***对比HikariCP***

| 对比项               | HikariCP                | BeeCP                   |
|---------------------|-------------------------|-------------------------|
| 连接缓存             | 多个                    | 单个                    |
| 连接存储             | CopyOnWriteArrayList   | 固定长度数组              |
| 等待队列             | SynchronousQueue       | ConcurrentLinkedQueue   |
| 连接补充             | 线程池                  | 单线程                   |
| 并发创建             | 不支持                  | 支持                    |
| 清理重启             | 不支持                  | 支持                    |
| 提供中断方法         | 未提供                  | 提供                    |
| 连接工厂扩展         | 未提供                  | 提供                    |
| 可禁用ThreadLocal   | 不可                    | 可                     |

<sup>_[**HikariCP**](https://github.com/brettwooldridge/HikariCP)是一款非常优秀的开源作品，在Java领域广泛使用，它由美国资深专家brettwooldridge开发_<sup>

--- 
**如何使用**

在使用方式上与其他连接池产品大体相似，可参照随后一些代码片段

_温馨提示：如果您的项目是基于springboot框架构建，且有兴趣应用BeeCP或已在使用它，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter)帮您管理BeeCP数据源_

--- 
**属性参数**

BeeCP是基于参数驱动的，使用前可在其数据源配置对象（BeeDataSourceConfig）上设置一些参数值（如url，username之类的）

| 属性                             | 描述                                                                  | 默认值                    |
|----------------------------------|----------------------------------------------------------------------|--------------------------|
| username                         | 连接数据库的用户名                                                     |空                         |
| password                         | 连接数据库的密码                                                       |空                        |
| jdbcUrl                          | 连接数据库的url                                                        |空                        |
| driverClassName                  | 数据库的Jdbc驱动类名                                                    |空                       |
| poolName	                   | 连接池名                                                               |空                      |
| fairMode                         | 是否使用公平模式                                                        |false（非公平模式）        | 
| initialSize                      | 连接池初始化时创建连接的数量                                             |0                       |
| maxActive                        | 池内最大连接数                                                          |10                     | 
| borrowSemaphoreSize              | 池内信号量最大许可数(借用线程最大并发数）                                  |min(最大连接数/2,CPU核心数） |
| defaultAutoCommit                | Connection.setAutoComit(defaultAutoCommit)                          |空                   |
| defaultTransactionIsolationCode  | Connection.setTransactionIsolation(defaultTransactionIsolationCode) |空                   |
| defaultCatalog                   | Connection.setCatalog(defaultCatalog)                               |空                   |
| defaultSchema                    | Connection.setSchema(defaultSchema)                                 |空                   |
| defaultReadOnly                  | Connection.setReadOnly(defaultReadOnly)                             |空                   |
| maxWait                          | 借用连接时的最大等待时间(毫秒)                                         |8000                |
| idleTimeout                      | 未借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |18000               |  
| holdTimeout                      | 已借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |0                   |  
| aliveTestSql                     | 连接存活检查sql                                                      |SELECT 1            |  
| aliveTestTimeout                 | 连接存活检测结果的等待最大时间(秒)                                      |3                   |  
| aliveAssumeTime                  | 存活检测阈值时间差，小于则假定为活动连接，大于则检测                       |500                 |  
| forceCloseUsingOnClear           | 清理时，是否强制回收已借连接                                            |false               |
| parkTimeForRetry                 | 清理时，等待已借连接返回池中的时间(毫秒)                                 |3000                |             
| timerCheckInterval               | 池内定时线程工作隔时间(毫秒)                                            |18000               |
| forceDirtyOnSchemaAfterSet       | 是否在schema上设置脏标记，而忽略属性是否改变（PG时可设置）                 |false               |
| forceDirtyOnCatalogAfterSet      | 是否在catalog上设置脏标记，而忽略属性是否改变（PG时可设置）                |false               |
| enableThreadLocal                | ThreadLocal是否启用（false时可支持虚拟线程）                             |true                | 
| enableJmx                        | JMX监控支持开关                                                           |false            | 
| printConfigInfo                  | 是否打印配置信息                                                           |false               | 
| printRuntimeLog                  | 是否打印运行时日志                                                         |false               | 
| **connectionFactory**            | 连接工厂实例                                                              |空                   |
| **connectionFactoryClass**       | 连接工厂类                                                               |空                   |
| **connectionFactoryClassName**   | 连接工厂类名                                                              |空                   |
| **evictPredicate**               | 异常断言实例                                                              |空                   |
| **evictPredicateClass**          | 异常断言类                                                                |空                   |
| **evictPredicateClassName**      | 异常断言类名                                                              |空                   |
| **jdbcLinkInfoDecoder**          | 连接信息解码器                                                             |空                   |
| **jdbcLinkInfoDecoderClass**     | 连接信息解码器类                                                            |空                   |
| **jdbcLinkInfoDecoderClassName** | 连接信息解码器类名                                                           |空                   |

*_以上属性可通过set方法进行设置； **对象级**属性的生效选择次序：实例 > 类 > 类名_

*_五个defaultxxx属性(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly)
若未设置,则从第一个成功创建的连接上读取_

--- 
**属性文件**

BeeCP支持从属性文件(*.properities)中装载配置信息，例子代码如下

```java
String configFileName = "d:\beecp\config.properties";
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.loadFromPropertiesFile(configFileName);//将配置信息映射到到BeeDataSourceConfig属性上
```

config.properties

```properties

username=root
password=root
jdbcUrl=jdbc:mysql://localhost/test
driverClassName=com.mysql.cj.jdbc.Driver

initial-size=1
max-active=10

sqlExceptionCodeList=500150,2399,1105
sqlExceptionStateList=0A000,57P01,57P02,57P03,01002,JZ0C0,JZ0C1

connectProperties=cachePrepStmts=true&prepStmtCacheSize=50

evictPredicateClassName=org.stone.beecp.objects.MockEvictConnectionPredicate
connectionFactoryClassName=org.stone.beecp.objects.MockCommonConnectionFactory
jdbcLinkInfoDecoderClassName=org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder

```

_connectProperties的配置方式，也可参照下面方式_

```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true

```

---
**驱动参数**

在BeeCP内部，是使用JDBC驱动或连接工厂去创建连接对象（Connection)，它们通常也是基于参数模式工作的，所以在BeeCP数据源配置对象（BeeDataSourceConfig）上提供了两个方法用来添加它们的参数，在BeeCP初始化的时候，参数会被注入进驱动或工厂内部。

* ```addConnectProperty(String,Object);//添加单个参数 ```

* ```addConnectProperty(String);//添加多个参数，字符格式参考：cachePrepStmts=true&prepStmtCacheSize=250```

例子代码

```java
 BeeDataSourceConfig config = new BeeDataSourceConfig();
 config.addConnectProperty("cachePrepStmts", "true");
 config.addConnectProperty("prepStmtCacheSize", "250");
 config.addConnectProperty("prepStmtCacheSqlLimit", "2048");

 //或者
 config.addConnectProperty("cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048");

 //或者
 config.addConnectProperty("cachePrepStmts:true&prepStmtCacheSize:250&prepStmtCacheSqlLimit:2048");
```

---
**阻塞与中断**

由于网络或服务器或其它原因，客户端无法与数据库之间的建立连接，导致客户端的Connection创建者线程处于阻塞状态，从而影响到连接池的使用，BeeCP在数据源对象（BeeDataSource）上提供了两个方法

* ``` getPoolMonitorVo();//查询方法，结果对象中包括池中的连接闲置数，已借数，正在创建数，创建超时数等 ```

* ``` interruptConnectionCreating(boolean);//连接创建时若被阻塞，调用此方法可用于结束阻塞；若参数是true时则只中断超时的创建 ```


<sup>**补充说明**</sup></br>
1：创建超时的时间与maxWait一致，比如设置8秒的maxWait，那么驱动或连接工厂8秒内没有返回连接对象，则判定为连接创建超时</br>
2：创建超时发生后未被中断，BeeCP定时线程也会它们识别并中断</br>
3：BeeCP监控页面上也可查看创建信息和超时情况，并提供中断按钮

--- 
**清理与重启**

BeeCP在数据源（BeeDataSource）上提供了两个clear方法可清理池内已创建的连接，让池恢复到初始状态，清理过程中不接受外部请求

* ```clear(boolean forceCloseUsing);//forceCloseUsing为true时，强制回收已借连接 ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing为true时，强制回收已借连接；清理后使用新配置初始化连接池 ```

*_清理时过程中，若存在连接创建，则中断它们；若存在等待者，则让其退出等待，并以异常结束连接请求_


--- 
**异常驱逐**

连接在使用过程中可能会发生SQL异常（SQLException），有些则是普通异常，有些是比较严重问题，需要从池中移除（驱逐）它们的连接；如何识别需要驱逐的异常，BeeCP提供三种配置方式

* A. 异常代码配置：``` addSqlExceptionCode(int code)；//对应SQLException.vendorCode ```

* B. 异常状态配置：``` addSqlExceptionState(String state)；/对应SQLException.SQLState```

* C. 异常断言配置：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);//通过自定义的方式识别异常 ```
    
<sup>**补充说明**</sup></br>
1：若存在断言配置，则忽略其他两项配置，断言检查结果非空则驱逐异常连接（_灵活性驱逐，比如需要同时满足某个异常代码和某个异常状态_）</br>
2：若未配置断言，异常代码优先于异常状态检查，若SQLException.vendorCode或SQLException.SQLState存在于配置清单中，则驱逐_</br>
3：强制驱逐，调用连接上的abort方法(connecton.abort(null))即可
4：驱逐时若存在等待者则候补一个连接，并传递给等待者</br>

--- 
**连接工厂**

BeeCP提供工厂接口（BeeConnectionFactory，BeeXaConnectionFactory）供自定义实现连接的创建，并且在配置BeeDataSourceConfig对象上有四个方法（setConnectionFactory，setXaConnectionFactory，setConnectionFactoryClass，setConnectionFactoryClassName）分别设置
_工厂对象，工厂类，工厂类名_，下面给一个参考例子

```java
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import org.stone.beecp.BeeConnectionFactory;

public class MyConnectionFactory implements BeeConnectionFactory {
    private final String url;
    private final Driver driver;
    private final Properties connectInfo;

    public MyConnectionFactory(String url, Properties connectInfo, Driver driver) {
        this.url = url;
        this.driver= driver;
        this.connectInfo = connectInfo;
    }

    public Connection create() throws SQLException {
        return driver.connect(url, connectInfo);
    }
}


public class MyConnectionDemo {
    public static void main(String[] args) throws SQLException {
        final String url = "jdbc:mysql://localhost:3306/test";
        final Driver driver = DriverManager.getDriver(url);
        final Properties connectInfo = new Properties();
        connectInfo.put("user","root");
        connectInfo.put("password","root");

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MyConnectionFactory(url, connectInfo, driver));
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //put your code here
        }
    }
}

```

_温馨提示：若同时设置连接工厂和驱动类参数（driver,url,user,password)，那么连接工厂被优先使用。_

