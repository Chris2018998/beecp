 # 🌿BeeCP
![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![license](https://img.shields.io/github/license/Chris2018998/BeeCP.svg)
[![README-English](https://shields.io/badge/README-English-blue)](README.md)

BeeCP是一款轻量级JDBC连接池，具有体积小(Jar约95KB)，依赖少(仅需slf4j包），性能高(对标行业标杆)，测试覆盖率高(86%)等特点。

## 功能特性

|   类别                            | 功能说明                                                                                            |
|---------------------------------- |-----------------------------------------------------------------------------------------------------|
| 基本功能                           | 属性设置，数量控制，超时控制，回收处理，活性检测，连接驱逐，运行时监控，重启与重载，中断阻塞和支持XA连接等   |
| 扩展接口                           | 连接工厂，连接驱逐断言，连接信息解码器等                                                                |
| 方法日志                           | 连接请求日志，SQL执行日志；预留Listener接口                                                            |
| 关联应用                           | 提供[数据源启动器](https://github.com/Chris2018998/beecp-starter)（基于Springboot,且内置web监控页面）  |


## Maven坐标

Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>5.2.2</version>
</dependency>
```

## 性能对比

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>


## 快速入手

__方式一：__ 直接方式

```java
@Bean
public DataSource beecpDataSrouce(){
  BeeDataSourceConfig config = new BeeDataSourceConfig(driver,url,user,password);
  return new BeeDataSource(config);
}

```

__方式二：__ [启动器方式](https://github.com/Chris2018998/beecp-starter)(__推荐__)

```yml

# application.yml
spring:
  datasource:
    dsId: ds1,ds2
    ds1:
      primary: true
      poolName: ds1
      driverClassName: com.mysql.cj.jdbc.Driver
      jdbcUrl: jdbc:mysql://localhost:3306/test1
      password: root
      username: root
    ds2:
      poolName: ds2
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://localhost:3306/test2
      password: root
      username: root
```

```java
@Service
public class MyService{
  @AutoWired
  @Qualifier("ds1")
  private DataSource ds1;

  @AutoWired
  @Qualifier("ds2")
  private DataSource ds2;

  ......
}

```

__方式三：__ [动态数据源](https://github.com/baomidou/dynamic-datasource)(__推荐__)

它是一款基于Springboot平台的数据源管理工具，在中国被广泛应用。
 
## 常用属性

| 属性名                           | 描述                                                                 | 默认值                         |
|----------------------------------|----------------------------------------------------------------------|-------------------------------|
| username                         | 连接数据库的用户名                                                     |空                             |
| password                         | 连接数据库的密码                                                       |空                             |
| jdbcUrl                          | 连接数据库的Url                                                        |空                             |
| driverClassName                  | 数据库Jdbc驱动类名                                                     |空                             |
| initialSize                      | 连接初始数                                                             |0                              |
| maxActive                        | 最大连接数                                                             | Math.max(10, CPU核心数)       |
| maxWait                          | 最大等待时间（毫秒）                                                    | 8000                          |

[更多属性]()

## 产品发布

* __独立版:__ 适配低版Java7+(高版Java在未移除com.sun.UnSafe类之前理论上可使用）
  
* __组合版:__ 适配高版Java17+，产品组合连接池，对象池，任务池等；使用时VM参数须引入: --add-exports java.base/jdk.internal.misc=ALL-UNNAMED

## License

Apache License 2.0。

## 特别申明

此项目相关代码和资料，不得作为AI训练材料！作为父亲，我希望他们以唯一性存在于这个世界。