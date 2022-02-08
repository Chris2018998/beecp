<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
![图片](https://user-images.githubusercontent.com/32663325/153004295-35c77bdf-c857-4486-8a80-272ef608c0ed.png)

<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 <a><img src="https://img.shields.io/github/v/release/Chris2018998/beecp.svg"></a>  
</p> 


## 一：产品简介
小蜜蜂连接池（BeeCP）是一款利用Java语言(计算机语言)开发的基础性组件, 用来帮助J2EE应用统一管理数据库的桥接对象（Connection），达到连接资源重用和提高系统性能的目标。 

## 二：技术特点
BeeCP是作者多年的倾心之作，它是<a href="https://www.oschina.net">中国开源社区</a>中高质量代表性开源作品，具有性能高，代码轻，稳定好的特点。

1：Java语言开发，具有跨平台的优点

2：基于参数驱动，支持多种参数设置， 支持配置文件导入

3：适用多种数据库驱动（截止当前，主流数据库均可适配）

4：支持本地事务与分布式事务

5：产品采用JUC技术开发，具有单点缓存，信号量控制，队列复用，非移动等待，自旋控制， 连接传递，异步候补，安全关闭等亮点

6：健壮性好，敏捷应对意外情况（如断网，数据库服务崩溃）

7：良好的接口扩展性 

## 三：角色设计
BeeCP定位：一款管理数据库连接的容器产品，其原理与图书馆类似，以基础组件的角色可存在于各类数据库应用中，是J2EE应用通向数据库的纽带，从驱动者角度来看，产品设想主要有两类角色：1：使用数据库连接的各类应用产品  2：池内管理员，管控内部一切活动。

![图片](https://user-images.githubusercontent.com/32663325/153000242-33211226-ce18-4dca-8487-e5ac1c7cac67.png)


。。。。。。待续






## :arrow_down: 下载 

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
