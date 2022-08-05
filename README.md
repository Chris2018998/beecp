![图片](https://user-images.githubusercontent.com/32663325/154847136-10e241ae-af4c-478a-a608-aaa685e0464b.png)<br/>
<a><img src="https://img.shields.io/badge/JDK-1.7+-green.svg"></a>
<a><img src="https://img.shields.io/badge/License-LGPL%202.1-blue.svg"></a>
<a><img src="https://maven-badges.herokuapp.com/maven-central/com.github.chris2018998/beecp/badge.svg"></a>
 
## :coffee: Introduction 

BeeCP, a small JDBC connection pool component, has the characteristics of high performance, light code and good stability.

*  Java language development, with cross platform advantages
*  Based on parameter driving, support multiple parameter settings and import of configuration files
*  Applicable to a variety of database drivers (up to now, mainstream databases can be adapted)
*  Support local transaction and distributed transaction
*  Developed by JUC technology, with highlights such as single point cache, semaphore control, queue multiplexing, non mobile waiting, spin control, transfer connection and exception , asynchronous add , and safe close
*  Provide log output and monitoring tools  
*  Good robustness and quick response to unexpected situations (such as network disconnection and database service crash)
*  Good interface extensibility

## :arrow_down: Download 

Java7 or higher
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>3.3.8</version>
</dependency>
```

Java6
```xml
<dependency>
   <groupId>com.github.chris2018998</groupId>
   <artifactId>beecp</artifactId>
   <version>1.6.10</version>
</dependency>
```
## :tractor: Example

### :point_right: Example-1(independent)

```java
BeeDataSourceConfig config = new BeeDataSourceConfig();
config.setDriverClassName("com.mysql.jdbc.Driver");
config.setJdbcUrl("jdbc:mysql://localhost/test");
config.setUsername("root");
config.setPassword("root");
BeeDataSource ds=new BeeDataSource(config);
Connection con=ds.getConnection();
....

```
### :point_right: Example-2(Springbooot)

*application.properties*

```java
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.url=jdbc:mysql://localhost/test
spring.datasource.driverClassName=com.mysql.jdbc.Driver
``` 

*DataSourceConfig.java*
```java
@Configuration
public class DataSourceConfig {
  @Value("${spring.datasource.username}")
  private String user;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.driverClassName}")
  private String driver;

  @Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().type(cn.beecp.BeeDataSource.class).build();
  }
  
  @Bean
  public DataSource secondDataSource() {
   return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
  }
}
```

:sunny: *If your project is of springboot type, we recommended to use the data source management tool:<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a> (without code development and configuration, and with its own monitoring interface)*

## :book: Function list
![图片](https://user-images.githubusercontent.com/32663325/153597592-c7d36f14-445a-454b-9db4-2289e1f92ed6.png)


## :computer: Runtime monitor

In order to better monitor  the pool (* idle connections, in use connections, waiting connections *), three ways are provided   
* slf4j log
* Jmx monitor
* Provide method level monitoring (access the monitoring method of the data source to obtain a VO result object that can reflect the status in the pool)

In addition to the above methods, we have prepared a set of solutions with monitoring interface：<a href="https://github.com/Chris2018998/BeeCP-Starter">BeeCP-Starter</a>

![图片](https://user-images.githubusercontent.com/32663325/154832186-be2b2c34-8765-4be8-8435-b97c6c1771df.png)

![图片](https://user-images.githubusercontent.com/32663325/178511569-8f6e16f4-92fc-41ee-ba6b-960e54bf364b.png


