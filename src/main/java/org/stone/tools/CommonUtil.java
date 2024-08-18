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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

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

    public static int getArrayIndex(int hash, int arrayLen) {
        return (arrayLen - 1) & (hash ^ (hash >>> 16));
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
    public static int advanceProbe(final int probe) {
        int adProbe = probe;
        adProbe ^= adProbe << 13;
        adProbe ^= adProbe >>> 17;
        adProbe ^= adProbe << 5;
        return adProbe;
    }

    public static Properties loadPropertiesFromClassPathFile(String filename) {
        InputStream fileStream = CommonUtil.class.getClassLoader().getResourceAsStream(filename);
        if (fileStream == null) throw new IllegalArgumentException("Not found classpath file:" + filename);

        try {
            Properties properties = new Properties();
            properties.load(fileStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load classpath file:" + filename);
        } finally {
            try {
                fileStream.close();
            } catch (IOException e) {
                System.err.println("Failed to close stream on classpath file:" + filename);
            }
        }
    }
}
