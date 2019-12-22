[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
BeeCP is a lightweight (15 files, 2000lines of source code) high-performance Java connection pool

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>

Release download
---

Java7
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>2.0.0</version>
</dependency>
```

Java6
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.6.0</version>
</dependency>
```

Configuration
---
|  Name           |  Description |   Remark |
| ----------------| ------------ | ------------ |
| initialSize     |Connection pool initial size|   |
| maxActive       |Maximum number of connection pools|    |
| maxWait         |Maximum borrowing waiting time (milliseconds)|   |
| idleTimeout     |connection maximum idleness time(milliseconds)|   |  
| preparedStatementCacheSize |preparedStatement cache Size |   
| validationQuery |Connection active Query Statement |    |   |


Refence demo With SpringBoot
---
```java
application.properties

spring.datasource.username=xx
spring.datasource.password=xx
spring.datasource.url=xx
spring.datasource.driverClassName=xxx
spring.datasource.datasourceJndiName=xxx
```

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
    return DataSourceBuilder.create().type(cn.bee.dbcp.BeeDataSource.class).build();
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

Performance
---
The performance of each connection pool is tested by multi-threaded query (1000 threads execute 1000 times each, totally 1 million times), and the time-consuming distribution and average time-consuming are printed. Finally, the connection pools are ranked according to the usual time-consuming. Single time statistics (machine status impact on the test results):

[datasource.getConnection(),connection.prepareStatement,statement.execute(),statement.close(),connection.close()]</i>

1：Below are the test results of each connection pool at Mysql5.6 (milliseconds)

Bee_C(5.3623) > Bee_F(6.8492) > HikariCP(9.0176)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191105_JDBCPool_I54300.log">20191105_JDBCPool_I54300.log</a>

project for performance test code,please visit：https://github.com/Chris2018998/PoolPerformance
 
2：Test with HikariCP benchmark

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191221_I3_7100_HikariCP_Bech_Pict.png">20191221_I3_7100_HikariCP_Bech_Pict.png</img>

Download <a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>

