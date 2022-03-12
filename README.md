<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|
<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)
<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
</p> 

## :coffee: Introduction 

BeeCP is a lightweight JDBC connection pool 

## :arrow_down: Download 

Java7 or higher
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.2</version>
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
|**Item Name**                     |**Desc**                              |**Default**                          |
| ---------------------------------| ------------------------------------- | ----------------------------------- |
|username                          |jdbc username                          |empty                                |
|password                          |jdbc password                          |empty                                |
|jdbcUrl                           |jdbc url                               |empty                                |
|driverClassName                   |jdbc driver class name                 |empty                                |
|poolName	                   |pool name,if not set,auto generated    |empty                                |
|fairMode                          |indicator,true:pool will use fair semaphore and fair transfer policy|false   | 
|initialSize                       |size of connections on pool starting      |0                                 |
|maxActive                         |max reachable size of connections in pool |10                                | 
|borrowSemaphoreSize               |max permit size of pool semaphore         |min(maxActive/2,CPU core size）   |
|defaultAutoCommit                 |'autoCommit' property default value       |true                 |
|defaultTransactionIsolationCode   |'transactionIsolation'property default value,if not set,then read out from first connection|-999|
|defaultCatalog                    |'catalog' property default value        |empty                                 |
|defaultSchema                     |'schema' property default value         |empty                                 |
|defaultReadOnly                   |'readOnly' property default value       |false                                 |
|maxWait                           |milliseconds:max wait time to get one connection from pool|8000                |
|idleTimeout                       |milliseconds:max idle time of connections,when reach,then close them and remove from pool|18000|                             
|holdTimeout                       |milliseconds:max no-use time of borrowed connections,when reach,then return them to pool by forced close           |18000                             |  
|validTestSql                      |connection valid test sql on borrowed              |SELECT 1                            |  
|validTestTimeout                  |seconds:max time to get valid test result          |3                                   |  
|validAssumeTime                   |milliseconds:connections valid assume time after last activity,if borrowed,not need test during the duration                   |500                               |  
|forceCloseUsingOnClear            |using connections forced close indicator on pool clear|false                            |
|delayTimeForNextClear             |milliseconds:delay time for next loop to clear,when<code>forceCloseUsingOnClear</code> is false and exists using connections                  |3000                                |                   
|timerCheckInterval                |milliseconds:interval time to run timer check task|18000                               |
|connectionFactoryClassName        |raw JDBC connection factory class name            |empty                               |
|enableJmx                         |boolean indicator,true:register dataSource to jmx |false                               | 
|enableConfigLog                   |boolean indicator,true:print config item info on pool starting|false                   | 
|enableRuntimeLog                  |boolean indicator,true:print runtime log                      |false                   | 
