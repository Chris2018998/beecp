[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP is a lightweight JDBC connection pool,its Jar file only 133KB and its techology highlights:caching single connection,non moving waiting,fixed length array.

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

* Connection pool blocking and interruption
* Connection pool clean and reinitialization
* Support loanding configuration from properties files
* Provide interfaces for customization
* Support virtual thread applications
* [Provide web monitor](https://github.com/Chris2018998/beecp-starter)

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)
![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

**JMH Performance**

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>

***Compare to HikariCP***

| item                                           | HikariCP             | BeeCP                 |
|------------------------------------------------|----------------------|-----------------------|
| Number of connections in threadlocal           | multiple             | single                |
| Store structre of pool connections             | CopyOnWriteArrayList | a fixed length array  |
| Transfer queue between waiters and releasers   | SynchronousQueue     | ConcurrentLinkedQueue |
| Connection addition way                        | Thread pool          | single thread         |
| Conneciton Concurrency creation                | Not Support          | Support               |
| Pool Clean and Restartup                       | Not Support          | Support               |
| Interruption method to end blocking            | Not Provide          | Provide               |
| Connection factory interface for customization | Not Support          | Not Support           |
| Disable ThreadLocal to support VT              | Not Support          | Support               |

<sup>_[**HikariCP**](https://github.com/brettwooldridge/HikariCP) is a very excellent open source work widely used in the Java world, it developed by Brettwooldridge, a senior JDBC expert of United States_<sup>

--- 
**How to use it**

At usage,it is generally similar to other connection pools.

_Reminder: If your project is built on springboot framework and you are interested at beecp or already using it,we recommend [beecp starter](https://github.com/Chris2018998/beecp-starter) to
you to manage beecp data source in your project._

--- 
**Configuration properties**

BeeCP is driven by parameters,before startup,its parameters can be set to data source configuration object (BeeDataSourceConfig).

| property name                   | description                                                            | default value             |
|---------------------------------|------------------------------------------------------------------------|---------------------------|
| username                        | user name of db                                                        | blank                     |
| password                        | user password of db                                                    | blank                     |
| jdbcUrl                         | link url to db                                                         | blank                     |
| driverClassName                 | jdbc driver class name                                                 | blank                     |
| poolName	                  | pool name                                                              | blank                     |
| fairMode                        | an indictor to use fair mode for connection getting                    | false（unfair）            | 
| initialSize                     | creation size of connecitons at pool initialize                        | 0                         |
| maxActive                       | max size of connections in pool                                        | 10                        | 
| borrowSemaphoreSize             | max permit size of semaphore for conneciton getting                    | min(maxActive/2,CPU size） |
| defaultAutoCommit               | Connection.setAutoComit(defaultAutoCommit)                             | blank                     |
| defaultTransactionIsolationCode | Connection.setTransactionIsolation(defaultTransactionIsolationCode)    | blank                     |
| defaultCatalog                  | Connection.setCatalog(defaultCatalog)                                  | blank                     |
| defaultSchema                   | Connection.setSchema(defaultSchema)                                    | blank                     |
| defaultReadOnly                 | Connection.setReadOnly(defaultReadOnly)                                | blank                     |
| maxWait                         | max wait time in pool to get connection(ms)                            | 8000                      |
| idleTimeout                     | max idle time of connecitons in pool (ms)                              | 18000                     |  
| holdTimeout                     | max inactive time of borrowed connections(ms)                          | 0                         |  
| aliveTestSql                    | alive test sql                                                         | SELECT 1                  |  
| aliveTestTimeout                | max wait time to get alive check result(seconds)                       | 3                         |  
| aliveAssumeTime                 | a hreshold time to do alive check on borrowed connections,assume alive if less,otherwise check(ms)| 500                       |  
| forceCloseUsingOnClear          | indicator to recyle borrowed connecton by force when pool clean       | false                     |
| parkTimeForRetry                | timed wait for borrowed connections to return to pool and close them(ms) | 3000                      |             
| timerCheckInterval              | a iterval time for pool to scan idle-timeout conencitons (ms)            | 18000                     |
| forceDirtyOnSchemaAfterSet      | set dirty flag on schema by force,ignore whether change(supports transcation)     | false                     |
| forceDirtyOnCatalogAfterSet     | set dirty flag on catlog by force,ignore whether change(supports transcation)     | false                     |
| enableThreadLocal               | an indicator to disable/enable threadlocal in pool（false to support VT          |  true                      | 
| enableJmx                       | enable indicator to support Jmx                                        | false                     | 
| printConfigInfo                 | indicator to print configuration items by log when pool initialize     | false                     | 
| printRuntimeLog                 | indicator to print runtime logs of pool                                | false                     | 
| **connectionFactory**               | connection factory instance                                            | blank                     |
| **connectionFactoryClass**          | connection factory class                                               | blank                     |
| **connectionFactoryClassName**      | connection factory class name                                          | blank                     |
| **evictPredicate**                  | predicate instance                                                     | blank                     |
| **evictPredicateClass**             | predicate class                                                        | blank                     |
| **evictPredicateClassName**         | predicate class name                                                   | blank                     |
| **jdbcLinkInfoDecoder**             | decoder instance of jdbc link info                                     | blank                     |
| **jdbcLinkInfoDecoderClass**        | decoder class of jdbc link info                                        | blank                     |
| **jdbcLinkInfoDecoderClassName**    | decoder class name of jdbc link info                                   | blank                     |

<sup>*The above attributes can be filled by their set methods; The priority order of effective selection of object type
properties: instance>class>class name<br/>
*Five defaultxxx properties(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly), if them not be set,then read value as default from first success creation connection</sup>

--- 
**Properties file of configuration**

BeeCP supports loading configuration information from properties files,referrence example is blow

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

Within BeeCP, JDBC drivers or connection factories are used to create connection objects (Connections), which typically work based on parameter patterns. Therefore, two methods are provided on the BeeCP Data Source Configuration object (BeeDataSourceConfig) to add their parameters, which are injected into the driver or factory during BeeCP initialization.

* ``` addConnectProperty(String,Object);// Add a single parameter ```

* ``` addConnectProperty(String);// Add multiple parameters, character format reference: cachePrepStmts=true&prepStmtCacheSize=250  ```


Example for this

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
**Blocking and interruption**

Due to network, server, or other reasons, the client is unable to establish a connection with the database, resulting in the connection creator thread of the client being blocked, which affects the use of the connection pool. BeeCP provides two methods on the data source object (BeeDataSource)

* ``` getPoolMonitorVo();//Query method, the result object constains that number of idle connections, borrowed connections, creating connections, creating timeouts, etc ```

* ``` interruptConnectionCreating(boolean);//If thread is blocked during creation a connection,calling this method can be used to end the blocking;If method parameter is true,only interrupt timeout creation ```


<sup>**additional description**</sup></br>
1：The creation timeout is same to maxWait.For example:if this value is 8 seconds,and no connection returned from driver or connection factory,this called as creation timeout</br>
2：After creation timeout and not be interrupted, BeeCP timed thread will scan out them and interrupted them</br>
3：Connecton Creation info of pool also display on BeeCP monitor page and provide interruption button on page


--- 
**Clean and Reinitialization**

BeeCP provides two clear methods on the data source (BeeDataSource) to clean up the connections created in the pool and restore the pool to its initial state,not accept external requests during clean

* ```clear(boolean forceCloseUsing);//forceCloseUsing is true,then recyle borrowed conenction by force ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing is true,then recyle borrowed conenction by force；then reinitiaize pool with new configuration```

*_Interrupt them if connection creation exist druing clean process;let waiters to exit waiting for ending request of connection getting_

--- 
**Connection Eviction**

During the use of connections, SQL exceptions may occur, some of which are normal exceptions, while others are more serious issues that need to be removed (evicted) from the pool; How to identify exceptions that need to be expelled, BeeCP provides three configuration methods

* A. configuration of exception code：``` addSqlExceptionCode(int code)；//related to SQLException.vendorCode ```

* B. configuration of exception state：``` addSqlExceptionState(String state)；/related to SQLException.SQLState```

* C. configuration of predicate：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);```
    
<sup>**additional description**</sup></br>
1：If predicate set, then ignore the other two configurations;evict connection from pool where check reuslt of sql exception is not null/empty</br>
2：If predicate not configured,exception code check is priority to exception state check, if matched,then evict connections</br>
3：Force eviction,call abort method of connection(connect.abort (null))</br>
4：After eviction,if exist waiter for connection transfer,then create a new conenction and transfer it to waiter  

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
    private final Driver driver;
    private final Properties connectInfo;

    public MyConnectionFactory(String url, Properties connectInfo, Driver driver) {
        this.url = url;
        this.driver = driver;
        this.connectInfo = connectInfo;
    }

    public Connection create() throws SQLException {
        return driver.connect(url, connectInfo);
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



