package org.stone.beecp.issue.HikariCP.issue2183;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Connection factory Implementation with a JDBC Driver
 * <p>
 * Class copy from org.stone.beecp.pool.ConnectionFactoryByDriver
 *
 * @author Chris liao
 * @version 1.0
 */
public final class SnowflakeConnectionFactory implements RawConnectionFactory {
    //url to your Snowflake db
    private final String url;
    //extra properties to link db
    private final Properties properties;
    //token expire time point(milliseconds)
    private long accessTokenExpireTime;//access token only 15 minutes lifetime

    //Constructor
    public SnowflakeConnectionFactory(String userName, String url) {
        this.url = url;
        this.properties = new Properties();
        this.properties.put("u", userName);
    }

    //return a connection if link successful to db,otherwise,throws a failure exception
    public final Connection create() throws SQLException {
        //1: refresh snowflake token
        if (accessTokenExpireTime == 0 || accessTokenExpireTime <= System.currentTimeMillis()) {//token was expired
            synchronized (this) {
                if (accessTokenExpireTime == 0 || accessTokenExpireTime <= System.currentTimeMillis()) {
                    String token = this.getToken();//refresh token by expired time;
                    this.accessTokenExpireTime = calculateExpireTime(token);//new time
                    this.properties.put("p", token);//set as a new password
                }
            }
        }

        //2: establish a connection by DriverManager
        Connection con = DriverManager.getConnection(url, properties);

        //3:create a wrapper on the connection
        return new SnowflakeConnectionWrapper(con, accessTokenExpireTime);
    }

    private long calculateExpireTime(String token) {
        //@todo if exists expired time in token. get it and return
        return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
    }

    private String getToken() {
//        var region = new DefaultAwsRegionProviderChain().getRegion();
//        var hostnamePort = getHostnamePort();
//
//        RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
//                .credentials(new DefaultAWSCredentialsProviderChain())
//                .region(region)
//                .build();
//
//        GetIamAuthTokenRequest request = GetIamAuthTokenRequest.builder()
//                .hostname(hostnamePort.getFirst())
//                .port(hostnamePort.getSecond())
//                .userName(getUsername())
//                .build();
//
//         return generator.getAuthToken(request);

        return "";//return a dummy token
    }
//    // JDBC URL has a standard URL format, like: jdbc:postgresql://localhost:5432/test_database
//    private Pair<String, Integer> getHostnamePort() {
//        var slashing = url.indexOf("//") + 2;
//        var sub = getJdbcUrl().substring(slashing, getJdbcUrl().indexOf("/", slashing));
//        var splitted = sub.split(":");
//        return Pair.of(splitted[0], Integer.parseInt(splitted[1]));
//    }

}
