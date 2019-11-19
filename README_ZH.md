[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

一：介绍 <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
小蜜蜂连接池：一款简易型JDBC连接池

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>

二：特点
---

1：性能较好：跑分略高于光连接池(期待着网友们开箱验货^-^）

2：功能单一：只实现了池的借还和PreparedStatement缓存

3：代码量少：大约2000行左右开发源码

三：版本下载
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.3.3</version>
</dependency>

```

四：配置项说明
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

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191108_I3_7100_HikariCP_Bech_Pict.png"></img>

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191108_I3_7100_HikariCP_Bech.png"></img>

Download <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/BenchBase.java">BenchBase.java</a>

七：写给读者的话
---
1：由于是新开发作品，还存在一些问题和不足，恳请在您有空时帮忙看看，并告知。

2：如果您有更好更快的方案，期待您的分享。

3：最后祝您工作顺利，丰收2019,幸福2019。
