[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
BeeCP is a high performance JDBC connection pool

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>

Feature
--- 

1: Good performance,faster than HikariCP

2: Less code: 21 files, 2600 lines of source code
 
Performance
---

1: One million times borrow tests[1000 X 1000],time scope:[datasource.getConnection(),connection.close()]

| Time(ms)         |   HikariC3.3.1  |  Bee_F(BeeCP-2.3.2)| Bee_C(BeeCP-2.3.2)    |
| ---------------- |---------------- | -------------------| ----------------------| 
| Total time       | 151516          | 53384              |          142          | 
| Avg time         | 0.1515          | 0.0534             |        0.0001         ||  

Bee_F:Fair Mode Pool，Bee_C:Compete Mode Pool

Total time=Thread1 time + Thread2 time + ...... + Thread1000 time,  Avg time  = Total time/1000000

PC: Win7 I3-7100 8G mysql5.6.46_64,  Pool Setting: init size10, max size:10
 
DB restart after every test,log file:<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200417_JDBCPool_I37100.log">20200417_JDBCPool_I37100.log</a>

project for performance test code,please visit：https://github.com/Chris2018998/PoolPerformance


2：Test with HikariCP benchmark(I3-7100,8G)

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/PoolPerformaceCompare.png"></img>

Download <a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>



Demo1
---

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

Demo2(SpringBoot)
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


Release download
---

Java7
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.0.6</version>
</dependency>
```

Java6
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.8</version>
</dependency>
```

*Friendly tips: the latest version is recommended*

Function List
---
1: Request timeout support

2: Two modes: fairness and competition

3: Pool recreate new connections when network restore

4: Idle timeout and holding timeout 

5: Before connection recovery, things can be rolled back

6: Support property reset before connection recycling (for example: autocommit, transactionisolation, readonly, Catlog, schema, networktimeout)

7: XDataSource

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
| borrowConcurrentSize | borrower thread concurrent size  | not greater than  'maxActive'   |
| defaultAutoCommit  |default autoCommit                | default is true               |
| defaultTransactionIsolation|trasaction level          | default:Connection.TRANSACTION_READ_COMMITTED |
| defaultCatalog     |                                  |                                     |
| defaultSchema      |                                  |                                     |
| defaultReadOnly    |                                  | default is false                     |
| maxWait            |max wait time to borrow a connection(mills)| default is 8 seconds       |
| idleTimeout        |max idle time in pool(mills)      | default is 3 minutes                |  
| holdIdleTimeout    |max hold time in not using        | default is 5 minutes              |  
| connectionTestSQL  |Connection valid test sql          | a 'select' statment               |  
| connectionTestTimeout |Connection valid test timeout(seconds)  | default 5 seconds         |  
| connectionTestInterval |connection valid test interval time(mills)| default 500ms          |  
| forceCloseConnection   |connection close force ind  |default is false,true:close using directly，false:close using when it is idle|
| waitTimeToClearPool    |wait time to clean when exist using conneciton（seconds） | default is 3 seconds |                  
| idleCheckTimeInterval  |idle check time interval(mills)  |                     |
| idleCheckTimeInitDelay |idle check thread delay time to check first|                    |
| connectionFactoryClassName|Custom JDBC connection factory class name              | default is null          |
| enableJMX                 |JMX Ind                                |                    | |
	

JDBC Driver and DB List
---
|  DB             |  JDBC Driver Class              |   Refer url                  |
| ----------------| ---------------------------     | ------------------------    |
|Mariadb         |org.mariadb.jdbc.Driver   	   |  jdbc:mariadb://localhost/test  |
|MySQL            |om.mysql.jdbc.Driver            |  jdbc:mysql://localhost/test    |
|Oracle          |oracle.jdbc.driver.OracleDriver |  jdbc:oracle:thin:@localhost:1521:orcl|
|MSSQL           |com.microsoft.sqlserver.jdbc.SQLServerDriver | jdbc:sqlserver://localhost:1433;databaseName=test|
|Postgresql      |org.postgresql.Driver                 |  jdbc:postgresql://localhost:5432/postgres|

