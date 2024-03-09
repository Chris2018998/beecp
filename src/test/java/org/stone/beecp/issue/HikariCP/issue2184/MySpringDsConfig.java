package org.stone.beecp.issue.HikariCP.issue2184;

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
        String password = "";//put your driver class name

        BeeDataSourceConfig config = new BeeDataSourceConfig(driverName, jdbcUrl, userName, password);

        //fill Code and State
        config.addSqlExceptionCode(500150);//default is empty
        config.addSqlExceptionState("0A000");//default is empty

        /*
         * below are some code and state for reference to you,which are copied from HikariCP(ProxyConnection.java)
         * https://github.com/brettwooldridge/HikariCP/blob/dev/src/main/java/com/zaxxer/hikari/pool/ProxyConnection.java
         */
//        ERROR_STATES.add("0A000"); // FEATURE UNSUPPORTED
//        ERROR_STATES.add("57P01"); // ADMIN SHUTDOWN
//        ERROR_STATES.add("57P02"); // CRASH SHUTDOWN
//        ERROR_STATES.add("57P03"); // CANNOT CONNECT NOW
//        ERROR_STATES.add("01002"); // SQL92 disconnect error
//        ERROR_STATES.add("JZ0C0"); // Sybase disconnect error
//        ERROR_STATES.add("JZ0C1"); // Sybase disconnect error

//        ERROR_CODES = new HashSet<>();
//        ERROR_CODES.add(500150);
//        ERROR_CODES.add(2399);
//        ERROR_CODES.add(1105);

        /*
         * I will add a sql exception predication interface,which is used to determinate whether evict cons from pool
         * this feature is similar to SQLExceptionOverride of HikariCP,below are some ref code:
         *
         * SqlexceptionPredication predication = new xxxSqlexceptionPredication();
         * config.setSqlexceptionPredication(predication);
         *
         * public class xxxSqlexceptionPredication implements SqlexceptionPredication{
         *     //result is not null or not empty,means a cause of eviction
         *    public String evictTest(SQLException e){
         *      //put down your code
         *    }
         *  }//
         *
         * Ps: predication check -----> error code check ----->SQLState check
         */

        return new BeeDataSource(config);
    }
}
