[English](README.md)|[中文](README_CN.md)

![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![](https://img.shields.io/github/license/Chris2018998/BeeCP)

BeeCP is a lightweight JDBC connection pool,its Jar file only 133KB and its techology highlights: caching single connection, non moving waiting, fixed length array

##
Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.7</version>
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

##
✨**Highlight Features**

* Provide interruption mehod to broke blocking
* Support Pool clean and pool reinitalization
* Support properties file configuration
* Provide interfaces for customization
* Support virtual thread applications
* [Provide web monitor](https://github.com/Chris2018998/beecp-starter)

<img width="1280" alt="image" src="https://github.com/user-attachments/assets/dcff61d5-d5e5-4b21-bbed-b53ef919e17f" /><br/>

<img width="1280" alt="image" src="https://github.com/user-attachments/assets/03937fe0-c559-49a8-b3c2-debcbb8f76b9" />

_Reminder: If your project is built on springboot framework and also you are interested at beecp or already using it,we recommend [beecp starter](https://github.com/Chris2018998/beecp-starter) to
you._

📊**JMH Performance**

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>

🍒***Compare to HikariCP***

| item                                           | HikariCP             | BeeCP                 |
|------------------------------------------------|----------------------|-----------------------|
| Size of connection in threadlocal              | One or more          | Single                |
| Type of container store connections            | CopyOnWriteArrayList | A fixed length array  |
| Transfer queue/wait queue                      | SynchronousQueue     | ConcurrentLinkedQueue |
| Asyn way of thread to create connections       | Thread pool          | Single thread         |
| Support concurrency cretion of connections     | Not Support          | Support               |
| Support clean and reinitialization in pool     | Not Support          | Support               |
| Provide interruption method to broke blocking  | Not Provide          | Provide               |
| Provide connection factory interface           | Not Support          | Support               |
| Support threadLocal-cache disable              | Not Support          | Support               |
| Support XADataSource                           | Not Support          | Support               |

_[**HikariCP**](https://github.com/brettwooldridge/HikariCP) is an excellent open source project and widely used in the Java world,it is developed by Brettwooldridge,a senior JDBC expert of United States_

## 
👉**How to use it**

Its usage is generally similar to popular connection pools,and some reference source codes in followed chapters 

*_One: Directly use_, similar to the traditional DBC operation

```java

//step1: set parameters and create datasource
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.cj.jdbc.Driver");//driver class names
config.setJdbcUrl("jdbc:mysql://localhost/test");//or like it：setUrl("jdbc:mysql://localhost/test");
config.setUsername("root");//user name
config.setPassword("root");//password
BeeDataSource ds = new BeeDataSource(config);

//step2：get connection and use it
try(Connection con = ds.getConnection()){
  //......
}
```

* _Second：Indirect approach_,Register as a Spring Bean and used by persistence frameworks

```java
@Configuration
public class DataSourceConfiguration{

  @Bean
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource ds1(){
     return new BeeDataSource();
  }

  @Bean
  public DataSource ds2(){
    BeeDataSourceConfig config = new BeeDataSourceConfig();
    //.......set parameters
    return new BeeDataSource(config);
  }
}
```

* _Third：[beecp-starter](https://github.com/Chris2018998/beecp-starter)_，File configuration, supporting multiple sources

##
🔡**Configuration properties**

BeeCP woring parameters are from its configuration object(BeeDataSourceConfig),below is a list of properites,which can be confiured by their set methods

| property name                   | description                                                            | default value             |
|---------------------------------|------------------------------------------------------------------------|---------------------------|
| username                        | user name of db                                                        | blank                     |
| password                        | user password of db                                                    | blank                     |
| jdbcUrl                         | link url to db                                                         | blank                     |
| driverClassName                 | jdbc driver class name                                                 | blank                     |
| poolName	                  | pool name,if not set,a generated name will be assigned to it           | blank                     |
| fairMode                        | a mode to get connections from pool                                    | false（unfair mode）       | 
| initialSize                     | creation size of connecitons during pool initialization                | 0                         |
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
| parkTimeForRetry                | timed wait for borrowed connections to return to pool and close them(ms)   | 3000                      |             
| timerCheckInterval              | a iterval time for pool to scan idle-timeout conencitons (ms)              | 18000                     |
| forceDirtyOnSchemaAfterSet      | force reset flag for schema property when conneciton close(can used in app of PG) | false                     |
| forceDirtyOnCatalogAfterSet     | force reset flag for catlog property when conneciton close(can used in app of PG) | false                     |
| enableThreadLocal               | an indicator to enable/disable threadlocal in pool（false to support VT)    |  true                      | 
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

***Object type properties**，choosed priority order：instance > class > class name

***Object type properties**，property class must be not abstract and a constructor without parameters exist in class

***Five defaultxxx properties**(defaultAutoCommit,defaultTransactionIsolationCode,defaultCatalog,defaultSchema,defaultReadOnly), if them not be set,then read value as default from first success creation connection

##
📝**Properties file of configuration**

BeeCP supports loading configuration from properties type files and properties objects(java.util.Properties),a referrence example is blow

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

#implemention class name of connection factory 
connectionFactoryClassName=org.stone.beecp.objects.MockCommonConnectionFactory
#implemention class name of link info decoder
jdbcLinkInfoDecoderClassName=org.stone.beecp.objects.SampleMockJdbcLinkInfoDecoder

```
Reminder: The configuration format of properties name currently supports camel hump, middle line, underline

##
⚙**Driver parameters**

BeeCP internally uses drivers or connection factories to create connection objects, and factories may depend on some parameters. Two methods are provided in the configuration object (BeeDataSourceConfig) to for it

* ``` addConnectProperty(String,Object);// Add a parameter ```

* ``` addConnectProperty(String);// Add multiple parameters,for example: cachePrepStmts=true&prepStmtCacheSize=250  ```


An example

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

* _Refrence poperites file1_
```properties

connectProperties=cachePrepStmts=true&prepStmtCacheSize=50

```

* _Refrence Poperites file2(recommended it when multiple parameters)_

```properties
connectProperties.size=2
connectProperties.1=prepStmtCacheSize=50
connectProperties.2=prepStmtCacheSqlLimit=2048&useServerPrepStmts=true
```

##
🔚**Connection Eviction**

 BeeCP provides two ways

1. Manual eviction, call the abort method of connections (connect. abort (null)), pool immediately physically closes them and removes them

2. Eviction by configuration，which is used to help pool identify connections thrown SQLException, there are three configuration way for it

 * A. configuration of exception code：``` addSqlExceptionCode(int code)；//related to SQLException.vendorCode ```
 * B. configuration of exception state：``` addSqlExceptionState(String state)；/related to SQLException.SQLState```
 * C. configuration of predicate：``` setEvictPredicate(BeeConnectionPredicate p);setEvictPredicateClass(Clas c); setEvictPredicateClassName(String n);```
 
<br/>

_**Properties File(example)**_
```properties

sqlExceptionCodeList=500150,2399,1105
sqlExceptionStateList=0A000,57P01,57P02,57P03,01002,JZ0C0,JZ0C1

//or
evictPredicateClassName=org.stone.beecp.objects.MockEvictConnectionPredicate

```

_**Additional info**_

1：If predicate set, then ignore the other two configurations;evict connection from pool where check reuslt of sql exception is not null/empty</br>
2：If predicate not configured,exception code check is priority to exception state check, if matched,then evict connections</br>
3：Force eviction,call abort method of connection(connect.abort (null))</br>
4：After eviction,if exist waiter for connection transfer,then create a new conenction and transfer it to waiter 


##
✂**Interruption when blocking**

Connection creation is an important activity in pool, but due to server, network, or other reasons, the creation process may be blocked. To address this issue, BeeCP provides two ways to solve it


1. External approach, providing two methods,query method： **BeeDataSource.getPoolMonitorVo()** ；Interruption method： **BeeDataSource.interruptConnectionCreating(boolean)** ；

2. Internal approach，internal worker thread scan and find out all blocking and interrupt them

<br/>

_**Additional info**_

* 1：If elapsed time of conneciton creation is greater than maxwait value,pool regards it as blocking 
* 2: If borrower thread is interrupted,then an interrupt exception will be thrown from  **getConnection ** method
* 3: Creation info and blocking info is also display on monitor page


##
🛒**Clean and Reinitialization**

BeeCP provides two clear methods on the data source (BeeDataSource) to clean up the connections created in the pool and restore the pool to its initial state,not accept external requests during clean

* ```clear(boolean forceCloseUsing);//forceCloseUsing is true,then recyle borrowed conenction by force ```

* ```clear(boolean forceCloseUsing, BeeDataSourceConfig config);//forceCloseUsing is true,then recyle borrowed conenction by force；then reinitiaize pool with new configuration```

*_Interrupt them if connection creation exist druing clean process;let waiters to exit waiting for ending request of connection getting_


##
🏭**Factory customization**

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

