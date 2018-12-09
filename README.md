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


<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/Pool_Test_src.zip">>Download Test source</a>


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

|Sumary|C3P0|DBCP|TOMCAT|Druid|Vibur|HikariCP| Bee_F|Bee_C|
| ------------ | ------------| ------------ | ------------ | ------------ | ------------ | ------------ | ------------ | ------------ |
|success Count|1000000|1000000|1000000|1000000|1000000|1000000|1000000|1000000|
|fail count|0|0|0|0|0|0|0|0|
|avr(ms)|37.6495|27.7597|24.5578|26.1534|4.1056|0.4689|1.1911|0.513|
|min(ms)|0|0|23|0|0|0|0|0|
|max(ms)|1372|197|413|926|133|214|170|159|
|time=0ms|154777|3061|0|301326|5315|940818|51675|999993|
|0ms<time<=10ms|157981|88|0|5915|985628|52059|944961|5|
|10ms<time<=30ms|269698|945209|985612|462979|5498|2846|1477|0|
|30ms<time<=50ms|160315|42660|11622|34688|1029|1067|933|0|
|50ms<time<=100ms|173887|7291|714|174597|2529|2304|944|0|
|100ms<time<=200ms|71897|1691|1054|19084|1|902|10|2|
|200ms<time<=500ms|10673|0|998|527|0|4|0|0|
|500ms<time<=1000ms|733|0|0|884|0|0|0|0|
|1000ms<time<=2000ms|39|0|0|0|0|0|0|0|
|2000ms<time|0|0|0|0|0|0|0|0||

**Statement cycle test result(1000thread x 1000 cycle )**

|Sumary|C3P0|DBCP|TOMCAT|Druid|Vibur|HikariCP| Bee_F|Bee_C|
| ---  | ---  | --- | --- | --- | --- | --- | --- | --- |
|success Count|1000000|1000000|1000000|1000000|1000000|1000000|1000000|1000000|
|fail count|0|0|0|0|0|0|0|0|
|avr(ms)|83.5001|64.8457|62.1005|62.8310|35.9717|22.6460|36.9728|22.3423|
|min(ms)|0|0|59|0|0|0|0|0|
|max(ms)|3092|236|472|1708|189|35270|180|36795|
|time=0ms|124195|2996|0|146851|1133|995210|2133|995299|
|0ms<time<=10ms|74814|101|0|4736|6|2058|26|2131|
|10ms<time<=30ms|178069|10|0|4387|1|104|11|299|
|30ms<time<=50ms|121037|5|0|5525|990469|23|986821|46|
|50ms<time<=100ms|209700|991943|995716|711425|6684|36|9058|33|
|100ms<time<=200ms|189003|4514|3227|119582|1707|42|1951|28| 
|200ms<time<=500ms|95721|431|1057|6176|0|143|0|88|
|500ms<time<=1000ms|6525|0|0|794|0|150|0|105|
|1000ms<time<=2000ms|820|0|0|524|0|249|0|176|
|2000ms<time|116|0|0|0|0|1985|0|1795||
