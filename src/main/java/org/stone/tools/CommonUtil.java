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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static org.stone.tools.BeanUtil.BeeClassLoader;

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;
    public static final int maxUntimedSpins = maxTimedSpins << 4;
    public static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1023L;
    public static final int INT_MOVE_SHIFT = 16;
    public static final int INT_CLN_HIGH_MASK = 0xFFFF;//65535;

    public static int low16(int v) {
        return v & INT_CLN_HIGH_MASK;
    }

    public static int high16(int v) {
        return v >>> INT_MOVE_SHIFT;
    }

    public static int contact(int h, int l) {
        return (h << INT_MOVE_SHIFT) | (l & INT_CLN_HIGH_MASK);
    }

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
        for (int aChar : str.toCharArray()) {
            if (!Character.isWhitespace(aChar))
                return false;
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        if (str == null) return false;
        for (int aChar : str.toCharArray()) {
            if (!Character.isWhitespace(aChar))
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
        try (InputStream fileStream = BeeClassLoader.getResourceAsStream(filename)) {
            if (fileStream == null) throw new FileNotFoundException("Not found file:" + filename);
            Properties properties = new Properties();
            properties.load(fileStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load classpath file:" + filename, e);
        }
    }
}
