<img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img> <a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>

小蜜蜂连接池：一款轻量高性能JDBC连接池

Maven坐标(Java7)
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.0.6</version>
</dependency>
```
Maven坐标(Java6)
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.8</version>
</dependency>
```
---

##### 性能测试

**1：** 100万次借用/归还(1000线程x1000次),获取时间分布,平均时间
|   连接池名      |   HikariC3.3.1  | BeeCP-2.3.2_Fair  | BeeCP-2.3.2_compete   |
| ---------------|---------------- | ----------------- | ----------------------| 
| 总时间(毫秒)    | 151516          | 53384             |          142          | 
| 平均时间(毫秒)  | 0.1515          | 0.0534            |        0.0001         |

测试配置：PC:I5-4210M(2.6赫兹，双核4线程),12G内存 Java:JAVA8_64 Pool:初始10,最大10

日志文件：<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200417_JDBCPool_I37100.log">20200417_JDBCPool_I37100.log</a>
 
测试源码：https://github.com/Chris2018998/PoolPerformance

**2：** 使用光连接池基准测试结果(I3-7100,8G)

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/PoolPerformaceCompare.png"></img> 

测试源码：<a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>

---

##### 范例1

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

###### 范例2（SpringBoot）

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

###### 功能与特性

1：请求超时支持

2：两种模式：公平与竞争

3：支持连接安全关闭

4：断网连接池自动恢复

5：闲置超时和持有超时处理

6：支持连接回收前，事物回滚

7：若连接出现问题，池自动补充

8：支持连接回收前，属性重置（比如：autoCommit,transactionIsolation,readonly,catlog,schema,networkTimeout）

9：支持XADataSource

10：支持连接工厂自定义

11：支持连接池重置

12：支持JMX

---
###### 配置项说明

|  配置项          |   描述                        |   备注                            |
| ----------------| ---------------------------  | ------------------------          |
| username        | JDBC用户名                    |                                   |
| password        | JDBC密码                      |                                   |
| jdbcUrl         | JDBC连接URL                   |                                   |
| driverClassName | JDBC驱动类名                   |                                   |
| poolName	      |池名                            |如果未赋值则会自动产生一个            |
| fairMode        | 连接池是否公平模式               | 默认false,竞争模式                 | 
| initialSize     | 连接池初始大小                  |                                   |
| maxActive       | 连接池最大个数                  |                                   | 
| borrowSemaphoreSize  | 信号量请求并发数（借用者线程数）| 不允许大于连接最大数                 |
| defaultAutoCommit|连接是否为自动提交              | 默认true                            |
| defaultTransactionIsolation|事物等级             | 默认读提交，Connection.TRANSACTION_READ_COMMITTED |
| defaultCatalog    |                             |                                     |
| defaultSchema     |                             |                                     |
| defaultReadOnly   |                             | 默认false                            |
| maxWait           |连接借用等待最大时间(毫秒)       | 默认8秒，连接请求最大等待时间           |
| idleTimeout       |连接闲置最大时间(毫秒)          | 默认3分钟，超时会被清理                 |  
| holdTimeout       |连接被持有不用的最大时间(毫秒)    | 默认5分钟，超时会被清理                 |  
| connectionTestSQL |连接有效性测试SQL语句           | 一条 select 语句，不建议放入存储过程     |  
| connectionTestTimeout |连接有效性测试超时时间(秒)   |默认5秒 执行查询测试语句时间，在指定时间范围内等待反应|  
| connectionTestInterval |连接测试的间隔时间(毫秒)     |默认500毫秒 连接上次活动时间点与当前时间时间差值小于它，则假定连接是有效的|  
| forceCloseConnection   |是否需要暴力关闭连接         |默认false;true:直接关闭使用中连接，false:等待处于使用中归还后再关闭|
| waitTimeToClearPool    |延迟清理的时候时间（秒）      |默认3秒，非暴力清理池下，还存在使用中的连接，延迟等待时间再清理|                   
| idleCheckTimeInterval  |闲置扫描线程间隔时间(毫秒)     |   默认5分钟                                 |
| idleCheckTimeInitDelay |闲置扫描线程延迟时间再执行第一次扫描(毫秒)|    默认1秒                |
| connectionFactoryClassName|自定义的JDBC连接工作类名            | 默认为空                  |
| enableJMX                 |JMX监控支持开关                    | 默认false                | 


---
###### 数据库与驱动信息
|  数据库          |   驱动类名                     |   参考url                       |
| ----------------| ---------------------------   | ------------------------       |
|Mariadb         |org.mariadb.jdbc.Driver   				  |  jdbc:mariadb://localhost/test  |
|MySQL            |om.mysql.jdbc.Driver            |  jdbc:mysql://localhost/test    |
|Oracle          |oracle.jdbc.driver.OracleDriver |  jdbc:oracle:thin:@localhost:1521:orcl|
|MSSQL           |com.microsoft.sqlserver.jdbc.SQLServerDriver | jdbc:sqlserver://localhost:1433;databaseName=test|
|Postgresql      |org.postgresql.Driver                 |  jdbc:postgresql://localhost:5432/postgres|
 
