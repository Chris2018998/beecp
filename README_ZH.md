<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
![图片](https://user-images.githubusercontent.com/32663325/153004295-35c77bdf-c857-4486-8a80-272ef608c0ed.png)
<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
</p> 

## 一：简介
小蜜蜂连接池（BeeCP）是一款轻量级JDBC连接池，它是<a href="https://www.oschina.net">中国开源社区</a>中高质量开源作品之一，具有性能高，代码轻，稳定好的特点。

1：Java语言开发，具有跨平台的优点

2：基于参数驱动，支持多种参数设置， 支持配置文件导入

3：适用多种数据库驱动（截止当前，主流数据库均可适配）

4：支持本地事务与分布式事务

5：产品采用JUC技术开发，具有单点缓存，信号量控制，队列复用，非移动等待，自旋控制， 连接和异常的传递，异步候补，安全关闭等亮点

6：提供日志输出和监控工具

7：健壮性好，敏捷应对意外情况（如断网，数据库服务崩溃）

8：良好的接口扩展性 

 
## 二：功能图

![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)
 
                                    
## :tractor: 三：使用

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

## :arrow_down: 四：下载 

Java7或更高
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.1</version>
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

## 五：扩展接口

###### 1：连接工厂接口
对于连接的构造，产品提供了两个工厂接口，可供外部扩展，如下：
 
![图片](https://user-images.githubusercontent.com/32663325/153597017-2f3ba479-8f3f-4a82-949b-275068c287cd.png)
 
数据源配置类(BeeDataSourceConfig)中有一个工厂类名配置项，支持4种类型

![图片](https://user-images.githubusercontent.com/32663325/153597130-a22c0d92-2899-46db-b982-35b998434eae.png)
 
参考例子

![图片](https://user-images.githubusercontent.com/32663325/153597143-3a8e45f8-4894-4e98-913d-63994d3486c6.png)


###### 2：连接密文解密
JDBC驱动连接数据库时，通常会需要使用密码，但是密码以明文的形式存在文件或数据库中，可能存在泄露的风险，BeeCP产品给出一个解决方案：将连接密码进行加密处理，使用时再实时对密文进行解密。产品内部提供了一个密文解密工具类，用户扩展该类，实现解密过程，使用时将实现类名注入配置中。
 

![图片](https://user-images.githubusercontent.com/32663325/153597176-e48382b9-7395-4c6c-9f34-425072d7c510.png)


## :book:六：配置项

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

## :sparkling_heart: 捐助

如果您觉得此作品不错，请捐赠我们喝杯咖啡吧，在此表示感谢^_^。

![图片](https://user-images.githubusercontent.com/32663325/153004175-cb7dd622-03f8-47ae-a454-15d5fc82aebb.png)
