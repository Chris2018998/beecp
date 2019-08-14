
import javax.sql.DataSource;
import org.jmin.bee.BeeDataSource;
import org.jmin.bee.BeeDataSourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 *SpringBoot多数据源配置参考
 */
@Configuration
@Profile({"dev"})
public class DataSourceConfig {
	@Value("spring.primary.datasource.driverClassName")
	private String driver;
	@Value("spring.primary.datasource.jdbcUrl")
	private String url;
	@Value("spring.primary.datasource.username")
	private String user;
	@Value("spring.primary.datasource.password")
	private String password;
	
	@Bean(name = "primaryDataSource")
    @Primary
    @ConfigurationProperties(prefix="spring.primary.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().type(org.jmin.bee.BeeDataSource.class).build();
    }

    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix="spring.secondary.datasource")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().type(org.jmin.bee.BeeDataSource.class).build();
    }
    
    @Bean(name = "threeDataSource")
    public DataSource threeDataSource(){
       return new BeeDataSource(new BeeDataSourceConfig(driver,url,user,password));
    }
}