package org.stone.beecp.issue.HikariCP.issue2174;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;

//@Configuration
public class SpringBootDsConfig {

    //@Bean
    public DataSource myDataSource() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setMaxActive(50);
        config.setMaxWait(30000);
        return new BeeDataSource(config);//very simple? play with my baby pool
    }
}
