[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

一：介绍 <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
小蜜蜂池：一款高性能JDBC连接池

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>

二：特点
---
1：性能好：性能高于光连接池

2：代码少：21个文件，2600行源码

三：版本下载
---
**Java7**
```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>2.0.0</version>
</dependency>
```

**Java6**
```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.6.0</version>
</dependency>

```
*友情提示：建议使用最新版本


四：功能支持
---
```java
1：请求超时支持

2：两种模式：公平与竞争

3：断网连接池自动恢复

4：闲置超时和持有超时处理

5：PreparedStatement缓存支持（也可关闭）

6：支持连接回收前，事物回滚

7：支持连接回收前，属性重置（比如：autoCommit,transactionIsolation,readonly,catlog,schema,networkTimeout）

8: 支持JMX

9：支持连接工厂自定义
```

五：配置项说明
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
| initialSize     |连接池初始大小  |   |
| maxActive       |连接池最大个数  |    |
| maxWait         |连接借用等待最大时间(毫秒)  |   |
| idleTimeout     |连接闲置最大时间(毫秒)     |   |  
| preparedStatementCacheSize |SQL宣言缓存大小 |   
| validationQuery |连接是否存活测试查询语句   |    |   |


五：参考Demo
---
```java
application.properties
spring.datasource.username=xx
spring.datasource.password=xx
spring.datasource.url=xx
spring.datasource.driverClassName=xxx
spring.datasource.datasourceJndiName=xxx
```

```java
@Configuration
public class DataSourceConfig {
  @Value("${spring.datasource.driverClassName}")
  private String driver;
  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.username}")
  private String user;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${spring.datasource.datasourceJndiName}")
  private String datasourceJndiName;
  private BeeDataSourceFactory dataSourceFactory = new BeeDataSourceFactory();
  
  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(cn.bee.dbcp.BeeDataSource.class).build();
  }
  
  @Bean
  public DataSource secondDataSource(){
    return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
  }
  
  @Bean
  public DataSource thirdDataSource()throws SQLException {
    try{
       return dataSourceFactory.lookup(datasourceJndiName);
     }catch(NamingException e){
       throw new SQLException("Jndi DataSource not found："+datasourceJndiName);
     }
  }
}
```


六：性能测试
---
以多线程查询(1000个线程各自执行1000次，共100万次)的方式测试各连接池性能，并打印耗时分布以及平均耗时，最后依据平时耗时为各连接池进行名次排列，单次时间统计(机器状态对测试结果有较大影响)：

[datasource.getConnection(),connection.prepareStatement,statement.execute(),statement.close(),connection.close()]</i>

1：下面为各连接池在mysql5.6的下测试结果（单位：毫秒）

Bee_C(5.3623) > Bee_F(6.8492) > HikariCP(9.0176)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191105_JDBCPool_I54300.log">20191105_JDBCPool_I54300.log</a>
 
性能测试代码请访问项目：https://github.com/Chris2018998/PoolPerformance

3： 采用光连接池的性能基准测试结果

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191212_I3_7100_HikariCP_Bech_Pict.png"></img>

下载 <a href="https://raw.githubusercontent.com/Chris2018998/BeeCP/master/doc/performance/HikariCP-benchmark_BeeCP.zip">HikariCP-benchmark_BeeCP.zip</a>
