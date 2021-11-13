/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.sql.Connection.*;

/**
 * Transaction Isolation Level
 *
 * @author Chris.Liao
 */

public final class TransactionIsolationLevel {

    public final static String LEVEL_NONE = "NONE";

    public final static String LEVEL_READ_COMMITTED = "READ_COMMITTED";

    public final static String LEVEL_READ_UNCOMMITTED = "READ_UNCOMMITTED";

    public final static String LEVEL_REPEATABLE_READ = "REPEATABLE_READ";

    public final static String LEVEL_SERIALIZABLE = "SERIALIZABLE";

    public final static String TRANS_LEVEL_CODE_LIST = String.valueOf(TRANSACTION_NONE) + "," +
            TRANSACTION_READ_COMMITTED + "," +
            TRANSACTION_READ_UNCOMMITTED + "," +
            TRANSACTION_REPEATABLE_READ + "," +
            TRANSACTION_SERIALIZABLE;

    private final static Map<String, Integer> IsolationLevelMap = new HashMap<String, Integer>(5);

    static {
        IsolationLevelMap.put(LEVEL_NONE, TRANSACTION_NONE);
        IsolationLevelMap.put(LEVEL_READ_COMMITTED, TRANSACTION_READ_COMMITTED);
        IsolationLevelMap.put(LEVEL_READ_UNCOMMITTED, TRANSACTION_READ_UNCOMMITTED);
        IsolationLevelMap.put(LEVEL_REPEATABLE_READ, TRANSACTION_REPEATABLE_READ);
        IsolationLevelMap.put(LEVEL_SERIALIZABLE, TRANSACTION_SERIALIZABLE);
    }

    public final static int getTransactionIsolationCode(String name) {
        Integer code = IsolationLevelMap.get(name.toUpperCase(Locale.US));
        return (code != null) ? code : -999;
    }

    public final static boolean isValidTransactionIsolationCode(int code) {
        return IsolationLevelMap.containsValue(code);
    }
}
