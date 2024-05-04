/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools;

import java.util.Objects;

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final long spinForTimeoutThreshold = 1023L;
    public static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;

    public static String trimString(String value) {
        return value == null ? null : value.trim();
    }

    public static boolean objectEquals(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static boolean isBlank(String str) {
        if (str == null) return true;
        for (int i = 0, l = str.length(); i < l; ++i) {
            if (!Character.isWhitespace((int) str.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        if (str == null) return false;
        for (int i = 0, l = str.length(); i < l; ++i) {
            if (!Character.isWhitespace((int) str.charAt(i)))
                return true;
        }
        return false;
    }

    //xor
    public static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        return probe;
    }
}
