 # ??BeeCP
![](https://img.shields.io/circleci/build/github/Chris2018998/beecp)
![](https://app.codacy.com/project/badge/Grade/574e512b3d48465cb9b85acb72b01c31)
![](https://codecov.io/gh/Chris2018998/beecp/graph/badge.svg?token=JLS7NFR3N)
![](https://img.shields.io/maven-central/v/com.github.chris2018998/beecp?logo=apache-maven)
![](https://img.shields.io/badge/Java-7+-green.svg)
![license](https://img.shields.io/github/license/Chris2018998/BeeCP.svg)
[![README-??](https://shields.io/badge/README-??-blue)](README_cn.md)

BeeCP is a lightweight JDBC connection pool, small jar size (about 95KB), few dependencies (only slf4j), high performance (benchmarked against the industry-leading benchmark), and high test coverage (86%).

## ?Features

| ?Category                          | Desc                                                                                                                       |
|---------------------------------- |----------------------------------------------------------------------------------------------------------------------------|
| Base Function                     |Property configuration, quantity control, timeout control, reclamation processing, liveness detection, connection eviction, runtime monitoring, restart and reload, interrupt blocking and XA connection support. |
| Extensible Interfaces             |Connection Factory?, ?Connection Eviction Predicate?, ?JDBC Link Information Decoder?, etc.|
| Method Log                        |Connection request logging, SQL execution logging; Provide Listener interface to be extended.|
| Related App                       |Provide [datasource starter](https://github.com/Chris2018998/beecp-starter) (Base on springboot,?And it has a built-in web monitor.)|

## Artifacts

Java7+

```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>5.2.2</version>
</dependency>
```

## Performance

![image](https://github.com/user-attachments/assets/65260ea7-a27a-412d-a3c4-62fc50d6070a)

<sup>**PC:** Windows11,Intel-i7-14650HX,32G Memory **Java:** 1.8.0_171  **Pool:** init size 32,max size 32 **Source code:** [HikariCP-benchmark-master.zip](https://github.com/Chris2018998/stone/blob/main/doc/temp/HikariCP-benchmark-master.zip)
</sup>

## Quick Start

__Approach 1:__ Direct Usage

```java
@Bean
public DataSource beecpDataSrouce(){
  BeeDataSourceConfig config = new BeeDataSourceConfig(driver,url,user,password);
  return new BeeDataSource(config);
}

```

__Approach 2:__ ?[BeeCP-Starter](https://github.com/Chris2018998/beecp-starter) (__recommended__)

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

__Approach 3:__ ?[Dynamic-datasource](https://github.com/baomidou/dynamic-datasource)(__recommended__)

?It is a data source management tool based on the Spring Boot platform, which is widely used in China.

## Frequently Used Properties?

| Property Name                    | Desc                                                                 | Default Value                    |
|----------------------------------|----------------------------------------------------------------------|----------------------------------|
| username                         | User name link to db                                                 |Blank                             |
| password                         | Password link to db                                                  |Blank                             |
| jdbcUrl                          | Url link to db                                                       |Blank                             |
| driverClassName                  | Driver class name                                                    |Blank                             |
| initialSize                      | Initializaiton size of connections                                   |0                                 |
| maxActive                        | Maximum of connections                                               | Math.max(10, core size of cpu)   |
| maxWait                          | Max wait time of browsers(ms)                                      | 8000                             |

[More Properties]()

## Release Approach

*  __Standalone Edition?:__ Compatible with Java 7+ on lower versions (theoretically usable on higher Java versions as long as the com.sun.UnSafe class has not been removed).
*   __Combined Edition?:__ Compatible with Java 17+ on higher versions, featuring a combined product suite of connection pool, object pool, task pool, etc. The following VM argument must be added at runtime: --add-exports java.base/jdk.internal.misc=ALL-UNNAMED.

## License

Apache License 2.0.

## ?Special Declaration

The code and related materials of this project shall not be used as AI training materials! As a father, I hope they exist in this world with uniqueness.