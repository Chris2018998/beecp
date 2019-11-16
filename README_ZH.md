[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

一：介绍 <img height="50px" width="50px" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/individual/bee.png"></img>
---
一款简易型且具有较好性能的JDBC连接池

<a href="https://github.com/Chris2018998/BeeCP/blob/master/README.md">English</a>

二：特点
---

1：性能较好：跑分略高于光连接池(期待网友们验证）

2：功能单一：只实现了池的借还和PreparedStatement缓存

3：代码量少：大约2000行左右开发源码

三：技术细节
---

1：连接采用数组存储（思路源自CopyOnWriteArrayList）

2：采用信号量(Semaphore)作为并发控制（门阀控制）

3：使用并发队列(ConcurrentLinkedQueue)作为等待者队列（等待者自加入自离开，此点为池的最大亮点）（思路源自SynchronousQueue，它被光连接池用来做交换队列：释放者将连接推入队列，等待者则从中拉取）
   
4：使用Javassist生成JDBC相关接口的封装类（光连接池也是如此做法）

5：资源分配采用了两种模式：公平模式和竞争模式(二选一)
  
 * 5.1 公平模式下，信号量采用公平锁，先到先得连接；此外借用者在释放连接时，是直接Transfer给等待者。
 
 * 5.1 竞争模式下，信号量采用非公平锁，使用CMS锁的方式抢占池中的连接；连接被释放的时候，先将连接设置为闲置状态，再Transfer给等到者，让等待者和进入池中借用者进行抢占。（光连接池应该是采用这种方式，若采用SynchronousQueue等待者未抢到连接，是否需要二次或多次入列，从而造成性能损耗？） 
    
6：超时控制
   
 * 6.1：采用纳秒(NanoSecond)作为时间控制（LockSupport在Java并发包中使用纳秒的运用频率比较高)
	 
 * 6.2：预先计算好超时的时刻点(deadline)（光连接池好像是在取得锁之后才计算的)？
   
 * 6.3：信号量取得（门阀控制）可能存在超时可能性
	
 * 6.4：在未取到有效的连接后，等待过程中有可能超时
		
7：线程等待

 *  7.1：等待信号量（获得进门的机会）
  
 *  7.2：等待连接的释放（借用者释放或构建线程的传递）

四：与其他池的比较
---

 由于个人没有深入阅读其他连接池的代码（能力有限，实话，请网友们不要考我）不好做出正确的比较。
 
 目前只实现了连接池的基本功能(借还，Statement缓存), 从个人机器的跑分情况来看确实高于其他连接池。
 
 在监控方式比较偏弱，未来会加入Jmx和日志打印。

 
五：期望与愿景
---

1：开发此连接池纯碎业余学习爱好，只是尝试一下按照这个方式，能否让它更快一点

2：由于它是一款新开发的连接池，目前需要的是网友们的更多参与和实践检验。

3：若它能成熟稳健，希望它能为Java界贡献一点点小力量(人民大众力量才是伟大的，向中国广大程序工作者致敬：您辛苦了。 ^-^)

4：个人不图名利，有需要的话会持续支持它。

5：如果您有更好更快的方案，请分享给我们大家，同时也期待您更多的批评和指正意见。


六：版本下载
---

```java
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>BeeCP</artifactId>
   <version>1.3.1</version>
</dependency>

```

七：配置项说明
---
|  Name  |   Description |   Remark |
| ------------ | ------------ | ------------ |
| initialSize     |连接池初始大小  |   |
| maxActive       |连接池最大个数  |    |
| maxWait         |连接借用等待最大时间(毫秒)  |   |
| idleTimeout     |连接闲置最大时间(毫秒)     |   |  
| preparedStatementCacheSize |SQL宣言缓存大小 |   
| validationQuery |连接是否存活测试查询语句   |    |   |


八：参考Demo
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


九：性能测试
---
以多线程查询(1000个线程各自执行1000次，共100万次)的方式测试各连接池性能，并打印耗时分布以及平均耗时，最后依据平时耗时为各连接池进行名次排列，单次时间统计(机器状态对测试结果有较大影响)：

[datasource.getConnection(),connection.prepareStatement,statement.execute(),statement.close(),connection.close()]</i>

1：下面为各连接池在mysql5.6的下测试结果（单位：毫秒）


Bee_C(5.3623) > Vibur(6.5437) > Bee_F(6.8492) > HikariCP(9.0176)

<a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191105_JDBCPool_I54300.log">20191105_JDBCPool_I54300.log</a>
 
性能测试代码请访问项目：https://github.com/Chris2018998/PoolPerformance


3： 采用光连接池的性能基准测试结果

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191108_I3_7100_HikariCP_Bech_Pict.png"></img>

<img height="100%" width="100%" src="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/20191108_I3_7100_HikariCP_Bech.png"></img>

Download <a href="https://github.com/Chris2018998/BeeCP/blob/master/doc/performance/BenchBase.java">BenchBase.java</a>
