[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.72/BeeCP-0.75.jar">Download beeCP_0.75.jar</a>

```java
<dependency>
			<groupId>com.github.chris2018998</groupId>
			<artifactId>BeeCP</artifactId>
			<version>0.75</version>
</dependency>
....................
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

A: Borrow Result

|  pool name          |  min time(ms)    |  max time(ms) | avg time(ms)  | 
| ------------        | ------------     | ------------      | ------------  | 
| Tomcat-JDBC-9.0.19  |  0               | 323               |   2.7951      | 
| Aili-Druid-1.1.16   |  0               | 827               |   22.2587     | 
| Vibur-22.2.         |  3               | 235               |   4.4147      | 
| HikariCP-3.3.1      |  0               | 335               |   0.5305      | 
| BeeCP_Fair-0.75     |  0               | 229               |   1.9102      | 
| BeeCP_Compete-0.75  |  0               | 349               |   0.2792      | 

B: Query Result

|  pool name          |  min time(ms)    | max time(ms)      | avg time(ms)  | 
| ------------        | ------------     | ------------      | ------------  | 
| Tomcat-JDBC-9.0.19  |  0               | 453               |   40.80       | 
| Aili-Druid-1.1.16   |  0               | 1465              |   45.79       | 
| Vibur-22.2.         |  3               | 373               |   27.10       | 
| HikariCP-3.3.1      |  0               | 22634             |   18.81       | 
| BeeCP_Fair-0.75     |  0               | 276               |   26.23       | 
| BeeCP_Compete-0.75  |  0               | 1795              |   18.10       | 

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/JDBCPool.log">Download the log file</a> 

