<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
<img height="20px" width="20px" align="bottom" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>

<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 <a><img src="https://img.shields.io/github/v/release/Chris2018998/beecp.svg"></a> 
</p> 

## :coffee: Introduction 

BeeCP is a high-performance JDBC connection pool

## :tea: Origin 

BeeCP is derived from the sub-module transformation of the Jmin project (Java tool suite set, created in 2004) 

## :arrow_down: Download 

Java7 or higher
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

## :thumbsup: Highlight

1：ThreadLocal connection single cache to improve pooling performance 

2：Borrower does not move and waits, saving queue entry and exit costs 

3：Transmission pipeline reuse, which can transmit connections and exceptions 

4：Two-way asynchronous alternates, eliminating the time difference between the waiter and the sender


## :cherries: Compare to HikariCP pool 

|    Item      |    BeeCP                                                    |      HikariCP                                             |  
| -----------  |----------------------------------------------------------   | ----------------------------------------------------------|          
|Key technology|ThreadLocal + signal+ ConcurrentLinkedQueue +Thread          |FastList + ConcurrentBag + ThreadPoolExecutor              | 
|Similarity    |CAS，Agent pre-generation,not supply statement cache         |                                                           |
|Difference    |Support balance mode, support XA, force recovery of unused connections|                                                  |
|file          |32 source files, Jar package 93KB                            |44 source files, Jar package 158KB                         | 
|performance   |The overall performance is higher than 40% (optical connection pool benchmark) |                                         |

What are the defects of HikariCP? 

1：<a href="https://my.oschina.net/u/3918073/blog/4645061">For MySQL-driven applications, can the closed PreparedStatement be resurrected?</a> 

2：<a href="https://my.oschina.net/u/3918073/blog/5053082">Database Down machine or network problems, slow response</a>

3：<a href="https://my.oschina.net/u/3918073/blog/5171229">Transactional vulnerabilities</a>

.....

## :tractor: Demo

The usage is roughly similar to the general pool, there are two reference examples below 

###### :point_right: Demo1

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.jdbc.Driver");
config.setJdbcUrl("jdbc:mysql://localhost/test");
config.setUsername("root");
config.setPassword("root");
BeeDataSource ds=new BeeDataSource(config);
Connection con=ds.getConnection();
....

```

###### :point_right: Demo2

*application.properties*

```java
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.url=jdbc:mysql://localhost/test
spring.datasource.driverClassName=com.mysql.jdbc.Driver
``` 

*DataSourceConfig.java*
```java
@Configuration
public class DataSourceConfig {
  @Value("${spring.datasource.username}")
  private String user;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.driverClassName}")
  private String driver;

  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(cn.beecp.BeeDataSource.class).build();
  }
}
```

:sunny: *If the project is of Springboot type, recommend<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a>（No code development configuration is required, and it comes with a monitoring interface）*


## :book: Configuration item 

###### :capital_abcd: poolName 

If it is not configured, the system will automatically generate it in the format ：FastPool-x 

###### :1234: fairMode

The pool supports two modes: fair and competitive, and the default is the competitive mode; in the fair mode, the borrower obtains the connection on the first-come, first-served basis 

###### :capital_abcd: initialSize

When the pool is initialized, the number of connections is constructed. If it is 0, the pool will create 1 by default 

###### :1234: mxActive

The maximum number of active connections in the pool, the default value is 10 

###### :capital_abcd: borrowSemaphoreSize

The size of the semaphore in the pool, the default is the number of CPU cores 

###### :1234: defaultAutoCommit

The attribute default value setting of AutoCommit on the connection, the default is true 

###### :capital_abcd: defaultTransactionIsolationCode

The TransactionIsolation transaction isolation level setting of the connection, if not set, the default value is subject to the driver 

###### :1234: maxWait

The maximum waiting time of the borrower when obtaining a connection, the time unit is milliseconds, the default value is 8000

###### :capital_abcd: idleTimeout

The connection idle timeout time, if exceeded, it will be removed, the time unit is milliseconds, the default value is 18000 

###### :1234: holdTimeout

The connection that has been borrowed, if it is not active (executing SQL) within a specified period of time, it will be forcibly recycled, the default value is 18000 

###### :capital_abcd: connectionTestSql

Connection activity test Sql query statement, it is recommended not to embed procedure statement, it must be provided 

###### :1234: connectionTestTimeout

The response time range of connection activity test, the time unit is second, the default is 3 seconds 

###### :capital_abcd: connectionTestInterval

Connection activity test interval time, the current activity is assumed to be valid within the current time from the last activity, the default is 500 milliseconds 

:point_right: <a href="https://github.com/Chris2018998/BeeCP/wiki/%E9%85%8D%E7%BD%AE%E9%A1%B9%E5%88%97%E8%A1%A8">More configuration items </a>


## :sparkling_heart: Donate

If you think this work is good, please donate us a cup of coffee, thank you^_^。

<img height="50%" width="50%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/donate.png"> 
