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

/**
 * Unsafe adaptor holder
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class UnsafeAdaptorHolder {
    public static final UnsafeAdaptor U;
    private static final String SunMiscUnsafeClassName = "sun.misc.Unsafe";
    private static final String JdkMiscUnsafeClassName = "jdk.internal.misc.Unsafe";
    private static final String SunMiscUnsafeAdaptorImplClass = "org.stone.tools.unsafe.UnsafeAdaptorSunMiscImpl";//low version
    private static final String JdkMiscUnsafeAdaptorImplClass = "org.stone.tools.unsafe.UnsafeAdaptorJdkMiscImpl";//high version

    static {
        String adaptorImplClassName;
        try {
            Class.forName(JdkMiscUnsafeClassName);
            adaptorImplClassName = JdkMiscUnsafeAdaptorImplClass;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(SunMiscUnsafeClassName);
                adaptorImplClassName = SunMiscUnsafeAdaptorImplClass;
            } catch (ClassNotFoundException e2) {
                throw new Error("Failed to load Unsafe class:" + SunMiscUnsafeClassName, e);
            }
        }

        try {
            U = (UnsafeAdaptor) Class.forName(adaptorImplClassName).newInstance();
        } catch (InstantiationException e) {
            throw new Error("Failed to create unsafe adaptor by class:" + adaptorImplClassName, e);
        } catch (IllegalAccessException e) {
            throw new Error("Failed to create unsafe adaptor by class:" + adaptorImplClassName, e);
        } catch (ClassNotFoundException e) {
            throw new Error("Not found unsafe adaptor class:" + adaptorImplClassName);
        }
    }
}
