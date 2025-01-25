/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

/**
 * An overridable class to decode JDBC info(username,password,url).
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeJdbcLinkInfoDecoder {

    /**
     * Decodes jdbc url.
     *
     * @param url is from {@link BeeDataSourceConfig#getJdbcUrl()}
     * @return a decoded url
     */
    public String decodeUrl(String url) {
        return url;
    }

    /**
     * Decodes jdbc username.
     *
     * @param username is from {@link BeeDataSourceConfig#getUsername()}
     * @return a decoded username
     */
    public String decodeUsername(String username) {
        return username;
    }

    /**
     * Decodes jdbc password.
     *
     * @param password is from {@link BeeDataSourceConfig#getPassword()}
     * @return a decoded password
     */
    public String decodePassword(String password) {
        return password;
    }
}



