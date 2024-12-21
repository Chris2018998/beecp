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

* 连接池清理与重启
* 连接池内阻塞与中断
* 支持虚拟线程应用
* 支持属性文件配置
* 支持连接工厂自定义
* 支持连接信息解密自定义
* [提供Web监控页面](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

------
***性能对比***

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

***PC**:Windows11,Intel-i7-14650HX,32G内存  **Java**:1.8.0_171  **Pool**:初始32,最大32  **Source code
**:[HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)*

***对比HikariCP***

| 对比项            | HikariCP             | BeeCP                 |
|----------------|----------------------|-----------------------|
| 连接缓存           | 多个                   | 单个                    |
| 连接存储           | CopyOnWriteArrayList | 固定长度数组                |
| 等待队列           | SynchronousQueue     | ConcurrentLinkedQueue |
| 连接补充           | 线程池                  | 单线程                   |
| 并发创建           | 不支持                  | 支持                    |
| 清理重启           | 不支持                  | 支持                    |
| 提供中断方法         | 未提供                  | 提供                    |
| 连接工厂扩展         | 未提供                  | 提供                    |
| 可禁用ThreadLocal | 不可                   | 可                     |

*_[HikariCP](https://github.com/brettwooldridge/HikariCP)
是一款非常优秀的开源作品，在Java领域广泛使用，它由美国资深专家brettwooldridge开发_

--- 
**如何使用**

在使用方式上与其他连接池产品大体相似。

_
*如果您的项目是基于springboot框架构建，且有兴趣应用beecp或已在使用它，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter)
，它可以帮您管理一个或多个beecp数据源_

--- 
**工作参数**

beecp的工作模式是基于参数驱动，可通过其配置对象（BeeDataSourceConfig）的方法设置工作参数，下面为参数列表

| 属性                               | 描述                                                                  | 默认值                 |
|----------------------------------|---------------------------------------------------------------------|---------------------|
| username                         | 连接数据库的用户名                                                           | 空                   |
| password                         | 连接数据库的密码                                                            | 空                   |
| jdbcUrl                          | 连接数据库的url                                                           | 空                   |
| driverClassName                  | 数据库的Jdbc驱动类名                                                        | 空                   |
| poolName	                        | 连接池名                                                                | 空                   |
| fairMode                         | 是否使用公平模式                                                            | false（竞争模式）         | 
| initialSize                      | 连接池初始化时创建连接的数量                                                      | 0                   |
| maxActive                        | 池内最大连接数                                                             | 10                  | 
| borrowSemaphoreSize              | 池内信号量最大许可数(借用线程最大并发数）                                               | min(最大连接数/2,CPU核心数） |
| defaultAutoCommit                | Connection.setAutoComit(defaultAutoCommit)                          | 空                   |
| defaultTransactionIsolationCode  | Connection.setTransactionIsolation(defaultTransactionIsolationCode) | 空                   |
| defaultCatalog                   | Connection.setCatalog(defaultCatalog)                               | 空                   |
| defaultSchema                    | Connection.setSchema(defaultSchema)                                 | 空                   |
| defaultReadOnly                  | Connection.setReadOnly(defaultReadOnly)                             | 空                   |
| maxWait                          | 借用连接时的最大等待时间(毫秒)                                                    | 8000                |
| idleTimeout                      | 未借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                                        | 18000               |  
| holdTimeout                      | 已借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                                        | 0                   |  
| aliveTestSql                     | 连接存活检查sql                                                           | SELECT 1            |  
| aliveTestTimeout                 | 连接存活检测结果的等待最大时间(秒)                                                  | 3                   |  
| aliveAssumeTime                  | 存活检测阈值时间差，小于则假定为活动连接，大于则检测                                          | 500                 |  
| forceCloseUsingOnClear           | 清理时，是否强制回收已借连接                                                      | false               |
| parkTimeForRetry                 | 清理时，等待已借连接返回池中的时间(毫秒)                                               | 3000                |             
| timerCheckInterval               | 池内定时线程工作隔时间(毫秒)                                                     | 18000               |
| forceDirtyOnSchemaAfterSet       | 是否在schema上设置脏标记，而忽略属性是否改变（PG时可设置）                                   | false               |
| forceDirtyOnCatalogAfterSet      | 是否在catalog上设置脏标记，而忽略属性是否改变（PG时可设置）                                  | false               |
| enableThreadLocal                | ThreadLocal是否启用（false时可支持虚拟线程）                                      | true                | 
| enableJmx                        | JMX监控支持开关                                                           | false               | 
| printConfigInfo                  | 是否打印配置信息                                                            | false               | 
| printRuntimeLog                  | 是否打印运行时日志                                                           | false               | 
| **connectionFactory**            | 连接工厂实例                                                              | 空                   |
| **connectionFactoryClass**       | 连接工厂类                                                               | 空                   |
| **connectionFactoryClassName**   | 连接工厂类名                                                              | 空                   |
| **evictPredicate**               | 异常断言实例                                                              | 空                   |
| **evictPredicateClass**          | 异常断言类                                                               | 空                   |
| **evictPredicateClassName**      | 异常断言类名                                                              | 空                   |
| **jdbcLinkInfoDecoder**          | 连接信息解码器                                                             | 空                   |
| **jdbcLinkInfoDecoderClass**     | 连接信息解码器类                                                            | 空                   |
| **jdbcLinkInfoDecoderClassName** | 连接信息解码器类名                                                           | 空                   |

*_以上属性可通过set方法进行设置； **对象级**属性的生效选择次序：实例 > 类 > 类名_

*_五个defaultxxx属性(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly)
若无设置,则从第一个成功创建的连接上读取_

--- 
**参数文件**

beecp工作所需要的参数来自其配置对象（BeeDataSourceConfig）,它支持从Properties文件中一次性载入多个配置项，参考代码

```java
String configFileName = "d:\beecp\config.properties";
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.loadFromPropertiesFile(configFileName);
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

连接池使用JDBC驱动或连接工厂去创建连接对象，在驱动或连接工厂内部可能需要一些工作参数，beecp配置对象（BeeDataSourceConfig）提供了两个方法用来支持它们参数的设置。

* ```addConnectProperty(String,Object);//设置单个参数 ```

* ```addConnectProperty(String);//设置多个参数```

参考代码

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

由于网络或服务器等其它原因，客户端无法建立与数据库之间的正常连接，导致创建者线程处于阻塞状态，从而影响到连接池的使用，beecp在数据源对象（BeeDataSource）上提供了两个方法

* ``` getPoolMonitorVo();//运行时状态查询，返回结果对象中包括连接的闲置数，已借数，正在创建数，创建已超时数等 ```

* ``` interruptConnectionCreating(boolean);//中断连接创建过程，如果参数是true时则只中断已超时的创建 ```

*_一次创建耗时超过maxWait值，则视为创建超时，beecp池内定时扫描线程会自动终止它_

*_beecp监控界面上也可查看到运行时信息，若创建超时则出现中断按钮_

--- 
**清理与重启**

beecp在数据源（BeeDataSource）上提供了两个clear方法可清理池内已创建的连接，让池恢复到初始状态，清理过程中不接受外部请求

* ```clear(boolean forceCloseUsing);//forceCloseUsing为true时，强制回收已借连接 ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing为true时，强制回收已借连接；清理后使用新配置初始化连接池 ```

*_清理时过程中，若存在连接创建，则中断它们；若存在等待者，则让其退出等待，并以异常结束连接请求_


--- 
**异常驱逐**

beecp支持SQL级异常连接的驱逐，提供三种驱逐参数配置（BeeDataSourceConfig）

* 异常代码配置：``` addSqlExceptionCode(int code)；//增加代码 ```

* 异常状态配置：``` addSqlExceptionState(String state)；//增加状态 ```

* 异常断言配置：
  ``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);//设置异常断言对象或类 ```

_生效规则
a：若存在断言配置，则忽略其他两项配置，断言检查结果非空则驱逐异常连接
b：若无配置断言，异常代码（vendorCode）优先于异常状态（SQLState）检查，若code或state存在于配置清单中，则驱逐_

_强制驱逐：调用连接上的abort方法(connecton.abort(null))即可_



--- 
**连接工厂**

beecp提供工厂接口（BeeConnectionFactory，BeeXaConnectionFactory）供自定义实现连接的创建，并且在配置BeeDataSourceConfig对象上有四个方法（setConnectionFactory，setXaConnectionFactory，setConnectionFactoryClass，setConnectionFactoryClassName）分别设置
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

