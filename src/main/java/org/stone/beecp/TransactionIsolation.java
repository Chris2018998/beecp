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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.sql.Connection.*;

/**
 * Transaction Isolation Level
 *
 * @author Chris Liao
 */
public final class TransactionIsolation {

    public static final String LEVEL_NONE = "NONE";

    public static final String LEVEL_READ_COMMITTED = "READ_COMMITTED";

    public static final String LEVEL_READ_UNCOMMITTED = "READ_UNCOMMITTED";

    public static final String LEVEL_REPEATABLE_READ = "REPEATABLE_READ";

    public static final String LEVEL_SERIALIZABLE = "SERIALIZABLE";

    static final String TRANS_LEVEL_CODE_LIST = TRANSACTION_NONE + "," +
            TRANSACTION_READ_COMMITTED + "," +
            TRANSACTION_READ_UNCOMMITTED + "," +
            TRANSACTION_REPEATABLE_READ + "," +
            TRANSACTION_SERIALIZABLE;

    private static final Map<String, Integer> IsolationLevelMap = new HashMap<String, Integer>(5);

    static {
        IsolationLevelMap.put(LEVEL_NONE, TRANSACTION_NONE);
        IsolationLevelMap.put(LEVEL_READ_COMMITTED, TRANSACTION_READ_COMMITTED);
        IsolationLevelMap.put(LEVEL_READ_UNCOMMITTED, TRANSACTION_READ_UNCOMMITTED);
        IsolationLevelMap.put(LEVEL_REPEATABLE_READ, TRANSACTION_REPEATABLE_READ);
        IsolationLevelMap.put(LEVEL_SERIALIZABLE, TRANSACTION_SERIALIZABLE);
    }

    static Integer getTransactionIsolationCode(String name) {
        return IsolationLevelMap.get(name.toUpperCase(Locale.US));
    }
}
