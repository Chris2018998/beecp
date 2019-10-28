[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

产品介绍 <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
小蜜蜂是一款轻量级（15个文件，2000行源码）高性能Java连接池

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>


命名缘由
---
蜜蜂对人类是一种有益昆虫，曾有新闻报导其数量呈下降趋势，将会直接影响到人类食物的产量，希望能引起更多朋友的关注和重视：蜜蜂虽小，作用很大，保护环境，关爱大自然。

Maven下载
---
Download<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/release/BeeCP-1.1.1.jar">BeeCP_1.1.1.jar</a>

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.1.1</version>
</dependency>

```

配置说明
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
| initialSize     |连接池初始大小  |   |
| maxActive       |连接池最大个数  |    |
| maxWait         |连接借用等待最大时间(毫秒)  |   |
| idleTimeout     |连接闲置最大时间(毫秒)     |   |  
| preparedStatementCacheSize |SQL宣言缓存大小 |   
| validationQuery |连接是否存活测试查询语句   |    |   |


SpringBoot使用参考
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


性能测试
---
以多线程查询(1000个线程各自执行1000次，共100万次)的方式测试各连接池性能，并打印耗时分布以及平均耗时，最后依据平时耗时为各连接池进行名次排列，单次时间统计(机器状态对测试结果有较大影响)：

[datasource.getConnection(),connection.prepareStatement,statement.execute(),statement.close(),connection.close()]</i>

1：下面为各连接池在Oracle11G的下测试结果（单位：毫秒）

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20190717_I5_4210M_Oracle11g.log">20190808_I5_4210M_Orcale11g.log</a>

Bee_F(16.37) > Bee_C(18.25) > Vibur(28.79) > HikariCP(34.42) > TOMCAT(67.47) > DBCP(75.28) > Druid(75.97) > C3P0(96.40)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20190723_I5_4210M_Oracle11g.log">20190808_I5_4210M_Orcale11g.log</a>

Bee_F(13.39) > Bee_C(15.25) > Vibur(20.64) > HikariCP(28.79) > TOMCAT(57.93) > DBCP(66.47) > Druid(67.03) > C3P0(71.54)


2：以光连接池的驱动（专用于连接池性能测试的驱动）测试情况如下

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20190729_I3_7100_HikariCP_Driver.log">20190729_I3_7100_HikariCP_Driver.log</a>

Bee_F(0.0006) > Bee_C(0.0980) > HikariCP(0.3053) > Vibur(0.3068) > TOMCAT(1.9001) > DBCP(3.9862) > C3P0(6.3528) > Druid(9.7170)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20190808_I5_4210M_HikariCP_Driver.log">20190808_I5_4210M_HikariCP_Driver.log</a>

Bee_C(0.0018) > Vibur(0.0048) > Bee_F(0.1982) > HikariCP(0.3832) > TOMCAT(2.3388) > Druid(3.0775) > DBCP(5.2606) > C3P0(11.9082)

性能测试代码请访问项目：https://github.com/Chris2018998/PoolPerformance


3： 采用光连接池的性能基准测试结果

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191027_I3_7100_HikariCP_Bech_Pict.png"></img>

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191027_I3_7100_HikariCP_Bech.png"></img>

Download <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/BenchBase.java">BenchBase.java</a>


 
