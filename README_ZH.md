[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

一：介绍 <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
小蜜蜂池：一款高性能JDBC连接池

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>

二：特点
---
1：性能好：比<a href="https://github.com/brettwooldridge/HikariCP">光连接池</a>更快 

2：代码少：21个文件，2600行源码

三：性能测试
---
**1：** 100万次借用测试(1000线程 x 1000次),单次计时间范围:[datasource.getConnection(),connection.close()] 结果如下

| 时间(ms)    |   HikariC3.3.1  |  Bee_F(BeeCP-2.3.2)| Bee_C(BeeCP-2.3.2)    |
| ----------- |----------------| -------------------| ----------------------| 
| 总时间      | 151516          | 53384              |          142          | 
| 平均时间    | 0.1515          | 0.0534             |        0.0001         ||  

说明：

Bee_F:公平模式池，Bee_C:竞争模式池

总时间=线程1耗时 + 线程2耗时 + ...... + 线程1000耗时, 平均时间 = 总时间/1000000

测试电脑: Win7 I3-7100 8G mysql5.6.46_64  连接池设置: init size10 max size:10

每次测试重新启动数据库,日志文件下载：<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200417_JDBCPool_I37100.log">20200417_JDBCPool_I37100.log</a>
 
性能测试代码请访问项目：https://github.com/Chris2018998/PoolPerformance

**2：** 采用光连接池的性能基准测试结果(I3-7100,8G)

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20200315_I3_7100_HikariCP_Bech_Pict.png"></img> 下载性能测试源码：<a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>

四：参考Demo（SpringBoot）
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


五：版本下载
---
**Java7**
```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>2.4.0</version>
</dependency>
```

**Java6**
```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.6.7</version>
</dependency>

```

*友情提示：建议使用最新版本*


六：功能支持
---
```java
1：请求超时支持

2：两种模式：公平与竞争

3：断网连接池自动恢复

4：闲置超时和持有超时处理

5：PreparedStatement缓存支持（也可不用）

6：支持连接回收前，事物回滚

7：支持连接回收前，属性重置（比如：autoCommit,transactionIsolation,readonly,catlog,schema,networkTimeout）

8: 支持JMX

9：支持连接工厂自定义
```

七：配置项说明
---
|  配置项          |   描述                        |   备注                            |
| ----------------| ---------------------------  | ------------------------          |
| username        | JDBC用户名                    |                                   |
| password        | JDBC密码                      |                                   |
| jdbcUrl         | JDBC连接URL                   |                                   |
| driverClassName | JDBC驱动类名                   |                                   |
| poolName        | 连接池名                       |                                   |
| fairMode        | 连接池是否公平模式               | 公平锁,等待者优先获取连接            |
| initialSize     | 连接池初始大小                  |                                   |
| maxActive       | 连接池最大个数                  |                                   | 
| concurrentSize  | 请求并发数（借用者线程数）        | 不允许大于连接最大数                 |
| preparedStatementCacheSize |SQL宣言缓存大小       | 0 表示不适用缓存                    |
| defaultAutoCommit|连接是否为自动提交              | 默认true                            |
| defaultTransactionIsolation|事物等级             | 默认读提交，Connection.TRANSACTION_READ_COMMITTED |
| defaultCatalog    |                             |                                     |
| defaultSchema     |                             |                                     |
| defaultReadOnly   |                             | 默认false                            |
| maxWait           |连接借用等待最大时间(毫秒)       | 默认8秒，连接请求最大等待时间           |
| idleTimeout       |连接闲置最大时间(毫秒)          | 默认3分钟，超时会被清理                 |  
| holdIdleTimeout   |连接被持有不用的最大时间(毫秒)    | 默认5分钟，超时会被清理                 |  
| connectionTestSQL |连接有效性测试SQL语句           | 一条 select 语句，不建议放入存储过程     |  
| connectionTestTimeout |连接有效性测试超时时间(秒)   |默认5秒 执行查询测试语句时间，在指定时间范围内等待反应|  
| connectionTestInterval |连接测试的间隔时间(毫秒)     |默认500毫秒 连接上次活动时间点与当前时间时间差值小于它，则假定连接是有效的|  
| forceCloseConnection   |是否需要暴力关闭连接         |默认false;true:直接关闭使用中连接，false:等待处于使用中归还后再关闭|
| waitTimeToClearPool    |延迟清理的时候时间（秒）      |默认3秒，非暴力清理池下，还存在使用中的连接，延迟等待时间再清理|                   | idleCheckTimeInterval  |闲置扫描线程间隔时间(毫秒)             |                     |
| idleCheckTimeInitDelay |闲置扫描线程延迟时间再执行第一次扫描(毫秒)|                    |
| connectionFactoryClassName|自定义的JDBC连接工作类名            | 默认为空             |
| enableJMX                 |JMX监控支持开关                    |                    | |






