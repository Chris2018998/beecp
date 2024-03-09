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
 * A cipher text decoder on jdbc link info[jdbc url,jdbc username and jdbc password],
 * and provides some methods to may be override in sub class
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeJdbcLinkInfoDecoder {

    public String decodeUrl(String url) {
        return url;
    }

    public String decodeUsername(String username) {
        return username;
    }

    public String decodePassword(String password) {
        return password;
    }
}



