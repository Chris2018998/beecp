[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

Download<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.78/BeeCP-0.78.jar">BeeCP_0.78.jar</a>

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>0.78</version>
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

Performace test(2019-05-25)
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
Concurrent(1000threads x 1000iteration)Test <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/Jdbc-Performace.zip">Source code</a> 

A: Borrow Result

|  pool name          |  min time(ms)    |  max time(ms)     | avg time(ms)  | 
| ------------        | ------------     | ------------      | ------------  | 
| Tomcat-JDBC-9.0.19  |  0               | 272               |   2.7284      | 
| Aili-Druid-1.1.16   |  0               | 903               |   22.3774     | 
| Vibur-22.2          |  3               | 225               |   4.3447      | 
| HikariCP-3.3.1      |  0               | 348               |   0.3020      | 
| BeeCP_Fair-0.78     |  0               | 256               |   0.2659      | 
| BeeCP_Compete-0.78  |  0               | 304               |   0.2597      | 

B: Query Result

|  pool name          |  min time(ms)    | max time(ms)      | avg time(ms)  | 
| ------------        | ------------     | ------------      | ------------  | 
| Tomcat-JDBC-9.0.19  |  0               | 465               |   46.43       | 
| Aili-Druid-1.1.16   |  0               | 1301              |   51.03       | 
| Vibur-22.2          |  12              | 373               |   33.28       | 
| HikariCP-3.3.1      |  0               | 29987             |   22.66       | 
| BeeCP_Fair-0.78     |  0               | 2092              |   23.00       | 
| BeeCP_Compete-0.78  |  0               | 20783             |   18.24       | 

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/JDBCPool.log">Download the log file</a> 

