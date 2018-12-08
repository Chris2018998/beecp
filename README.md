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

Performace test
---
<table border="1" cellpadding="0" bgcolor="#f7fafc" bordercolor="#DCDAE5">
  <tr bgcolor="#6699CC" >
    <th>Env </th>
    <th>value</th>
    <th>Remark</th>
  </tr>
  <tr style="font-size:15">   
    <td style="background:yellow">CPU</td>
    <td>I3-7100(3.9HZ x 2)</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">Memory</td>
    <td>8G</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
   <td style="background:yellow">JDK</td>
    <td>OpenJdk8-192</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">Datase</td>
    <td>mysql5.6-64</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">JDBC Driver</td>
    <td>Connector/J 5.1.47</td>
    <td></td>
  </tr>	
<table>

---
Oe million cycle test for popular connection pools in mutil-thread Concurrent

1: take connection from pool. 

2: take conneciton and execute query. 

<table border="1" cellpadding="0"  bgcolor="#f7fafc" bordercolor="#DCDAE5">
  <tr bgcolor="#6699CC" >
    <th>Pool Name</th>
    <th>Version</th>
    <th>Remark</th>
  </tr>
   <tr style="font-size:15">   
    <td style="background:yellow">HikariCP</td>
    <td>3.2.0</td>
    <td></td>
  </tr>
  <tr style="font-size:15">   
    <td style="background:yellow">c3p0</td>
    <td>0.9.5.2</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">dbcp</td>
    <td>1.4</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
   <td style="background:yellow">Tomcat-JDBC</td>
    <td>9.0.13</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">Druid</td>
    <td>1.1.12</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">vibur-dbcp</td>
    <td>22.2</td>
    <td></td>
  </tr>	
<table>

---
Pool paramters settting 

<table border="1" cellpadding="0" bgcolor="#f7fafc" bordercolor="#DCDAE5">
  <tr bgcolor="#6699CC" >
    <th>Parameter Name</th>
    <th>Value</th>
    <th>Remark</th>
  </tr>	
   <tr style="font-size:15">   
    <td style="background:yellow">pool init size</td>
    <td>0</td>
    <td></td>
  </tr>
  <tr style="font-size:15">   
    <td style="background:yellow">pool max size</td>
    <td>10</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
    <td style="background:yellow">request timetou(ms)</td>
    <td>40000</td>
    <td></td>
  </tr>
  <tr style="font-size:15">
   <td style="background:yellow">statement cache size</td>
    <td>20</td>
    <td></td>
  </tr>
<table>

