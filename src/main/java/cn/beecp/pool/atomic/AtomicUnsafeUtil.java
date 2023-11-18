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

import static cn.beecp.pool.PoolStaticCenter.CommonLog;

/**
 * Atomic Unsafe Util
 *
 * @author Chris.Liao
 * @version 1.0
 */
class AtomicUnsafeUtil {
    static Unsafe unsafe;

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
            CommonLog.error("Unable to load unsafe", e);
        }
    }

//    static Unsafe getUnsafe() {
//        return AtomicUnsafeUtil.unsafe;
//    }
}
