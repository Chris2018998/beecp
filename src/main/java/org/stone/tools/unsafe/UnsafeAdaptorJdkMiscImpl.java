/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * A Unsafe adaptor,whose inside unsafe field type should be declared to
 * {@code jdk.internal.misc.Unsafe} at jdk higher version(Java8)
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class UnsafeAdaptorJdkMiscImpl implements UnsafeAdaptor {
    private static final Unsafe U;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //****************************************************************************************************************//
    //                                          methods of int type                                                   //
    //****************************************************************************************************************//
    public final long objectFieldOffset(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.objectFieldOffset(field);
    }

    public final long staticFieldOffset(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.staticFieldOffset(field);
    }

    public final Object staticFieldBase(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.staticFieldBase(field);
    }

    //****************************************************************************************************************//
    //                                          methods of int type                                                   //
    //****************************************************************************************************************//
    public final int getIntVolatile(Object object, long offset) {
        return U.getIntVolatile(object, offset);
    }

    public final void putIntVolatile(Object object, long offset, int update) {
        U.putIntVolatile(object, offset, update);
    }

    public final void putOrderedInt(Object object, long offset, int update) {
        U.putOrderedInt(object, offset, update);
    }

    public final boolean compareAndSwapInt(Object object, long offset, int expect, int update) {
        return U.compareAndSwapInt(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                         methods of long type                                                 //
    //****************************************************************************************************************//
    public final long getLongVolatile(Object object, long offset) {
        return U.getLongVolatile(object, offset);
    }

    public final void putLongVolatile(Object object, long offset, long update) {
        U.putLongVolatile(object, offset, update);
    }

    public final void putOrderedLong(Object object, long offset, long update) {
        U.putOrderedLong(object, offset, update);
    }

    public final boolean compareAndSwapLong(Object object, long offset, long expect, long update) {
        return U.compareAndSwapLong(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            methods of object type                                              //
    //****************************************************************************************************************//
    public final Object getObjectVolatile(Object object, long offset) {
        return U.getObjectVolatile(object, offset);
    }

    public final void putObjectVolatile(Object object, long offset, Object update) {
        U.putObjectVolatile(object, offset, update);
    }

    public final void putOrderedObject(Object object, long offset, Object update) {
        U.putOrderedObject(object, offset, update);
    }

    public final boolean compareAndSwapObject(Object object, long offset, Object expect, Object update) {
        return U.compareAndSwapObject(object, offset, expect, update);
    }
}
