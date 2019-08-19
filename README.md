[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
BeeCP is a lightweight (15 files, 2000lines of source code) high-performance Java connection pool

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>

Naming Reasons
---
Bees are a kind of beneficial insects to human beings. In recent years, the number of bees reported in the news is declining, which will directly affect the production of human food. Hope to attract more friends'attention and attention: although bees are small, they play a great role, Protecting the environment and caring for nature.

Release download
---
Download<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.89/BeeCP-0.89.jar">BeeCP_0.89.jar</a>

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>0.89</version>
</dependency>

```

Configuration
---
|  Name           |  Description |   Remark |
| ----------------| ------------ | ------------ |
| initialSize     |Connection pool initial size|   |
| maximumPoolSize |Maximum number of connection pools|    |
| maxWait         |Maximum borrowing waiting time (milliseconds)|   |
| idleTimeout     |connection maximum idleness time(milliseconds)|   |  
| preparedStatementCacheSize |preparedStatement cache Size |   
| validationQuery |Connection active Query Statement |    |   |


Refence demo in SpringBoot
---
```java
application.properties

spring.datasource.username=xx
spring.datasource.password=xx
spring.datasource.jdbcUrl=xx
spring.datasource.driverClassName=xxx
spring.datasource.datasourceJndiName=xxx
```

```java
@Configuration
public class DataSourceConfig {
  @Value("spring.datasource.driverClassName")
  private String driver;
  @Value("spring.datasource.jdbcUrl")
  private String url;
  @Value("spring.datasource.username")
  private String user;
  @Value("spring.datasource.password")
  private String password;
  @Value("spring.datasource.datasourceJndiName")
  private String datasourceJndiName;
  private BeeDataSourceFactory dataSourceFactory = new BeeDataSourceFactory();
  
  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(org.jmin.bee.BeeDataSource.class).build();
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

1：Below are the test results of each connection pool at Oracle11G (in milliseconds)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I5_4210M_Oracle11g_20190717.log">20190808_I5_4210M_Orcale11g.log</a>

Bee_F(16.37) > Bee_C(18.25) > Vibur(28.79) > HikariCP(34.42) > TOMCAT(67.47) > DBCP(75.28) > Druid(75.97) > C3P0(96.40)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I5_4210M_Oracle11g_20190723.log">20190808_I5_4210M_Orcale11g.log</a>

Bee_F(13.39) > Bee_C(15.25) > Vibur(20.64) > HikariCP(28.79) > TOMCAT(57.93) > DBCP(66.47) > Druid(67.03) > C3P0(71.54)


2：Test with HikariCP driver(Dedicated to performance testing),result are following

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I3_7100_HikariCP_Driver_20190729.log">I3_7100_HikariCP_Driver_20190729.log</a>

Bee_F(0.0006) > Bee_C(0.0980) > HikariCP(0.3053) > Vibur(0.3068) > TOMCAT(1.9001) > DBCP(3.9862) > C3P0(6.3528) > Druid(9.7170)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20190808_I5_4210M_HikariCP_Driver.log">20190808_I5_4210M_HikariCP_Driver.log</a>

Bee_C(0.0018) > Vibur(0.0048) > Bee_F(0.1982) > HikariCP(0.3832) > TOMCAT(2.3388) > Druid(3.0775) > DBCP(5.2606) > C3P0(11.9082)


project for performance test code,please visit：https://github.com/Chris2018998/PoolPerformance


Support 
---
Email:Chris2018998@tom.com

<img src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/w.png"> </img>
<img src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/z.png"> </img>

Development collaboration
---
Welcome the netizens who are interested in connection pool to develop and maintain together.
