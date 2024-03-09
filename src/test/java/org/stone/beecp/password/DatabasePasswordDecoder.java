/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.password;

import org.stone.beecp.BeeJdbcLinkInfoDecoder;

/**
 * Password Decoder
 *
 * @author chris liao
 */
public class DatabasePasswordDecoder extends BeeJdbcLinkInfoDecoder {
    private static final String new_password = "abc";

    public static final String password() {
        return new_password;
    }

    public String decodePassword(String password) {
        return new_password;
    }
}