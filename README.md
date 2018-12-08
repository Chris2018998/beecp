[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.67/BeeCP-0.67.jar">Download beeCP_0.67.jar</a>

# Configuration
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
|  poolInitSize  | connection size need create when pool initialization  |   |
|  poolMaxSize |  max connnection size in pool |    |
|  borrowerMaxWaitTime |request timeout for  borrower  |   |
|  preparedStatementCacheSize | stement cache size |   |
| connectionIdleTimeout  | max idle time,then will be close  |    |
| connectionValidateSQL |  a test sql to check connection ative   |    |   |

# DataSource Demo
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

# Performace test
---
Oe million cycle test for popular connection pools in mutil-thread Concurrent

1: take connection from pool.
    *getConenction(), con.close()*
	
2: take conneciton and execute query.
    *getConenction(), con.preparedStetment(), statement.execute(),   statement.close(),  con.close()*


|  Env |   value |   Remark|
| ------------ | ------------ | ------------ |
|  CPU | I3-7100(3.9HZ x 2)  | dual core  |
|  Memory |  8G |   |
| JDK  |  OpenJdk8-192 | not optimize  |
|  Datase | mysql5.6-64  | not optimize  |
|  JDBC Driver | Connector/J 5.1.47  |   | |

###### Connection pool for comparison

|  Pool Name  |   Version |   Remark|
| ------------ | ------------ | ------------ |
|  HikariCP|3.2.0 |  '光’ means the fastest  |
|  c3p0 |  0.9.5.2 |   |
| dbcp  |  1.4 |   |
|  Tomcat-JDBC |9.0.13 |   |
|  Druid | 1.1.12  | Alibaba product from china     |
|  vibur-dbcp |22.2 |   | |

######  Test Pool paramters settting

|  Parameter Name  |   Value |   Remark|
| ------------ | ------------ | ------------ |
|  pool init size | 0 |  |
|  pool max size |  0 |   |
| request timetou(ms)  |  40000 |    |
|  statement cache size |20 |    |  |


