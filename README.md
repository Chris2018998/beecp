[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

Download<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.76/BeeCP-0.76.jar">BeeCP_0.75.jar</a>

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>0.76</version>
</dependency>

```

Configuration
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
| poolInitSize  | connection size need create when pool initialization  |   |
| poolMaxSize |  max connnection size in pool |    |
| borrowerMaxWaitTime |request timeout for borrower(ms)  |   |
| preparedStatementCacheSize | stement cache size |   |
| connectionIdleTimeout  | max idle time,then will be close(ms)  |    |
| validationQuerySQL |  a test sql to check connection ative   |    |   |

DataSource Demo
---
```java
String userId="root";
String password="";
String driver="com.mysql.jdbc.Driver";
String URL="jdbc:mysql://localhost/test";
BeeDataSourceConfig config = new BeeDataSourceConfig(driver,URL,userId,password);
DataSource datasource = new BeeDataSource(config);
Connection con = datasource.getConnection();
....................
```

Performace test
---

computer Env

|  Name        |  Description | 
| ------------ | ------------ | 
|  OS          | Win7_64      |   
| CPU          | I3-7100(3.9hz*2) |  
| Memory       | 8G           |   
| Java         |JDK1.8.0_192  |  
|  DB          | mariadb10.3  |  
| JDBC Driver  | mariadb-java-client-2.4.1  |   |  

Pool setting 

|  Name                |  value     | 
| ------------         | -----------| 
| init size            | 0          |   
| max size             | 10         |  
| statement cache size | 10         | 
| max wait time        | 30 seconds |  |  

---
1: JMH Test with <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/HikariCP-jdbcBech.zip">HikariCP Benchmarks source code</a> 

![Image text](https://github.com/Chris2018998/BeeCP/blob/master/doc/HikariCP-jdbcBech.png)

2: Concurrent(1000threads x 1000iteration)Test <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/Jdbc-Performace.zip">Source code</a> 

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/JDBCPool.log">Download the log file</a> 

