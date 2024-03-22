package org.stone.beecp.config;

import org.stone.beecp.SQLExceptionPredication;

import java.sql.SQLException;

public class DummySqlExceptionPredication implements SQLExceptionPredication {

    //return desc of eviction,if null or empty,not be evicted
    public String check(SQLException e) {
        return null;
    }
}
