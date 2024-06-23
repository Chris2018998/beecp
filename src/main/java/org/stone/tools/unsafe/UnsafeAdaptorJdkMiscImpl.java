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

import org.stone.tools.exception.ReflectionOperationException;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.stone.tools.BeanUtil.setAccessible;

/**
 * An Unsafe adaptor,whose inside unsafe field type should be declared to
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
            setAccessible(theUnsafe);
            U = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionOperationException(e);
        }
    }

    //****************************************************************************************************************//
    //                                          methods of int type                                                   //
    //****************************************************************************************************************//
    public long objectFieldOffset(Field field) {
        if (!field.isAccessible()) setAccessible(field);
        return U.objectFieldOffset(field);
    }

    public long staticFieldOffset(Field field) {
        if (!field.isAccessible()) setAccessible(field);
        return U.staticFieldOffset(field);
    }

    public Object staticFieldBase(Field field) {
        if (!field.isAccessible()) setAccessible(field);
        return U.staticFieldBase(field);
    }

    //****************************************************************************************************************//
    //                                          methods of int type                                                   //
    //****************************************************************************************************************//
    public int getIntVolatile(Object object, long offset) {
        return U.getIntVolatile(object, offset);
    }

    public void putIntVolatile(Object object, long offset, int update) {
        U.putIntVolatile(object, offset, update);
    }

    public void putOrderedInt(Object object, long offset, int update) {
        U.putOrderedInt(object, offset, update);
    }

    public boolean compareAndSwapInt(Object object, long offset, int expect, int update) {
        return U.compareAndSwapInt(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                         methods of long type                                                 //
    //****************************************************************************************************************//
    public long getLongVolatile(Object object, long offset) {
        return U.getLongVolatile(object, offset);
    }

    public void putLongVolatile(Object object, long offset, long update) {
        U.putLongVolatile(object, offset, update);
    }

    public void putOrderedLong(Object object, long offset, long update) {
        U.putOrderedLong(object, offset, update);
    }

    public boolean compareAndSwapLong(Object object, long offset, long expect, long update) {
        return U.compareAndSwapLong(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            methods of object type                                              //
    //****************************************************************************************************************//
    public Object getObjectVolatile(Object object, long offset) {
        return U.getObjectVolatile(object, offset);
    }

    public void putObjectVolatile(Object object, long offset, Object update) {
        U.putObjectVolatile(object, offset, update);
    }

    public void putOrderedObject(Object object, long offset, Object update) {
        U.putOrderedObject(object, offset, update);
    }

    public boolean compareAndSwapObject(Object object, long offset, Object expect, Object update) {
        return U.compareAndSwapObject(object, offset, expect, update);
    }
}
