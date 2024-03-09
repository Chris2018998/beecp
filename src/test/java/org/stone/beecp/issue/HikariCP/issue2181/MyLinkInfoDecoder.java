package org.stone.beecp.issue.HikariCP.issue2181;

import org.stone.beecp.BeeJdbcLinkInfoDecoder;

public class MyLinkInfoDecoder extends BeeJdbcLinkInfoDecoder {

    public String decodeUrl(String url) {
        //inputted url: jdbc:redshift://${REDSHIFT_CLUSTER_NAME}.redshift.amazonaws.com:${REDSHIFT_PORT}/${REDSHIFT_DATABASE_NAME}
        //outputted url: jdbc:redshift://my-cluster.redshift.amazonaws.com:port/databaseName

        //@todo implement code to replace variables in url
        return url;
    }

    public String decodeUsername(String username) {
        return username;
    }

    public String decodePassword(String password) {
        return password;
    }
}
