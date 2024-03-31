/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config.customization;

import org.stone.beecp.BeeJdbcLinkInfoDecoder;

public class DummyJdbcLinkInfoDecoder extends BeeJdbcLinkInfoDecoder {

    public String decodeUrl(String url) {
        return url + "-Decoded";
    }

    public String decodeUsername(String username) {
        return username + "-Decoded";
    }

    public String decodePassword(String password) {
        return password + "-Decoded";
    }
}
