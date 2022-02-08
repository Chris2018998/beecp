<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>|<a href="https://github.com/Chris2018998/BeeCP/blob/master/README_ZH.md">中文</a>
<img height="20px" width="20px" align="bottom" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>

<p align="left">
 <a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
 <a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
 <a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 <a><img src="https://img.shields.io/github/v/release/Chris2018998/beecp.svg"></a>  
</p> 


## 一：产品简介
小蜜蜂连接池（BeeCP）是一款利用Java语言(计算机语言)开发的基础性组件, 用来帮助J2EE应用统一管理数据库的桥接对象（Connection），达到连接资源重用和提高系统性能的目标。 

## 二：技术特点
BeeCP是作者多年的倾心之作，它是中国开源社区中高质量代表性开源作品，具有性能高，代码轻，稳定好的特点。

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

###### :capital_abcd: validTestSql

连接活性测试Sql查询语句，建议不要嵌入过程语句，必须提供

###### :1234: validTestTimeout

连连接活性测试反应时间范围，时间单位为秒，默认为3秒

###### :capital_abcd: validAssumeTime

连连接活性测试间隔时间，当前距离上次活动内则假定活动是有效的，默认500毫秒


:point_right: <a href="https://github.com/Chris2018998/BeeCP/wiki/Configuration--List">更多配置项</a>


## :sparkling_heart: 捐助

如果您觉得此作品不错，请捐赠我们喝杯咖啡吧，在此表示感谢^_^。

<img height="50%" width="50%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/donate.png"> 
