[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Introduction
---
BeeCP,a lightweight and  fast JDBC connection pool implementation. 

<a href="http://central.maven.org/maven2/com/github/chris2018998/BeeCP/0.67/BeeCP-0.67.jar">Download beeCP_0.67.jar</a>

Configuration
---
<table border="1" cellpadding="0" width="100%" bgcolor="#f7fafc" bordercolor="#DCDAE5">
  <tr bgcolor="#6699CC" >
    <th>Name</th>
    <th>Description</th>
	<th>Remark</th>
  </tr>
  <tr style="font-size:15">
    <td>poolInitSize</td>
    <td>pool initialization size</td>
	<td></td>
  </tr>
  <tr bgcolor="#E8D098" style="font-size:15">
    <td>poolMaxSize</td>
    <td>pool max size</td>
	<td></td>
  </tr>
  <tr style="font-size:15">
    <td>borrowerMaxWaitTime</td>
    <td>max waiting time for connection borrower</td>
	<td>time unit:Millisecond,default value:three minutes</td>
  </tr>	
  <tr style="font-size:15">
    <td>preparedStatementCacheSize</td>
    <td>preparedStatement cache size</td>
	<td></td>
  </tr>
  <tr bgcolor="#E8D098" style="font-size:15">
    <td>connectionIdleTimeout</td>
    <td>if connections idle time is more then the value,pool will close them</td>
	<td>time unit:Millisecond,default value:three minutes</td>
  </tr>
  <tr bgcolor="#E8D098" style="font-size:15">
    <td>connectionValidateSQL</td>
    <td>test connection is whether active</td>
	<td></td>
  </tr>
<table>

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
