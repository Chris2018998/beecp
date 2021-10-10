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
   <version>1.6.10</version>
</dependency>
```

## :thumbsup: Highlight

1：Single connection threadLocal cache to improve pooling performance 

2：Borrower non move waiting, saving queue entry and exit costs 

3: Transfer queue reuse, which can transfer connections and exceptions 

4：Connection asynchronized-add thread,which can be triggerred by releaser or waiter


## :cherries: Compare to HikariCP

|    Item      |    BeeCP                                                    |      HikariCP                                             |  
| -----------  |----------------------------------------------------------   | ----------------------------------------------------------|          
|Key           |ThreadLocal + semaphore+ ConcurrentLinkedQueue +Thread       |FastList + ConcurrentBag + ThreadPoolExecutor              | 
|Similarity    |CAS,Proxy pre-generation,Driver statement cache,Jmx          |                                                           |
|Difference    |Balance mode,Hold-timeout,Support XA,Pool clean              |Pool suspend,Config runtime change                         |                            |File          |32 source files,Jar package 93KB                             |44 source files,Jar package 158KB                          | 
|Performance   |Higher than 40%                                              |                                                           |

Which defects of HikariCP?
 
1：<a href="https://my.oschina.net/u/3918073/blog/4645061">Closed preparedStatements can be activation, when using MySQ-driver</a> 

2：<a href="https://my.oschina.net/u/3918073/blog/5053082">When database down or network failed, getConnection response time == 'connectionTimeout'(if configed value is large,what happen?) </a>

3：<a href="https://my.oschina.net/u/3918073/blog/5171229">Exists transaction leak,when using 'setSavepoint' on connection</a>

.....


**Conclusion:** faster, simpler, reliabler



## :tractor: Demo

Its usage is roughly similar to other pool, two reference examples below

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

:sunny: *If your projects are based on springboot, we recommend<a href="https://github.com/Chris2018998/BeeCP-Starter"> BeeCP-Starter </a>
to manage your datasource(file configuration, less code, monitor-ui)*


## :book: Configuration item 

###### :capital_abcd: poolName 

If not configured, auto generated

###### :1234: fairMode

Boolean indicator,if true,pool will use fair semaphore and fair transfer policy. **default value:** false

###### :capital_abcd: initialSize

Size of connections on pool starting,if zero,pool will try to create one.**default value:** 0

###### :1234: mxActive

Max reach size of connections in pool.**default value:** 10
 
###### :capital_abcd: borrowSemaphoreSize

Size of semaphore in pool. **default value:** number of CPU cores 

###### :1234: defaultAutoCommit

Value setting on conneciton creating and return, *default value:**true

###### :capital_abcd: defaultTransactionIsolationCode

Value setting on conneciton creating and return. **default value:**-999,if not set then read value from first connection 

###### :1234: maxWait

Max wait time for one connection for borrower using 'getConnection'. unit: milliseconds, **default value:** 8000

###### :capital_abcd: idleTimeout

Max idle time of connections in pool,when reach,then remove from pool.unit: milliseconds, **default value:** 18000
 
###### :1234: holdTimeout

Max no-use time of borrowed connections,when reach,then return them to pool by forced close.unit: milliseconds, **default value:** 18000

###### :capital_abcd: connectionTestSql

Connection valid test sql on borrowed. **default value:** SELECT 1

###### :1234: connectionTestTimeout

Max time to get a valid test result. unit:second, **default value:** 3
 
###### :capital_abcd: connectionTestInterval

Conenction valid assume time after last activity,if borrowed,not need test during the duration.unit: milliseconds, **default value:** 500


:point_right: <a href="https://github.com/Chris2018998/BeeCP/wiki/%E9%85%8D%E7%BD%AE%E9%A1%B9%E5%88%97%E8%A1%A8">More configuration items </a>

