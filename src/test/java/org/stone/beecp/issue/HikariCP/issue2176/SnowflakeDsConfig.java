package org.stone.beecp.issue.HikariCP.issue2176;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.DataSource;
import java.sql.SQLException;

//@Configuration
public class SnowflakeDsConfig {

    //@Bean
    public DataSource snowDataSource() throws SQLException {
        //1:create a key container to store snowflake keys(just support two keys)
        String firstKey = null;//@todo need you put first key here
        SnowflakeKeyStore store = new SnowflakeKeyStore(firstKey);

        //2:create a bee datasource
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        SnowConnectionFactory factory = new SnowConnectionFactory(store);
        config.setRawConnectionFactory(factory);
        BeeDataSource beeDs = new BeeDataSource(config);

        //3:create a wrapped dataSource on bee datasource
        SnowDataSource ds = new SnowDataSource(store, beeDs);

        return ds;//register to spring container
    }
}
