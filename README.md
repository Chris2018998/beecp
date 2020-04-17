[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
BeeCP is a high performance JDBC connection pool

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>

Feature
--- 

1: Good performance: faster than <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> 

2: Less code: 21 files, 2600 lines of source code

Performance
---
 1: Pool connection borrow test:1000 threads X 1000 times
 
[datasource.getConnection(),connection.close()]</i>

1：Avg time(milliseconds) 

Bee_C(0.0001) > Bee_F(0.0534) > HikariCP(0.1515)

```java
PC: Win7 I3-7100 8G mysql5.6.46_64

Pool init size10, max size:10

Pool version: HikariCP-3.3.1,BeeCP-2.3.2

DB restart after every pool test
```


<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200417_JDBCPool_I37100.log">20200417_JDBCPool_I37100.log</a>

project for performance test code,please visit：https://github.com/Chris2018998/PoolPerformance
 
2：Test with HikariCP benchmark(I3-7100,8G)

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200315_I3_7100_HikariCP_Bech_Pict.png"></img>

Download <a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>
 

Demo(SpringBoot)
--- 

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


Maven download
---

Java7
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>2.3.1</version>
</dependency>
```

Java6
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.6.7</version>
</dependency>
```

*Friendly tips: the latest version is recommended*

Function List
---
1: Request timeout support

2: Two modes: fairness and competition

3: Pool recreate new connections when network restore

4: Idle timeout and holding timeout 

5: Preparedstatement cache support (optional)

6: Before connection recovery, things can be rolled back

7: Support property reset before connection recycling (for example: autocommit, transactionisolation, readonly, Catlog, schema, networktimeout)

8: JMX support

9: Support connection factory customization


Configuration
---
| Configuration item |   Description                |   remark                          |
| ----------------   | ---------------------------  | ------------------------          |
| username           | JDBC username                 |                                   |
| password           | JDBC password                 |                                   |
| jdbcUrl            | JDBC url                      |                                   |
| driverClassName    | Driver class name             |                                   |
| poolName           | Pool name                     |                                   |
| fairMode           | fair mode for pool            | default is false                   |
| initialSize        | pool initial size             |                                   |
| maxActive          | pool max size                 |                                   | 
| concurrentSize     | borrower thread concurrent size  | not greater than  'maxActive'   |
| preparedStatementCacheSize |statment cache size       | 0 cache is invalid            |
| testOnBorrow       |test connection valid on borrowed | invalid,then close it         |
| testOnReturn       |test connection valid on return   |  invalid,then close it        |
| defaultAutoCommit  |default autoCommit                | default is true               |
| defaultTransactionIsolation|trasaction level          | default:Connection.TRANSACTION_READ_COMMITTED |
| defaultCatalog     |                                  |                                     |
| defaultSchema      |                                  |                                     |
| defaultReadOnly    |                                  | default is false                     |
| maxWait            |max wait time to borrow a connection(mills)| default is 8 seconds       |
| idleTimeout        |max idle time in pool(mills)      | default is 3 minutes                |  
| holdIdleTimeout    |max hold idle time in pool(mills)  | default is 5 minutes              |  
| connectionTestSQL  |Connection valid test sql          | a 'select' statment               |  
| connectionTestTimeout |Connection valid test timeout(seconds)  | default 5 seconds         |  
| connectionTestInterval |connection valid test interval time(mills)| default 500ms          |  
| forceCloseConnection   |connection close force ind  |default is false,true:close using directly，false:close using when it is idle|
| waitTimeToClearPool    |wait time to clean when exist using conneciton（seconds） | default is 3 seconds |                  
| idleCheckTimeInterval  |idle check time interval(mills)  |                     |
| idleCheckTimeInitDelay |idle check thread delay time to check first|                    |
| connectionFactoryClassName|Custom JDBC connection factory class name              | default is null          |
| enableJMX                 |JMX Ind                                |                    | |
	
