![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)<br/>
<a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
<a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 
## :coffee: Introduction 

BeeCP, a small JDBC connection pool: high performance, lightweight code and good stability.

*  Support main popular database drivers
*  Support XAConnection/JTA 
*  Pool features:CAS,single connection cache, queue reuse, non move waiting self spin, asynchronous add , safe close,web monitor and so on
*  Good robustness and quick response to unexpected situations (such as network disconnection and database service crash)
*  Good interface extensibility

## :arrow_down: Download 

Java7 or higher
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.9</version>
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
## :tractor: Example

### :point_right: Example-1(independent)

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
### :point_right: Example-2(Springbooot)

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
  
  @Bean
  public DataSource secondDataSource() {
   return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
  }
}
```

## :book: Function map
![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)

## :computer: Runtime monitor

Three ways are provided in pool

* slf4j log
* Jmx mbean
* Pool Vo(get it by call datasource method:getPoolMonitorVo)

:sunny: *If your project is using beecp and base on springboot, we recommend our datasource management tool:<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a> (web ui, no code development ,just some configuration)*


![图片](https://user-images.githubusercontent.com/32663325/178511569-8f6e16f4-92fc-41ee-ba6b-960e54bf364b.png)

## :cherries: Compare to HikariCP

| **Compare Item**                |**BeeCP**                                               |       **HikariCP**                                |
|---------------------------------|--------------------------------------------------------| ------------------------------------------------- |
| key technology                  |ThreadLocal，Semaphore，ConcurrentLinkedQueue，Thread    | FastList，ConcurrentBag，ThreadPoolExecutor        |
| Similarities                    |CAS，pre-generate proxy，driver statement cache          |                                                   |
| Difference                      |fair mode，supprt XA，recycle hold-timeout connection，single connection cache，queue reuse，non move waiting spin|pool pause|
| Files                           |37 files，95KB Jar                                      |44 files，158KB Jar                                 |
| Performance                     |40 percent faster （HikariCP bench）                    |               


## :green_apple: Code quality

![图片](https://user-images.githubusercontent.com/32663325/163173015-2ce906f3-1b83-419d-82aa-a42b5c8d92b8.png)

## :factory: User Extend

### 1：Connection factory interfaces

Two interfaces,which are using to create raw connection or raw XAConnection for self-implement and its subclass name need set to 'connectionFactoryClassName' in Bee DataSourceConfig object.
 
![图片](https://user-images.githubusercontent.com/32663325/153597017-2f3ba479-8f3f-4a82-949b-275068c287cd.png)
 
![图片](https://user-images.githubusercontent.com/32663325/153597130-a22c0d92-2899-46db-b982-35b998434eae.png)
 
Example

![图片](https://user-images.githubusercontent.com/32663325/183244013-e2b32f8b-40d7-45d4-add2-e5394bddae3a.png)

### 2：Jdbc password ciphertext decrypt class

![图片](https://user-images.githubusercontent.com/32663325/153597176-e48382b9-7395-4c6c-9f34-425072d7c510.png)

## :blue_book: Configuration 
|**Item Name**                     |**Desc**                               |**Default**                          |
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
|printConfigInfo                   |boolean indicator,true:print config item info on pool starting|false                   | 
|printRuntimeLog                   |boolean indicator,true:print runtime log                      |false                   | 
