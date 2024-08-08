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

import org.stone.tools.exception.ReflectionOperationException;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.stone.tools.BeanUtil.setAccessible;

/**
 * Unsafe adaptor
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class UnsafeHolder {
    private static final Unsafe U;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            setAccessible(theUnsafe);
            U = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionOperationException(e);
        }
    }

    public static Unsafe getUnsafe() {
        return U;
    }
}
