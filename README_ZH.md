<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|
<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)
<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
</p> 

## 一：简介

小蜜蜂连接池（BeeCP）是一款轻量级JDBC连接池组件，它是<a href="https://www.oschina.net">中国开源社区</a>的优质开源作品之一，具有性能高，代码轻，稳定好的特点。
*   Java语言开发，具有跨平台的优点
*   基于参数驱动，支持多种参数设置， 支持配置文件导入
*   适用多种数据库驱动（截止当前，主流数据库均可适配）
*   支持本地事务与分布式事务<br/>
*   产品采用JUC技术开发，具有单点缓存，信号量控制，队列复用，非移动等待，自旋控制， 连接和异常的传递，异步候补，安全关闭等亮点
*   提供日志输出和监控工具
*   健壮性好，敏捷应对意外情况（如断网，数据库服务崩溃）
*   良好的接口扩展性

## 二：版本下载 

### Java7或更高

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.3</version>
</dependency>
```

### Java6

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.10</version>
</dependency>
```                                 
## 三：参考例子

### :point_right: 例子1(独立应用)

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
### :point_right: 例子2(Springbooot)

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

:sunny: *如果项目为Springboot类型，推荐使用数据源管理工具：<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a>（无需代码开发配置即可，且自带监控界面）*

## 四：功能导向

![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)

## 五：运行时监控

为了更好的监控池内的运行情况（*闲置连接数，使用中连接数，等待数等*），产品内部提供了三种方式
*   基于slf4j日志接口输出池内运行时信息
*   提供Jmx方式监控
*   提供方法级监控（可访问数据源的监控方法，得到一个可反映池内状态的Vo结果对象）

除以上方式，我们额外准备一套具有监控界面的解决方案：<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a>

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)

![图片](https://user-images.githubusercontent.com/32663325/154832193-62b71ade-84cc-41db-894f-9b012995d619.png)

## 六：对比其他池

### 1: 性能对比

![图片](https://user-images.githubusercontent.com/32663325/154832468-7659201e-be49-4bd8-802d-50f6aa3b95d8.png)

<sup>**机器**：Win7_64 I3-7100 8G   **池设置**：初始0，最大32  **测试基准**：<a href="https://github.com/brettwooldridge/HikariCP-benchmark">光连接池提供</a> （*光连接池被号称为史上最快的连接池*）</sup>

### 2: 技术对比（光连接池）

| **比较项**                      |**BeeCP**                                          | **HikariCP**                                      |
|---------------------------------|---------------------------------------------------| ------------------------------------------------- |
| 关键技术                         |ThreadLocal，信号量，ConcurrentLinkedQueue，Thread   | FastList，ConcurrentBag，ThreadPoolExecutor       |
| 相似点                           |CAS使用，代理预生成，使用驱动自带Statement缓存          |                                                  |
| 差异点                           |支持公平模式，支持XA分布事务，强制回收持有不用的连接，单点缓存，队列复用，非移动等待，独创自旋控制/连接传递程序片段|支持池暂停|
| 文件                             |37个源码文件，Jar包95KB                              |44个源码文件，Jar包158KB                                   |
| 性能                             |总体性能高40%以上（光连接池基准）                      |                                                         |

### 3: 启发池介绍

**德鲁伊（<a href="https://github.com/alibaba/druid">druid</a>）**：作者名为：**温绍锦**，一名任职中国阿里的技术专家，其作品最早发布于2012年，专为监控设计（尽管内部含有大量监控属性， 但在性能方面仍有不俗表现）,在中国有超多用户，点赞数超过25k。

**光连接池（<a href="https://www.github.com/brettwooldridge/HikariCP">HikariCP</a>）**：作者名为：**Brett Wooldridge**， 一位现居日本的美国Java专家，其作品最早发布于2014年，是高性能连接池的典型代表，已经被Java领域广泛使用。

BeeCP中部分灵感受启发于它们，感谢两位大师的贡献。

## 七：代码质量

![图片](https://user-images.githubusercontent.com/32663325/160231968-3d9a29e0-ab1a-4358-8f83-4652c51d644b.png)

## 八：扩展接口

### 1：连接工厂接口

产品内部提供两个工厂接口分别用来创建本地连接和Xa连接(**一般不建议自扩展**)
 
![图片](https://user-images.githubusercontent.com/32663325/153597017-2f3ba479-8f3f-4a82-949b-275068c287cd.png)
 
数据源配置类(BeeDataSourceConfig)中有一个工厂类名配置项，支持4种类型

![图片](https://user-images.githubusercontent.com/32663325/153597130-a22c0d92-2899-46db-b982-35b998434eae.png)
 
参考例子

![图片](https://user-images.githubusercontent.com/32663325/153597143-3a8e45f8-4894-4e98-913d-63994d3486c6.png)

### 2：连接密文解密

如果连接数据库使用的是密文，产品内部提供一个可供扩展的解密类，使用时将实现类名注入配置中即可。

![图片](https://user-images.githubusercontent.com/32663325/153597176-e48382b9-7395-4c6c-9f34-425072d7c510.png)

## 九：配置项

|项名                              |描述                                   |默认值                               |
| ---------------------------------| -------------------------------------| -----------------------------------|
|username                          |JDBC用户名                             |空                                  |
|password                          |JDBC密码                               |空                                  |
|jdbcUrl                           |JDBC连接URL                            |空                                  |
|driverClassName                   |JDBC驱动类名                            |空                                  |
|poolName	                   |池名，如果未赋值则会自动产生一个                 |空                                  |
|fairMode                          |是否使用公平模式                         |false（竞争模式）                     | 
|initialSize                       |连接池初始大小                           |0                                   |
|maxActive                         |连接池最大个数                           |10                                  | 
|borrowSemaphoreSize               |信号量许可大小                           |min(最大连接数/2,CPU核心数）           |
|defaultAutoCommit                 |AutoComit默认值,未配置则从第一个连接上读取默认值|空                               |
|defaultTransactionIsolationCode   |事物隔离代码，未设置时则从第一个连接上读取默认值|空                                |
|defaultCatalog                    |Catalog默认值 ,未配置则从第一个连接上读取默认值|空                                |
|defaultSchema                     |Schema默认值,未配置则从第一个连接上读取默认值|空                                  |
|defaultReadOnly                   |ReadOnly默认值 ,未配置则从第一个连接上读取默认值|空                               |
|maxWait                           |连接借用等待最大时间(毫秒)                |8000                                |
|idleTimeout                       |连接闲置最大时间(毫秒)                    |18000                               |  
|holdTimeout                       |连接被持有不用最大允许时间(毫秒)           |18000                               |  
|validTestSql                      |连接有效性测试SQL语句                     |SELECT 1                            |  
|validTestTimeout                  |连接有效性测试超时时间(秒)                 |3                                   |  
|validAssumeTime                   |连接测试的间隔时间(毫秒)                   |500                                 |  
|forceCloseUsingOnClear            |是否直接关闭使用中连接                     |false                               |
|delayTimeForNextClear             |延迟清理的时候时间（毫秒）                 |3000                                |                   
|timerCheckInterval                |闲置扫描线程间隔时间(毫秒)                 |18000                               |
|connectionFactoryClassName        |自定义的JDBC连接工作类名                   |空                                  |
|enableJmx                         |JMX监控支持开关                           |false                               | 
|printConfigInfo                   |是否打印配置信息                           |false                               | 
|printRuntimeLog                   |是否打印运行时日志                         |false                               | 
