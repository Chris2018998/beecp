<p align="left">
<a><img src="https://img.shields.io/circleci/build/github/Chris2018998/beecp"></a>
<a><img src="https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31"/></a>
<a><img src="https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3NP"/></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
<a><img src="https://img.shields.io/badge/Java-7+-green.svg"></a>
<a><img src="https://img.shields.io/github/license/Chris2018998/BeeCP"></a>
</p>

![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)
&nbsp;<a href="https://github.com/Chris2018998/stone/blob/main/README.md">:house:</a>|
<a href="https://github.com/Chris2018998/BeeCP/edit/master/README_cn.md">中文</a>|
<a href="https://github.com/Chris2018998/BeeCP/edit/master/README.md">English</a>

## :coffee: Introduction

BeeCP is a small JDBC connection pool: high performance, lightweight code and good stability.

* Support main popular database drivers
* Support XAConnection/JTA
* Pool features:CAS,single connection cache, queue reuse, non move waiting self spin, asynchronous add , safe close,web
  monitor and so on
* Good robustness and quick response to unexpected situations (such as network disconnection and database service crash)
* Good interface extensibility

## :arrow_down: Download

Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>4.1.2</version>
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

Two ways are provided in pool

* Jmx mbean
* Pool Vo(get it by call datasource method:getPoolMonitorVo)

:sunny: *If your project is using beecp and base on springboot, we recommend our datasource management
tool:<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a> (web ui, no code development ,just some
configuration)*

![图片](https://user-images.githubusercontent.com/32663325/178511569-8f6e16f4-92fc-41ee-ba6b-960e54bf364b.png)

## :cherries: Compare to HikariCP

| **Compare Item** | **BeeCP**                                                                                                     | **HikariCP**                              |
|------------------|---------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| key technology   | ThreadLocal，Semaphore，ConcurrentLinkedQueue，Thread                                                            | FastList，ConcurrentBag，ThreadPoolExecutor |
| Similarities     | CAS，pre-generate proxy，driver statement cache                                                                 |                                           |
| Difference       | fair mode，supprt XA，recycle hold-timeout connection，single connection cache，queue reuse，non move waiting spin | pool pause                                |
| Files            | 37 files，95KB Jar                                                                                             | 44 files，158KB Jar                        |
| Performance      | 40 percent faster （HikariCP bench）                                                                            |

## :green_apple: Code quality

![图片](https://user-images.githubusercontent.com/32663325/163173015-2ce906f3-1b83-419d-82aa-a42b5c8d92b8.png)

## :factory: User Extend

### 1：Connection factory interfaces

Two interfaces,which are using to create raw connection or raw XAConnection for self-implement and its subclass name
need set to 'connectionFactoryClassName' in Bee DataSourceConfig object.

![图片](https://user-images.githubusercontent.com/32663325/153597017-2f3ba479-8f3f-4a82-949b-275068c287cd.png)

![图片](https://user-images.githubusercontent.com/32663325/153597130-a22c0d92-2899-46db-b982-35b998434eae.png)

Example

![图片](https://user-images.githubusercontent.com/32663325/183244013-e2b32f8b-40d7-45d4-add2-e5394bddae3a.png)

### 2：Jdbc password ciphertext decrypt class

![图片](https://user-images.githubusercontent.com/32663325/153597176-e48382b9-7395-4c6c-9f34-425072d7c510.png)

## :blue_book: Configuration

| **Item Name**                   | **Desc**                                                                                                | **Default**                                                                       |
|---------------------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| username                        | a username link to db                                                                                   | null                                                                              |
| password                        | password of a db user                                                                                   | null                                                                              |
| jdbcUrl                         | url link to db                                                                                          | null                                                                              |
| driverClassName                 | jdbc driver class name                                                                                  | null                                                                              |
| poolName	                       | a generation name assgin to it if not set                                                               | null                                                                              |
| fairMode                        | pool work mode                                                                                          | false                                                                             | 
| initialSize                     | connection number of creation on pool initizization                                                     | 0                                                                                 |
| asyncCreateInitConnection       | creation mode for initial connections                                                                   | false(synchronization mode)                                                       |   
| maxActive                       | max reachable count of connections in pool                                                              | 10                                                                                | 
| borrowSemaphoreSize             | max permit size of pool semaphore                                                                       | min(maxActive/2，CPU size）                                                         |
| defaultAutoCommit               | initial value of autoCommit prop on created connections                                                 | null,read prop value from first connection as initial value for other connections |
| defaultTransactionIsolationCode | initial value of transactionIsolation prop on created connections                                       | null,read prop value from first connection as initial value for other connections |
| enableThreadLocal               | thread local cache enable indicator                                                                     | true,set false to support virtual threads                                         |
| defaultCatalog                  | initial value of catalog prop on created connections                                                    | null,read prop value from first connection as initial value for other connections |
| defaultSchema                   | initial value of schema  prop on created connections                                                    | null,read prop value from first connection as initial value for other connections |
| defaultReadOnly                 | initial value of readOnly prop on created connections                                                   | null,read prop value from first connection as initial value for other connections |
| maxWait                         | max wait time in pool for borrowers to get connection,time unit：milliseconds                            | 8000                                                                              |
| idleTimeout                     | idle time out for connections in pool,time unit：milliseconds                                            | 18000                                                                             |                             
| holdTimeout                     | max inactive time of borrowed connections,time unit：milliseconds                                        | 0(never timeout)                                                                  |  
| aliveTestSql                    | alive test sql on borrowed connections,pool remove dead connections                                     | SELECT 1                                                                          |  
| aliveTestTimeout                | max wait time to get validation result on test connectionstime unit：seconds                             | 3                                                                                 |  
| aliveAssumeTime                 | a gap time value from last activity time to borrowed time point,if less,not test,time unit：milliseconds | 500                                                                               |  
| forceCloseUsingOnClear          | indicator on direct closing borrowed connections while pool clears                                      | false                                                                             |
| timerCheckInterval              | an interval time to scan idle connections,time unit：milliseconds                                        | 18000                                                                             |
| connectionFactoryClassName      | connection factory class name                                                                           | null                                                                              |
| sqlExceptionCodeList            | store sql exception codes for connection eviction check                                                 | null,related methods:addSqlExceptionCode,removeSqlExceptionCode                   |
| sqlExceptionStateList           | store sql exception state for connection eviction check                                                 | null,related methods:addSqlExceptionCode,removeSqlExceptionCode                   |
| evictPredicateClassName         | eviction predicate class name                                                                           | null,pool only it to check exception if set                                       |
| jdbcLinkInfoDecoderClassName    | short lifecycle object and used to decode jdbc link info                                                | null                                                                              |
| forceDirtyOnSchemaAfterSet      | dirty force indicator on schema property under PG driver                                                | false                                                                             |
| forceDirtyOnCatalogAfterSet     | dirty force indicator on schema property under PG driver                                                | false                                                                             |
| enableJmx                       | enable indicator to register configuration and pool to Jmx                                              | false                                                                             | 
| printConfigInfo                 | boolean indicator,true:print config item info on pool starting                                          | false                                                                             | 
| printRuntimeLog                 | boolean indicator,true:print runtime log                                                                | false                                                                             | 
