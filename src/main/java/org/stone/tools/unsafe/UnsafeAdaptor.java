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

import java.lang.reflect.Field;

/**
 * Unsafe Adaptor interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface UnsafeAdaptor {

    //****************************************************************************************************************//
    //                                            field offset                                                        //
    //****************************************************************************************************************//

    long objectFieldOffset(Field field);

    long staticFieldOffset(Field field);

    Object staticFieldBase(Field field);

    //****************************************************************************************************************//
    //                                          methods of int type                                                   //
    //****************************************************************************************************************//
    int getIntVolatile(Object object, long offset);

    void putIntVolatile(Object object, long offset, int update);

    void putOrderedInt(Object object, long offset, int update);

    boolean compareAndSwapInt(Object object, long offset, int expect, int update);

    //****************************************************************************************************************//
    //                                           methods of long type                                                 //
    //****************************************************************************************************************//
    long getLongVolatile(Object object, long offset);

    void putOrderedLong(Object object, long offset, long update);

    void putLongVolatile(Object object, long offset, long update);

    boolean compareAndSwapLong(Object object, long offset, long expect, long update);

    //****************************************************************************************************************//
    //                                            methods of long type                                                //
    //****************************************************************************************************************//
    Object getObjectVolatile(Object object, long offset);

    void putOrderedObject(Object object, long offset, Object update);

    void putObjectVolatile(Object object, long offset, Object update);

    boolean compareAndSwapObject(Object object, long offset, Object expect, Object update);
}
