[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/BeeCP-0.68.jar">Download beeCP_0.68.jar</a>

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
| JDK  | Jdk8-192 | 2G  |
|  Datase | mariadb-10.3.11-winx64 | not optimize  |
|  JDBC Driver | mariadb-java-client-2.3.0.jar  |   | |

**Connection pool for comparison**

|  Pool Name  |   Version |   Remark|
| ------------ | ------------ | ------------ |
|  HikariCP|3.2.0 | '光’ means the fastest  |
|  c3p0 |  0.9.5.2 |   |
| dbcp  |  1.4 |   |
|  Tomcat-JDBC |9.0.13 |   |
|  Druid | 1.1.12  | Alibaba product from china|
|  BeeCP | 0.67  |  |
|  vibur-dbcp |22.2 |   | |

**Test Pool paramters settting**

|  Parameter Name  |   Value |   Remark|
| ------------ | ------------ | ------------ |
|  pool init size | 0 |  |
|  pool max size |10 |   |
| request timetou(ms)  |  40000 |    |
|  statement cache size |20 |    |  |


<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/Pool_Test_src.zip">Download Test source</a>

follow are one test result

**connection cycle test result(1000 thread x 1000 cycle )**

|Sumary|C3P0|DBCP|TOMCAT|Druid|Bee_F|Bee_C|Vibur|HikariCP|
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|success Count|1000000|1000000|1000000|1000000|1000000|1000000|1000000|1000000|
|fail count|0|0|0|0|0|0|0|0|
|avr(ms)|28.8612|46.5034|2.6441|70.4252|1.6737|0.1587|4.4452|0.3527|
|min(ms)|0|0|0|0|0|0|3|0|
|max(ms)|1651|169|149|1017|52|226|142|352|
|time=0ms|241229|3042|182|204544|999|997592|0|961504|
|0ms<time<=10ms|175996|55|994287|183643|998000|757|996201|36043|
|10ms<time<=30ms|272023|163197|1428|4018|500|628|2799|1187|
|30ms<time<=50ms|129341|386496|119|7939|500|54|0|148|
|50ms<time<=100ms|128583|446133|2542|367523|1|38|496|256|
|100ms<time<=200ms|46891|1077|1442|152347|0|930|504|362|
|200ms<time<=500ms|5079|0|0|78422|0|1|0|500|
|500ms<time<=1000ms|612|0|0|1563|0|0|0|0|
|1000ms<time<=2000ms|246|0|0|1|0|0|0|0|
|2000ms<time|0|0|0|0|0|0|0|0||

**Statement cycle test result(1000 thread x 1000 cycle )**

|Sumary|C3P0|DBCP|TOMCAT|Druid|Bee_F|Bee_C|Vibur|HikariCP|
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|success Count|1000000|1000000|1000000|1000000|1000000|1000000|1000000|1000000|
|fail count|0|0|0|0|0|0|0|0|
|avr(ms)|58.70|86.41|98.55|99.31|56.93|28.33|48.29|36.24|
|min(ms)|0|0|15|0|0|0|42|0|
|max(ms)|2322|170|346|1586|184|33926|328|43162|
|time=0ms|193493|3003|0|105359|1034|948197|0|921887|
|0ms<time<=10ms|100131|41|0|298510|1959|34945|0|65800|
|10ms<time<=30ms|207179|784|957|2524|1203|3411|0|2574|
|30ms<time<=50ms|124131|38173|176|3710|2720|1274|961195|869|
|50ms<time<=100ms|182236|757049|897661|146979|991238|1534|36753|931|
|100ms<time<=200ms|137494|200950|99493|270586|1846|1425|1062|760|
|200ms<time<=500ms|52675|0|1713|161311|0|1753|990|996|
|500ms<time<=1000ms|2404|0|0|10914|0|1385|0|876|
|1000ms<time<=2000ms|254|0|0|107|0|1769|0|1075|
|2000ms<time|3|0|0|0|0|4307|0|4232||
