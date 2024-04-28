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
 * In order to safety policy,users maybe set encrypted jdbc link items(url,username,password) to {@link BeeDataSourceConfig},
 * so an overridable decoder is provided to decode them when pool initialization.A decoder need be set to a {@link BeeDataSourceConfig}
 * before it works,there are three set methods are below
 * 1: decoder instance set: {@link BeeDataSourceConfig#setJdbcLinkInfoDecoder}
 * 2: decoder class set:{@link BeeDataSourceConfig#setJdbcLinkInfoDecoderClass}
 * 3: decoder class name set:{@link BeeDataSourceConfig#setJdbcLinkInfoDecoderClassName}
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeJdbcLinkInfoDecoder {

    /**
     * Decodes a jdbc url.
     *
     * @param url is from {@link BeeDataSourceConfig#getJdbcUrl()}.
     * @return a decoded url
     */
    public String decodeUrl(String url) {
        return url;
    }

    /**
     * Decodes a jdbc username.
     *
     * @param username is from {@link BeeDataSourceConfig#getUsername()}.
     * @return a decoded username
     */
    public String decodeUsername(String username) {
        return username;
    }

    /**
     * Decodes a jdbc password.
     *
     * @param password is from {@link BeeDataSourceConfig#getPassword()}.
     * @return a decoded password
     */
    public String decodePassword(String password) {
        return password;
    }
}



