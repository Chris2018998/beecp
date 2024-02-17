/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.atomic;

import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Atomic Integer Field Updater Implementation(Don't use in other place)
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
    private final static Unsafe unsafe = AtomicUnsafeUtil.unsafe;
    private final long offset;

    private AtomicIntegerFieldUpdaterImpl(long offset) {
        this.offset = offset;
    }

    public static <T> AtomicIntegerFieldUpdater<T> newUpdater(Class<T> beanClass, String fieldName) {
        try {
            return new AtomicIntegerFieldUpdaterImpl<T>(unsafe.objectFieldOffset(beanClass.getDeclaredField(fieldName)));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw e;
        } catch (Throwable e) {
            return AtomicIntegerFieldUpdater.newUpdater(beanClass, fieldName);
        }
    }

    public final boolean compareAndSet(T bean, int expect, int update) {
        return unsafe.compareAndSwapInt(bean, this.offset, expect, update);
    }

    public final boolean weakCompareAndSet(T bean, int expect, int update) {
        return unsafe.compareAndSwapInt(bean, this.offset, expect, update);
    }

    public final void set(T bean, int newValue) {
        unsafe.putIntVolatile(bean, this.offset, newValue);
    }

    public final void lazySet(T bean, int newValue) {
        unsafe.putOrderedInt(bean, this.offset, newValue);
    }

    public final int get(T bean) {
        return unsafe.getIntVolatile(bean, this.offset);
    }
}