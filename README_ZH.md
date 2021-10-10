<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
<img height="20px" width="20px" align="bottom" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>

<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 <a><img src="https://img.shields.io/github/v/release/Chris2018998/beecp.svg"></a> 
</p> 

## :coffee: 简介

BeeCP是一款高性能JDBC连接池

## :arrow_down: 下载 

Java7或更高
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.2.7</version>
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

## :thumbsup: 优点

1：ThreadLocal连接单缓存，提高池化性能

2：借用者非移动等待，节约队列出入开销

3：传递管道复用，既可以传递连接，也可传递异常

4：双向异步候补，消除等待者与传送者的时间差错位


## :cherries: 比较HikariCP池

|     比较项    |     BeeCP                                                   |      HikariCP                                             |  
| -----------  |----------------------------------------------------------   | ----------------------------------------------------------|          
|关键技术       |ThreadLocal + 信号量 + ConcurrentLinkedQueue +Thread          |FastList + ConcurrentBag + ThreadPoolExecutor              | 
|相似点         |CAS使用，代理预生成，使用驱动自带Statement缓存                    |                                                           |
|差异点         |支持平衡模式，支持XA，强制回收持有不用的连接                       |                                                           |
|文件           |32个源码文件，Jar包93KB                                        |44个源码文件，Jar包158KB                                     | 
|性能           |总体性能高40%以上（光连接池基准）                                |                                                           |

HikariCP有哪些缺陷？

1：<a href="https://my.oschina.net/u/3918073/blog/4645061">MySQL驱动应用,已经关闭的PreparedStatement居然可以复活？</a> 

2：<a href="https://my.oschina.net/u/3918073/blog/5053082">数据库Down机或网络问题，反应迟缓(俗称等你一万年)</a>

3：<a href="https://my.oschina.net/u/3918073/blog/5171229">事务性漏洞问题</a>

.....


## :tractor: 使用

使用方式与一般池大致相似，下面有两个参考例子

###### :point_right: 例子1

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

###### :point_right: 例子2

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

:sunny: *如果项目为Springboot类型，推荐使用数据源管理工具：<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a>（无需代码开发配置即可，且自带监控界面）*


## :book: 配置项

###### :capital_abcd: poolName 

池的名称配置，如果未配置，系统自动生成，格式为：FastPool-x 

###### :1234: fairMode

池支持公平与竞争两种模式，默认为竞争模式；公平模式下，借用者获取连接是先到先得原则

###### :capital_abcd: initialSize

池的初始化时，构建连接数量，如果为0，池默认创建1个

###### :1234: mxActive

池内连接最大活动数，默认值10

###### :capital_abcd: borrowSemaphoreSize

池内信号量大小，默认为CPU核心数

###### :1234: defaultAutoCommit

连接上AutoCommit的属性默认值设置，默认为true

###### :capital_abcd: defaultTransactionIsolationCode

连接的TransactionIsolation事务隔离等级设置，未设置时则默认值则以驱动为准

###### :1234: maxWait

获取连接时，借用者的最大等待时间，时间单位为毫秒，默认值8000

###### :capital_abcd: idleTimeout

连接闲置超时时间，超过则被移除，时间单位为毫秒，默认值18000

###### :1234: holdTimeout

已被借用的连接，若指定时间内未活动（执行SQL），则强制回收，默认值18000

###### :capital_abcd: connectionTestSql

连接活性测试Sql查询语句，建议不要嵌入过程语句，必须提供

###### :1234: connectionTestTimeout

连连接活性测试反应时间范围，时间单位为秒，默认为3秒

###### :capital_abcd: connectionTestInterval

连连接活性测试间隔时间，当前距离上次活动内则假定活动是有效的，默认500毫秒


:point_right: <a href="https://github.com/Chris2018998/BeeCP/wiki/Configuration--List">更多配置项</a>


## :sparkling_heart: 捐助

如果您觉得此作品不错，请捐赠我们喝杯咖啡吧，在此表示感谢^_^。

<img height="50%" width="50%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/donate.png"> 
