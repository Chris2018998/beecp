[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

Download<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.84/BeeCP-0.84.jar">BeeCP_0.84.jar</a>

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>0.84</version>
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
<i>Pool statement performance Test(1000Threads x 1000iteration)</i>
<i>[datasource.getConnection(),connection.prepareStatement,statement.execute(),statement.close(),connection.close()]</i>

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I5_4210M_Oracle11g_20190717.log">I5_4210M_Oracle11g_20190717.log</a>

Bee_F(16.37) > Bee_C(18.25) > Vibur(28.79) > HikariCP(34.42) > TOMCAT(67.47) > DBCP(75.28) > Druid(75.97) > C3P0(96.40)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I5_4210M_Oracle11g_20190723.log">I5_4210M_Oracle11g_20190723.log</a>

Bee_F(13.39) > Bee_C(15.25) > Vibur(20.64) > HikariCP(28.79) > TOMCAT(57.93) > DBCP(66.47) > Druid(67.03) > C3P0(71.54)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I5_4460_Marridb10.3_20190723.log">I5_4460_Marridb10.3_20190723.log</a>

Bee_C(8.05) > Bee_F(8.77) > Vibur(9.20) > HikariCP(11.11) > TOMCAT(26.15) > DBCP(29.72) > Druid(29.96) > C3P0(38.92)

I3_7100_HikariCP_Bech_20190701.png
<img src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/I3_7100_HikariCP_Bech_20190701.png"></img>

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/Jdbc_CP_Test.zip">Download performance source </a>


Contact 
---

Email:Chris2018998@tom.com

<img src="https://github.com/Chris2018998/BeeCP/tree/master/doc/individual/w.png"> </img>
<img src="https://github.com/Chris2018998/BeeCP/tree/master/doc/individual/z.png"> </img>

