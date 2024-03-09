package org.stone.beecp.issue.HikariCP.issue2181;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;

/**
 * Springboot Configuration file
 */
//@Configuration
public class MySpringDsConfig {
    //@Bean
    public DataSource SnowflakeDataSource() {
        String userName = "";//put your userName
        String jdbcUrl = "";//put your url
        String driverName = "";//put your driver class name

        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setUrl(jdbcUrl);
        config.setDriverClassName(driverName);
        config.setJdbcLinkInfoDecoderClass(MyLinkInfoDecoder.class);

        return new BeeDataSource(config);
    }
}
