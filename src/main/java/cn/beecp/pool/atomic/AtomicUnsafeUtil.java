/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.pool.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Atomic Unsafe Util
 *
 * @author Chris.Liao
 * @version 1.0
 */
class AtomicUnsafeUtil {
    private static final Unsafe unsafe;

    static {
        try {
            unsafe = AccessController.doPrivileged(new PrivilegedExceptionAction<Unsafe>() {
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            });
        } catch (Throwable e) {
            throw new Error("Unable to load unsafe", e);
        }
    }

    public static Unsafe getUnsafe() {
        return AtomicUnsafeUtil.unsafe;
    }

    public static void parkNanos(long nanos) {
        AtomicUnsafeUtil.unsafe.park(false, nanos);
    }

    public static void unpark(Thread thread) {
        AtomicUnsafeUtil.unsafe.unpark(thread);
    }
}
