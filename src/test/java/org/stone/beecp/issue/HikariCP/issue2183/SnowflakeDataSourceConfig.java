package org.stone.beecp.issue.HikariCP.issue2183;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Configuration file on Springboot
 */
//@Configuration
public class SnowflakeDataSourceConfig {

    //@Bean
    public DataSource SnowflakeDataSource() {
        String userName = "";//put your snowflake userName
        String jdbcUrl = "jdbc:snowflake://localhost:8080";//replace it with your snowflake url
        String driverName = "com.snowflake.client.jdbc.SnowflakeDriver";

        /* jdbc artifactId
         * <dependency>
         *    <groupId>net.snowflake</groupId>
         *    <artifactId>snowflake-jdbc</artifactId>
         *    <version>3.15.0</version>
         *</dependency>
         */

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //config.setUrl(jdbcUrl);
        //config.setDriverClassName(driverName);
        long timeout = TimeUnit.MINUTES.toMillis(15);
        config.setIdleTimeout(timeout);
        config.setHoldTimeout(timeout);

        //you can add more parameters into this ConnectionFactory
        config.setRawConnectionFactory(new SnowflakeConnectionFactory(userName, jdbcUrl));

        //return SnowflakeDataSource to springboot
        return new SnowflakeDataSource(new BeeDataSource(config));
    }
}
