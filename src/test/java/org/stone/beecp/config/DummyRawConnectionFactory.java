package org.stone.beecp.config;

import org.stone.beecp.RawConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DummyRawConnectionFactory implements RawConnectionFactory {
    private String url;
    private String user;
    private String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Connection create() throws SQLException {
        return null;
    }
}
