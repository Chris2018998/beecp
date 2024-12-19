<p align="left">
<a><img src="https://img.shields.io/circleci/build/github/Chris2018998/beecp"></a>
<a><img src="https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31"/></a>
<a><img src="https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3NP"/></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
<a><img src="https://img.shields.io/badge/Java-7+-green.svg"></a>
<a><img src="https://img.shields.io/github/license/Chris2018998/BeeCP"></a><br>
<a href="README_CN.md">中文</a>|<a href="README.md">English</a>
</p>

BeeCP是一款轻量级JDBC连接池，其Jar包仅133kB，技术亮点：单连接缓存，非移动等待，固定长度数组。

---
Java7+
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.4</version>
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

***PC**:Windows11,Intel-i7-14650HX,32G内存  **Java**:1.8.0_171  **Pool**:初始32,最大32  **Source code**:[HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)*

*_[HikariCP](https://github.com/brettwooldridge/HikariCP)是一款非常优秀的开源作品，在Java领域广泛使用，它由美国资深专家brettwooldridge开发_

***技术对比***

| 对比项  |     HikariCP                                   | BeeCP                         |
|--------|------------------------------------------------|-------------------------------|
|连接缓存 |多个                                            |单个                           |
|连接存储 |CopyOnWriteArrayList                           |固定长度数组                    |
|等待队列 |SynchronousQueue                               |ConcurrentLinkedQueue          |
|连接补充 |线程池                                          |单线程                         |
|同步创建 |不支持                                          |支持                           |
|清理重启 |不支持                                          |支持                           |
|提供中断方法|未提供                                        |提供                           |
|连接工厂扩展|未提供                                       |提供                           |
|可禁用ThreadLocal|不可                                   |可                             |
--- 
**如何使用**

在使用方式上与其他连接池产品大体相似。

_*如果您的项目是基于springboot框架构建，且有意愿应用beecp或已在使用它，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter),它可以帮您管理一个或多个beecp数据源_

--- 
**配置属性**

| 属性                            | 描述                                              | 默认值                                   |
|---------------------------------|--------------------------------------------------|---------------------------------------- |
| username                        | 连接数据库的用户名                                 | 空                                      |
| password                        | 连接数据库的密码                                   | 空                                      |
| jdbcUrl                         | 连接数据库的url                                    | 空                                      |
| driverClassName                 | 数据库的Jdbc驱动类名                                | 空                                       |
| poolName	                  | 连接池名                                           | 空                                       |
| fairMode                        | 是否使用公平模式                                    | false（竞争模式）                         | 
| initialSize                     | 连接池初始化时创建连接个数                           | 0                                        |
| maxActive                       | 池内最大连接数                                      | 10                                       | 
| borrowSemaphoreSize             | 池内信号量许可数                                    | min(最大连接数/2,CPU核心数）               |
| defaultAutoCommit               | AutoComit属性的默认值,未配置则从第一个连接上读取       | 空                                        |
| defaultTransactionIsolationCode | 事物隔离代码属性的默认值，未设置时则从第一个连接上读取  | 空                                        |
| defaultCatalog                  | Catalog属性的默认值 ,未配置则从第一个连接上读取        | 空                                        |
| defaultSchema                   | Schema属性的默认值,未配置则从第一个连接上读取          | 空                                        |
| defaultReadOnly                 | ReadOnly属性的默认值 ,未配置则从第一个连接上读取       | 空                                        |
| maxWait                         | 借用时最大等待时间(毫秒)                              | 8000                                     |
| idleTimeout                     | 连接闲置最大时间(毫秒)                                | 18000                                    |  
| holdTimeout                     | 已借连持但未使用的时间(毫秒)，超过强制回收              | 0                                        |  
| aliveTestSql                    | 连接存活检查sql                                      | SELECT 1                                 |  
| aliveTestTimeout                | 连接存活检测结果的等待最大时间(秒)                      | 3                                        |  
| aliveAssumeTime                 | 最近一次活动到借用时刻两者时间差值 (毫秒)，大于则存活检测 | 500                                      |  
| forceCloseUsingOnClear          | 清理时，是否强制回收使用中的连接                        | false                                    |
| parkTimeForRetry                | 非强制清理时，等待使用中的连接返回池中的时间(毫秒)        | 3000                                    |             
| timerCheckInterval              | 扫描闲置连接的隔时间(毫秒)                              | 18000                                   |
| forceDirtyOnSchemaAfterSet      | 是否强制设schema为脏属性（支持事务）                     | false                                   |
| forceDirtyOnCatalogAfterSet     | 是否强制设Catalog为脏属性（支持事务）                    | false                                   |
| enableThreadLocal               | ThreadLocal是否启用（false时可支持虚拟线程）             | true                                    | 
| enableJmx                       | JMX监控支持开关                                         | false                                   | 
| printConfigInfo                 | 是否打印配置信息                                        | false                                    | 
| printRuntimeLog                 | 是否打印运行时日志                                      | false                                    | 
| connectionFactory               | 连接工厂实例                                            | 空                                       |
| connectionFactoryClass          | 连接工厂类                                              | 空                                        |
| connectionFactoryClassName      | 连接工厂类名                                            | 空                                        |
| evictPredicate                  | 异常断言实例                                            | 空                                       |
| evictPredicateClass             | 异常断言类                                              | 空                                        |
| evictPredicateClassName         | 异常断言类名                                            | 空                                        |
| jdbcLinkInfoDecoder             | 连接信息解码器                                          | 空                                       |
| jdbcLinkInfoDecoderClass        | 连接信息解码器类                                        | 空                                        |
| jdbcLinkInfoDecoderClassName    | 连接信息解码器类名                                      | 空                                        |

*_以上属性可通过set方法进行设置；对象级属性的生效选择次序：实例 > 类 > 类名_

*_五个defaultxxx属性(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly)若无设置,则从第一个成功创建的连接读取_

--- 
**驱动参数**

数据库驱动内部一般是基于参数的方式进行工作，在使用的时候可根据具体情况进行调整；在BeeDataSourceConfig对象上提供两个方法（addConnectProperty(String,Object）,addConnectProperty(String））用于追加这方面的参数，在初始化的时候，连接池可将这些参数注入到连接工厂内部，使用参考如下

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
**属性文件**

beecp支持从properties文件中加载配置信息，如下参考

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

_如果connectProperties上信息比较多，也可参照下面方式_ 

```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true

```

---
**阻塞与中断**

由于网络或服务器等原因，连接池在创建连接的时候可能出现阻塞现象，从而影响到连接池的使用，针对这个问题，在beecp的数据源对象上（BeeDataSource）提供了两个方法

* ``` getPoolMonitorVo();//方法可查询到连接池的运行时的一些信息，如闲置数，使用数，创建数，创建超时数 ```

* ``` interruptConnectionCreating(boolean);//手工调用该方法用来中断执行中的创建，如果参数是true时则只中断超时的创建 ```


*_beecp的监控界面上也可查看到创建信息，并提供中断按钮_

*_maxWait属性值为超时时间，对于已经超时的创建，连接池内的定时扫描线程也会中断它们_


--- 
**清理与重启**

beecp在数据源上提供两个clear方法可让连接池恢复到初始状态，清理过程中不接受外部请求


* ```clear(boolean forceCloseUsing);//forceCloseUsing为true时，强制关闭使用中的连接 ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing为true时，强制关闭使用中的连接；清理后使用新配置初始化连接池 ```


*_清理时若存在创建，则进行中断处理_


--- 
**异常驱逐**

beecp提供SQL异常的连接驱逐功能，支持三种配置(BeeDataSourceConfig)
 
* 异常代码配置：``` addSqlExceptionCode(int code)；//增加代码 ```

* 异常状态配置：``` addSqlExceptionState(String state)；//增加状态 ```

* 异常断言配置：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);//设置异常断言对象或类 ```

_验证次序：a,若已配置断言，异常时则只执行断言验证,结果非空则驱逐 b,若无配置断言，异常代码（vendorCode）检查优先于异常状态（SQLState）检查，若存在于配置清单中，则驱逐_
 
_强制驱逐：调用连接上的abort方法(connecton.abort(null))即可_



--- 
**连接工厂**

beecp提供工厂接口（BeeConnectionFactory，BeeXaConnectionFactory）供自定义实现连接的创建，并且在配置BeeDataSourceConfig对象上有四个方法（setConnectionFactory，setXaConnectionFactory，setConnectionFactoryClass，setConnectionFactoryClassName）分别设置 _工厂对象，工厂类，工厂类名_，下面给一个参考例子

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

