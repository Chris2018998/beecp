<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
<img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>

BeeCP：A lightweight,high-performance JDBC pool

Maven artifact(Java7 and higher)
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.2.2</version>
</dependency>
```
Maven artifact(Java6)
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.9</version>
</dependency>
```
---

### Performance

**1：** One million Mutil-thread query (10000 threads x 10 times)
|   Pool type      | HikariCP-3.4.5  | beecp-3.0.5_compete|  
| ---------------  |---------------- | ----------------- |          
| Average time(ms) |25.132750        | 0.284550          | 
#### SQL:select 1 from dual
#### PC:I5-4210M(2.6Hz,dual core4threads),12G memory Java:JAVA8_64 Pool:init-size10,max-size:10

Test log file：<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/temp/JDBCPool2020-11-06.log">JDBCPool2020-11-06.log</a>
 
Test soruce：https://github.com/Chris2018998/PoolPerformance

**2：** Test with HikariCP performance benchmark(I3-7100,8G)

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/PoolPerformaceCompare.png"></img> 

Test source：<a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>

---

#### Example-1

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.jdbc.Driver");
config.setJdbcUrl("jdbc:mysql://localhost/test");
config.setUsername("root");
config.setPassword("root");
config.setMaxActive(10);
config.setInitialSize(0);
config.setMaxWait(8000);//ms
//DataSource ds=new BeeDataSource(config);
BeeDataSource ds=new BeeDataSource(config);
Connection con=ds.getConnection();
....

```

#### Example-2（SpringBoot）

*application.properties*

```java
spring.datasource.username=xx
spring.datasource.password=xx
spring.datasource.url=xx
spring.datasource.driverClassName=xxx
spring.datasource.datasourceJndiName=xxx
``` 

*DataSourceConfig.java*
```java
@Configuration
public class DataSourceConfig {
  @Value("${spring.datasource.driverClassName}")
  private String driver;
  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.username}")
  private String user;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${spring.datasource.datasourceJndiName}")
  private String datasourceJndiName;
  private BeeDataSourceFactory dataSourceFactory = new BeeDataSourceFactory();
  
  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(cn.beecp.BeeDataSource.class).build();
  }
  
  @Bean
  public DataSource secondDataSource(){
    return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
  }
  
  @Bean
  public DataSource thirdDataSource()throws SQLException {
    try{
       return dataSourceFactory.lookup(datasourceJndiName);
     }catch(NamingException e){
       throw new SQLException("Jndi DataSource not found："+datasourceJndiName);
     }
  }
}
```

---

### Features

1：Borrow timeout

2：Fair mode and compete mode for borrowing

3：Proxy object safe close when return

4：Pooled connection cleared when network bad,pooled connection recreate when network restore OK

5：Idle timeout and hold timeout(long time inactively hold by borrower)

6：Connection transaction rollback if exist commit transaction when return 

7：Pooled connection closed when exception,then create new one and transfer it to waiter

8：Pooled connection attributes reset when return（autoCommit,transactionIsolation,readonly,catlog,schema,networkTimeout）

9：XADataSource support

10：support self defined connection factory

11：Pool Reset

12：JMX support

---
### Configuration
|     Field name         |       Description                               |   Remark                                                   |
| ---------------------  | ------------------------------------------------| -----------------------------------------------------------|
|username                |JDBC User                                       |                                                             |
|password                |JDBC Password                                   |                                                             |
|jdbcUrl                 |DBC URL                                         |                                                             |  
|driverClassName         |JDBC driver class name                          |                                                             |
|poolName                |pool name                                       |name auto generated when not set                            |                              
|fairMode                |boolean indicator for borrow fair mode           |true:fair mode,false:comepete mode;default is false         |
|initialSize             |pooled object creation size when pool initialized|default is 0                                                |
|maxActive               |max size for pooled object instances in pool     |default is 10                                               | 
|borrowSemaphoreSize     |borrow concurrent thread size                    |default val=min(maxActive/2,cpu size)                       |                       
|defaultAutoCommit       |connection transaction open indicator            |default is true                                             |
|defaultTransactionIsolation|connection default transaction level          |default is Connection.TRANSACTION_READ_COMMITTED             |
|defaultCatalog             |                                              |                                                            |
|defaultSchema              |                                              |                                                            |
|defaultReadOnly            |                                              |default is false                                            |
|maxWait                    |max wait time to borrow one connection        |time unit is ms,default is 8000 ms                          |                       
|idleTimeout                |max idle time of connection instance in pool  |time unit is ms,default is 18000 ms                         |  
|holdTimeout                |max inactive time hold by borrower            |time unit is ms,default is 300000 ms                        |  
|connectionTestSql          |connection valid test sql                     |select statement（don't recommand store procedure in select  |  
|connectionTestTimeout      |connection test timeout                       |time unit is second, default is 5 seconds                    |  
|forceCloseUsingOnClear     |using connection close indicator|true,close directly;false,wait util connection becoming idle,then close it |            |delayTimeForNextClear      |delay time to clear pooled connections        |time unit is ms,default is 3000 ms                          |                            |idleCheckTimeInterval      |scan thread time interval to check idle connection |time unit is ms,default is 300000 ms                   |
|connectionFactoryClassName |object factory class name                           |default is null                                         |
|enableJmx                 |JMX boolean indicator for pool                      |default is false                                        |

---
### Donate

If the software can help you, please donate fee of one coffe to us,thanks.

<img height="50%" width="50%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/donate.png"> 
