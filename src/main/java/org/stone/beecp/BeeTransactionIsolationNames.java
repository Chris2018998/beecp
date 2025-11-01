/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Transaction Isolation Names.
 *
 * @author Chris Liao
 */
public final class BeeTransactionIsolationNames {

    public static final String TRANSACTION_NONE = "NONE";

    public static final String TRANSACTION_READ_COMMITTED = "READ_COMMITTED";

    public static final String TRANSACTION_READ_UNCOMMITTED = "READ_UNCOMMITTED";

    public static final String TRANSACTION_REPEATABLE_READ = "REPEATABLE_READ";

    public static final String TRANSACTION_SERIALIZABLE = "SERIALIZABLE";

    static final String TRANS_ISOLATION_CODE_LIST =
            Connection.TRANSACTION_NONE + "," +
                    Connection.TRANSACTION_READ_COMMITTED + "," +
                    Connection.TRANSACTION_READ_UNCOMMITTED + "," +
                    Connection.TRANSACTION_REPEATABLE_READ + "," +
                    Connection.TRANSACTION_SERIALIZABLE;

    private static final Map<String, Integer> IsolationNameToCodeMap = new HashMap<>(5);

    static {
        IsolationNameToCodeMap.put(TRANSACTION_NONE, Integer.valueOf(Connection.TRANSACTION_NONE));
        IsolationNameToCodeMap.put(TRANSACTION_READ_COMMITTED, Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED));
        IsolationNameToCodeMap.put(TRANSACTION_READ_UNCOMMITTED, Integer.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));
        IsolationNameToCodeMap.put(TRANSACTION_REPEATABLE_READ, Integer.valueOf(Connection.TRANSACTION_REPEATABLE_READ));
        IsolationNameToCodeMap.put(TRANSACTION_SERIALIZABLE, Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE));
    }

    private BeeTransactionIsolationNames() {
    }

    static Integer getTransactionIsolationCode(String name) {
        return IsolationNameToCodeMap.get(name.toUpperCase(Locale.US));
    }
}
