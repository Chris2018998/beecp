[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP是一款轻量级JDBC连接池，Jar包仅133KB，其技术亮点：单连接缓存，非移动等待，固定长度数组

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

* 提供中断处理
* 支持清理与重启
* 支持配置文件载入
* 提供扩展性接口
* 支持虚拟线程应用
* [提供Web监控页面](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

_温馨提示：如果您的项目是基于springboot框架构建，且有兴趣应用BeeCP或已在使用它，那么推荐[beecp-starter](https://github.com/Chris2018998/beecp-starter)(个人的另一个项目)_

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
| 提供中断             | 未提供                  | 提供                    |
| 连接工厂扩展         | 未提供                  | 提供                    |
| 可禁用ThreadLocal   | 不可                    | 可                     |
| 支持XAConnection    | 不支持                  | 支持                     |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP)是一款非常优秀的开源作品，在Java领域广泛使用，它由美国资深专家brettwooldridge开发_

--- 
**如何使用**

在使用方式上与主流连接池产品大体相似，也可参照随后一些代码片段


--- 
**参数配置**

BeeCP使用的参数信息来自其配置对象（BeeDataSourceConfig），下面列表为主要的参数属性名
| 属性                              | 描述                                                                  | 默认值                    |
|----------------------------------|----------------------------------------------------------------------|--------------------------|
| username                         | 连接数据库的用户名                                                     |空                         |
| password                         | 连接数据库的密码                                                       |空                         |
| jdbcUrl                          | 连接数据库的url                                                        |空                        |
| driverClassName                  | 数据库的Jdbc驱动类名                                                    |空                        |
| poolName	                   | 连接池名                                                               |空                        |
| fairMode                         | 是否使用公平模式                                                        |false（非公平模式）         | 
| initialSize                      | 连接池初始化时创建连接的数量                                             |0                         |
| maxActive                        | 池内最大连接数                                                         |10                        | 
| borrowSemaphoreSize              | 池内信号量最大许可数(借用线程最大并发数）                                 |min(最大连接数/2,CPU核心数） |
| defaultAutoCommit                | Connection.setAutoComit(defaultAutoCommit)                          |空                          |
| defaultTransactionIsolationCode  | Connection.setTransactionIsolation(defaultTransactionIsolationCode) |空                          |
| defaultCatalog                   | Connection.setCatalog(defaultCatalog)                               |空                          |
| defaultSchema                    | Connection.setSchema(defaultSchema)                                 |空                          |
| defaultReadOnly                  | Connection.setReadOnly(defaultReadOnly)                             |空                          |
| maxWait                          | 借用连接时的最大等待时间(毫秒)                                         |8000                |
| idleTimeout                      | 未借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |18000               |  
| holdTimeout                      | 已借连接闲置超时时间(毫秒)，不可大于数据库最大闲置时间                    |0                   |  
| aliveTestSql                     | 连接存活检查sql                                                      |SELECT 1            |  
| aliveTestTimeout                 | 连接存活检测结果的等待最大时间(秒)                                      |3                   |  
| aliveAssumeTime                  | 存活检测阈值时间差，小于则假定为活动连接，大于则检测                       |500                 |  
| forceCloseUsingOnClear           | 清理时，是否强制回收已借连接                                            |false               |
| parkTimeForRetry                 | 清理时，等待已借连接返回池中的时间(毫秒)                                 |3000                |             
| timerCheckInterval               | 池内定时线程工作隔时间(毫秒)                                            |18000               |
| forceDirtyOnSchemaAfterSet       | 连接归还时，Schema属性是否强制重置标记(PG可设置）                         |false               |
| forceDirtyOnCatalogAfterSet      | 连接归还时，Catalog属性是否强制重置标记(PG可设置）                        |false               |
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

*_**对象级属性**，生效选择次序：实例 > 类 > 类名_

*_**对象级属性**，若设置的是类或类名时，须非抽象且存在无参构造函数_

*_**五个defaultxxx属性**(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly)的默认值若未设置，则从第一个成功创建的连接上读取_

--- 
**文件配置**

BeeCP支持从属性文件（*.properities）或属性对象（java.util.properities）中读取参数信息到配置对象上，参考例子如下

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.loadFromPropertiesFile("d:\beecp\config.properties");
```

config.properties

```properties
username=root
password=root
jdbcUrl=jdbc:mysql://localhost/test
driverClassName=com.mysql.cj.jdbc.Driver

initial-size=1
max-active=10

#连接工厂实现的类名
connectionFactoryClassName=org.stone.beecp.objects.MockCommonConnectionFactory
#jdbc link信息的解码器实现的类名
jdbcLinkInfoDecoderClassName=org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder

```
_温馨提示：属性名配置方式目前支持：驼峰，中划线，下划线_

---
**驱动参数**

BeeCP内部是使用驱动或连接工厂创建连接对象，它们可能依赖一些参数，在配置对象(BeeDataSourceConfig)提供了两个方法

* ```addConnectProperty(String,Object);//添加单个参数 ```

* ```addConnectProperty(String);//以字符串的方式添加参数，可一次配置多个，如：cachePrepStmts=true&prepStmtCacheSize=250```

<br/>

_参考代码_

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

* _文件配置1_
```properties

connectProperties=cachePrepStmts=true&prepStmtCacheSize=50

```

* _文件配置2(多项参数时推荐)_
```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true
```

--- 
**连接驱逐**

BeeCP提供了两种方式

1. 手工驱逐，调用连接上的abort方法（connecton.abort(null)），连接池立即对它们进行物理关闭，并从池中移除

2. 配置驱逐，用于帮助连接池识别需要驱逐发生SQL异常的连接，三种配置

* A. 异常代码配置：``` addSqlExceptionCode(int code)；//对应SQLException.vendorCode ```

* B. 异常状态配置：``` addSqlExceptionState(String state)；/对应SQLException.SQLState```

* C. 异常断言配置：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n); ```
 
<br/>

_文件配置_
```properties

sqlExceptionCodeList=500150,2399,1105
sqlExceptionStateList=0A000,57P01,57P02,57P03,01002,JZ0C0,JZ0C1

//或者
evictPredicateClassName=org.stone.beecp.objects.MockEvictConnectionPredicate

```

_补充说明_

* 1：断言驱逐用于自定义性实现，当其验证结果非空（Not Null and Not Empty）则驱逐连接
* 2：断言配置的使用优先于代码配置和状态配置，若存在断言配置，自动忽略其他两项配置
* 3：异常代码检查优先于异常状态检查
* 4：驱逐后，若池种存在等待者，自动候补一个新连接

---
**中断处理**

连接创建是连接池内一项目重要活动，但是由于服务器或网络或其他原因，可能导致创建过程处于阻塞状态，为解决这一问题，BeeCP提供了两种方式

1. 外部方式，在数据源对象（BeeDataSource）提供两个方法：查询方法：getPoolMonitorVo()；中断方法：interruptConnectionCreating(boolean)；

2. 内部方式，内部工作线程定时识别阻塞，并中断它们<br/>

<br/>

_补充说明_

* 1：创建时间超过maxwait的值时，连接池则判断定为创建阻塞
* 2：中断的是借用者线程，getConnection上会抛出中断异常；若是候补线程，它会尝试将异常传递给等待者
* 3: BeeCP监控页面上也可查看到相关信息，如创建数，创建超时数，如超时则显示出中断按钮

--- 
**清理与重启**

BeeCP支持重置操作，让连接池恢复到初始状态，清理过程中不接受外部请求，它主要完成两个事项

* A: 清除池内所有的连接和等待者
* B: 重新初始化连接池（也可是使用新配置）

<br/>

_主要有两个方法_

* ```BeeDataSource.clear(boolean forceCloseUsing);//使用原配置重新初始化 ```

* ```BeeDataSource.clear(boolean forceCloseUsing, BeeDataSourceConfig newConfig);//使用新配置重新初始化```


--- 
**连接工厂接口**

在BeeCP内部定义了连接工厂接口，并内置两种基本实现（对驱动和数据源的封装），工厂接口是允许外部自定义实现，有4个相关配置方法（etConnectionFactory，setXaConnectionFactory，setConnectionFactoryClass，setConnectionFactoryClassName）分别设置工厂实例，工厂类，工厂类名，下面是一个参考例子

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

