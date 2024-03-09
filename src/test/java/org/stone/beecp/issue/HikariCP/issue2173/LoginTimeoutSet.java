package org.stone.beecp.issue.HikariCP.issue2173;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

public class LoginTimeoutSet {
    public static void main(String[] args) {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectTimeout(3);//time unit:second
        BeeDataSource ds = new BeeDataSource(config);

        BeeDataSource ds2 = new BeeDataSource("", "", "", "");
        ds2.setConnectTimeout(3);//time unit:second
    }
}
