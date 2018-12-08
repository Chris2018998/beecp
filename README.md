[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.67/BeeCP-0.67.jar">Download beeCP_0.67.jar</a>

Configuration
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
|  poolInitSize  | connection size need create when pool initialization  |   |
|  poolMaxSize |  max connnection size in pool |    |
|  borrowerMaxWaitTime |request timeout for  borrower  |   |
|  preparedStatementCacheSize | stement cache size |   |
| connectionIdleTimeout  | max idle time,then will be close  |    |
| connectionValidateSQL |  a test sql to check connection ative   |    |   |

 DataSource Demo
---
```java
String userId="root";
String password="";
String driver="com.mysql.jdbc.Driver";
String URL="jdbc:mysql://localhost/test";
BeeDataSourceConfig config = new JdbcPoolConfig(driver,URL,userId,password);
DataSource datasource = new BeeDataSource(config);
Connection con = datasource.getConnection();
....................
```

Performace test
---
Oe million cycle test for popular connection pools in mutil-thread Concurrent

1: take connection from pool.

    *getConenction(),con.close()*
	
2: take conneciton and execute query.

    *getConenction(),con.preparedStetment(),statement.execute(),statement.close(),con.close()*


|  Env |   value |   Remark|
| ------------ | ------------ | ------------ |
|  CPU | I3-7100(3.9HZ x 2)  | dual core  |
|  Memory |  8G |   |
| JDK  |  OpenJdk8-192 | not optimize  |
|  Datase | mysql5.6-64  | not optimize  |
|  JDBC Driver | Connector/J 5.1.47  |   | |

**Connection pool for comparison**

|  Pool Name  |   Version |   Remark|
| ------------ | ------------ | ------------ |
|  HikariCP|3.2.0 |  '光’ means the fastest  |
|  c3p0 |  0.9.5.2 |   |
| dbcp  |  1.4 |   |
|  Tomcat-JDBC |9.0.13 |   |
|  Druid | 1.1.12  | Alibaba product from china     |
|  vibur-dbcp |22.2 |   | |

**Test Pool paramters settting**

|  Parameter Name  |   Value |   Remark|
| ------------ | ------------ | ------------ |
|  pool init size | 0 |  |
|  pool max size |10 |   |
| request timetou(ms)  |  40000 |    |
|  statement cache size |20 |    |  |

**connection cycle test result(1000thread x 1000 cycle )**

| summary count         | c3p0   | dbcp   |Tomcat-JDBC|Druid|vibur-dbcp|HikariCP|BeeCP-Fair|BeeCP-Compete|
| ------                | -------| ------ |------ | ------| -----|------| ---- | -----|
|time==0ms              |163691  |3543    |1      |366527 |0     |987752|0     |993779|
|0ms<time<=10ms         |149938  |5958    |0      |8004   |995231|7622  |996487|1937  |
|10ms<time<=30ms        |273747  |952213  |988077 |375141 |3384  |1813  |2513  |1448  |
|30ms<time<=50ms        |159117  |36972   |6639   |142388 |385   |342   |0     |620   |
|50ms<time<=100ms       |171698  |36      |4243   |89574  |0     |1344  |0     |805   |
|100ms<time<=200ms      |70060   |16      |40     |15591  |0     |759   |0     |417   |
|200ms<time<=500ms      |10473   |165     |1000   |2291   |1000  |368   |1000  |994   |
|500ms<time<=1000ms     |1014    |550     |0      |484    |0     |0     |0     |0     |
|1000ms<time<=2000ms    |262     |547     |0      |0      |0     |0     |0     |0     |
|2000ms<time            |0       |0       |0      |0      |0     |0     |0     |0     |
|fail                   |0       |0       |0      |0      |0     |0     |0     |0     |
|avg(ms)                |37.68   |26.03   |22.09  |25.34  |4.33  |0.36  |1.33  |0.47  |
|min(ms)                |4       |1       |21     |16     |3     |0     |1     |0     |
|max(ms)                |1772    |1306    |422    |962    |301   |391   |283   |412   | |


**Statement cycle test result(1000thread x 1000 cycle )**

| summary count         | c3p0   | dbcp   |Tomcat-JDBC|Druid|vibur-dbcp|HikariCP|BeeCP-Fair|BeeCP-Compete|
| ------                | -------| ------ |------ | ------| -----|------| ---- | -----|
|time==0ms              |135629  |118     |2      |    |0     |0   |0      |0|
|0ms<time<=10ms         |75720   |1829    |1      |    |0     |0   |0      |
|10ms<time<=30ms        |184455  |1636    |22     |    |0     |0   |0      |0  |
|30ms<time<=50ms        |127556  |6739    |11     |    |0     |0   |0     |0   |
|50ms<time<=100ms       |208443  |986897  |994134 |    |0     |0   |0     |0   |
|100ms<time<=200ms      |180096  |1633    |4554   |    |0     |0   |0     |0   |
|200ms<time<=500ms      |82217   |145     |953    |    |0     |0   |0     |0   |
|500ms<time<=1000ms     |5500    |487     |323    |    |0     |0   |0     |0     |
|1000ms<time<=2000ms    |380     |516     |0      |0   |0     |0   |0     |0     |
|2000ms<time            |4       |0       |0      |0   |0     |0   |0     |0     |
|fail                   |0       |0       |0      |0   |0     |0   |0     |0     |
|avg(ms)                |76.79   |59.77   |56.28  |    |4.33  |0.36|1.33  |0.47  |
|min(ms)                |48      |1       |21     |    |3     |0   |1     |0     |
|max(ms)                |2131    |1170    |422    |    |301   |391 |283   |412   | |




