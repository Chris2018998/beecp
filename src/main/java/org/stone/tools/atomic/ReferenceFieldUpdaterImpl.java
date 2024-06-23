/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.atomic;

import org.stone.tools.exception.ReflectionOperationException;
import org.stone.tools.unsafe.UnsafeAdaptor;
import org.stone.tools.unsafe.UnsafeAdaptorHolder;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Atomic Reference Field Updater Implementation(Don't use in other place)
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
    private final static UnsafeAdaptor unsafe = UnsafeAdaptorHolder.U;
    private final long offset;
    private final Class<V> fieldType;

    private ReferenceFieldUpdaterImpl(long offset, Class<V> fieldType) {
        this.offset = offset;
        this.fieldType = fieldType;
    }

    public static <T, V> AtomicReferenceFieldUpdater<T, V> newUpdater(Class<T> beanClass, Class<V> fieldType, String fieldName) {
        try {
            return new ReferenceFieldUpdaterImpl<>(unsafe.objectFieldOffset(beanClass.getDeclaredField(fieldName)), fieldType);
        } catch (NoSuchFieldException e) {
            throw new ReflectionOperationException(e);
        } catch (SecurityException e) {
            throw e;
        } catch (Throwable e) {
            return AtomicReferenceFieldUpdater.newUpdater(beanClass, fieldType, fieldName);
        }
    }

    public boolean compareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
    }

    public boolean weakCompareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
    }

    public void set(T bean, V newValue) {
        unsafe.putObjectVolatile(bean, this.offset, newValue);
    }

    public void lazySet(T bean, V newValue) {
        unsafe.putOrderedObject(bean, this.offset, newValue);
    }

    public V get(T bean) {
        return fieldType.cast(unsafe.getObjectVolatile(bean, this.offset));
    }
}