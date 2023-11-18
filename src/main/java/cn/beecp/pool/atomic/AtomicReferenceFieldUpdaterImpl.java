/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.atomic;

import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Atomic Reference Field Updater Implementation(Don't use in other place)
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class AtomicReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
    private final static Unsafe unsafe = AtomicUnsafeUtil.unsafe;
    private final long offset;
    private final Class<V> fieldType;

    private AtomicReferenceFieldUpdaterImpl(long offset, Class<V> fieldType) {
        this.offset = offset;
        this.fieldType = fieldType;
    }

    public static <T, V> AtomicReferenceFieldUpdater<T, V> newUpdater(Class<T> beanClass, Class<V> fieldType, String fieldName) {
        try {
            return new AtomicReferenceFieldUpdaterImpl<T, V>(unsafe.objectFieldOffset(beanClass.getDeclaredField(fieldName)), fieldType);
        } catch (Throwable e) {
            return AtomicReferenceFieldUpdater.newUpdater(beanClass, fieldType, fieldName);
        }
    }

    public final boolean compareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
    }

    public final boolean weakCompareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSwapObject(bean, this.offset, expect, update);
    }

    public final void set(T bean, V newValue) {
        unsafe.putObjectVolatile(bean, this.offset, newValue);
    }

    public final void lazySet(T bean, V newValue) {
        unsafe.putOrderedObject(bean, this.offset, newValue);
    }

    public final V get(T bean) {
        return fieldType.cast(unsafe.getObjectVolatile(bean, this.offset));
    }
}