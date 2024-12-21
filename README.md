[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP is a lightweight JDBC connection pool,its Jar file only 133KB and its techology highlights:caching single
connection,non moving waiting,fixed length array.

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

---
**Highlight Features**

* Connection pool clean and restart
* Connection pool blocking and interruption
* Support virtual thread applications
* Support loanding configuration from properties files
* Provide connection factory interface for customization
* Provide jdbc link info decoder interface for customization
* [Provide web monitor](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

**JMH Performance**

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

***PC**:Windows11,Intel-i7-14650HX,32GMemory **Java**:1.8.0_171  **Pool**:init-size:32,max-size:32  **Source code
**:[HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)*

***Compare to HikariCP***

| item                                           | HikariCP             | BeeCP                 |
|------------------------------------------------|----------------------|-----------------------|
| Number of connections in threadlocal           | multiple             | single                |
| Store container of connections                 | CopyOnWriteArrayList | a fixed length array  |
| Trasfer queue                                  | SynchronousQueue     | ConcurrentLinkedQueue |
| Connection addition way                        | Thread pool          | single thread         |
| Conneciton Concurrency creation                | Not Support          | Support               |
| Pool Clean and Restartup                       | Not Support          | Support               |
| Interruption method to end blocking            | Not Provide          | Provide               |
| Connection factory interface for customization | Not Support          | Not Support           |
| Disable ThreadLocal to support VT              | Not Support          | Support               |

--- 
**How to use**

At usage, it is generally similar to other connection pools;If your project is built on springboot framework and you are
interested at beecp or already using it,we recommend [beecp starter](https://github.com/Chris2018998/beecp-starter) to
you to manage beecp data source in your project.

--- 
**Configure properties**

The working mode of Beecp is parameter driven, and the working parameters can be set to BeeDataSourceConfig object. The
following is a list of configuration properties

| property name                   | description                                                            | default value             |
|---------------------------------|------------------------------------------------------------------------|---------------------------|
| username                        | jdbc user name                                                         | blank                     |
| password                        | jdbc password of user                                                  | blank                     |
| jdbcUrl                         | jdbc url link to db                                                    | blank                     |
| driverClassName                 | jdbc driver class name                                                 | blank                     |
| poolName	                       | pool name                                                              | blank                     |
| fairMode                        | an indictor of pool working                                            | false（compete mode）       | 
| initialSize                     | creation size of connecitons at pool initialize                        | 0                         |
| maxActive                       | max size of connections in pool semaphore                              | 10                        | 
| borrowSemaphoreSize             | max permit size of pool seg                                            | min(maxActive/2,CPU size） |
| defaultAutoCommit               | default value of autoComit propety                                     | blank                     |
| defaultTransactionIsolationCode | default value of transactionIsolation propety                          | blank                     |
| defaultCatalog                  | default value of catalog propety                                       | blank                     |
| defaultSchema                   | default value of schema propety                                        | blank                     |
| defaultReadOnly                 | default value of read-only propety                                     | blank                     |
| maxWait                         | max wait time in pool to get connection(ms)                            | 8000                      |
| idleTimeout                     | max idle time of connecitons in pool (ms)                              | 18000                     |  
| holdTimeout                     | max inactive time on brorowed connections(ms)                          | 0                         |  
| aliveTestSql                    | alive test sql                                                         | SELECT 1                  |  
| aliveTestTimeout                | max wait time to get alive check result(seconds)                       | 3                         |  
| aliveAssumeTime                 | a gap time between current point and last activation(ms)               | 500                       |  
| forceCloseUsingOnClear          | force reycle connecitions in using when pool cleaning                  | false                     |
| parkTimeForRetry                | park time to wait borrowed conneciton return to pool when cleaning(ms) | 3000                      |             
| timerCheckInterval              | a iterval time to scan idle-timeout conencitons (ms)                   | 18000                     |
| forceDirtyOnSchemaAfterSet      | force set dirty flag on schema when set（transaction support）           | false                     |
| forceDirtyOnCatalogAfterSet     | force set dirty flag on catlog when set（transaction support）           | false                     |
| enableThreadLocal               | enable indicator to use threadlocal（false to support VT）               | true                      | 
| enableJmx                       | enable indicator to support Jmx                                        | false                     | 
| printConfigInfo                 | indicator to print config items when pool initialize                   | false                     | 
| printRuntimeLog                 | indicator to print ruttime logs of pool                                | false                     | 
| connectionFactory               | connection factory instance                                            | blank                     |
| connectionFactoryClass          | connection factory class                                               | blank                     |
| connectionFactoryClassName      | connection factory class name                                          | blank                     |
| evictPredicate                  | predicate instance                                                     | blank                     |
| evictPredicateClass             | predicate class                                                        | blank                     |
| evictPredicateClassName         | predicate class name                                                   | blank                     |
| jdbcLinkInfoDecoder             | decoder instance of jdbc link info                                     | blank                     |
| jdbcLinkInfoDecoderClass        | decoder class of jdbc link info                                        | blank                     |
| jdbcLinkInfoDecoderClassName    | decoder class name of jdbc link info                                   | blank                     |

*_The above attributes can be filled by their set methods; The priority order of effective selection of object type
properties: instance>class>class name_

--- 
**File configuration**

Beecp supports loading configuration information from properties files,below is a reference example

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

_The configuration method of connectProperties can also refer to the following way_

```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true

```

--- 
**Driver parameters**

Database drivers generally work internally based on parameters and can be adjusted according to specific situations
during use;two methods are provided in BeeDataSourceConfig object (addConnectProperty (String, Object),
addConnectProperty (String)) to add,these parameters can be injected into the connection factory during pool
initialization, as shown in the following reference

```java
 BeeDataSourceConfig config = new BeeDataSourceConfig();
 config.addConnectProperty("cachePrepStmts", "true");
 config.addConnectProperty("prepStmtCacheSize", "250");
 config.addConnectProperty("prepStmtCacheSqlLimit", "2048");

 //or
 config.addConnectProperty("cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048");

 //or 
 config.addConnectProperty("cachePrepStmts:true&prepStmtCacheSize:250&prepStmtCacheSqlLimit:2048");
```

--- 
**Connection Eviction**

Beecp provides connection eviction feature and supports three kinds of configuration (BeeDataSourceConfig)

* error codes configuration：``` addSqlExceptionCode(int code)；//add an code ```

* error state configuration：``` addSqlExceptionState(String state)；//add state```

* predicate configuration：
  ``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);//set predicate,class,class name```

_eviction check order:_
_a, execute predicae to check sql exception if configured,evict connecitons if result is not null or not empty._
_b, if no predicae,error code check is priority;if matched in configured list,then evict them._

_force eviction：call abaort method of conneciton instance._


--- 
**Factory customization**

Beecp provides factory interfaces (BeeConnectFactory, BeeXaConnectFactory) for custom implementation of connection
creation, and there are four methods on the BeeDataSourceConfig object (setConnectFactory, setXaConnectFactory,
setConnectFactoryClass, setConnectFactoryClassName) to set the factory object, factory class, and factory class name
respectively. The order of effective selection is: factory object>factory class>factory class name,below is a reference
example

```java
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import org.stone.beecp.BeeConnectionFactory;

public class MyConnectionFactory implements BeeConnectionFactory {
    private final String url;
    private final Driver myDriver;
    private final Properties connectInfo;

    public MyConnectionFactory(String url, Properties connectInfo, Driver driver) {
        this.url = url;
        this.myDriver = driver;
        this.connectInfo = connectInfo;
    }

    public Connection create() throws SQLException {
        return myDriver.connect(url, connectInfo);
    }
}


public class MyConnectionDemo {
    public static void main(String[] args) throws SQLException {
        final String url = "jdbc:mysql://localhost:3306/test";
        final Driver myDriver = DriverManager.getDriver(url);
        final Properties connectInfo = new Properties();
        connectInfo.put("user","root");
        connectInfo.put("password","root");

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactory(new MyConnectionFactory(url, connectInfo, myDriver));
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //put your code here
        }
    }
}

```

_Reminder: If both the connection factory and four basic parameters (driver, URL, user, password) are set
simultaneously, the connection factory will be prioritized for use._



