/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp;

import java.sql.Connection;

/**
 * Transaction Isolation Level
 *
 * @author Chris.Liao
 */

public final class TransactionIsolationLevel {

    public final static String LEVEL_NONE = "NONE";

    public final static String LEVEL_READ_COMMITTED = "READ_COMMITTED";

    public final static String LEVEL_READ_UNCOMMITTED = "READ_UNCOMMITTED";

    public final static String LVEVL_REPEATABLE_READ = "REPEATABLE_READ";

    public final static String LEVEL_ERIALIZABLE = "SERIALIZABLE";

    public final static String TRANS_LEVEL_LIST = new StringBuilder()
            .append(LEVEL_NONE).append(",")
            .append(LEVEL_READ_COMMITTED).append(",")
            .append(LEVEL_READ_UNCOMMITTED).append(",")
            .append(LVEVL_REPEATABLE_READ).append(",")
            .append(LEVEL_ERIALIZABLE).toString();

    public final static int CODE_NONE = Connection.TRANSACTION_NONE;

    public final static int CODE_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;

    public final static int CODE_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;

    public final static int CODE_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;

    public final static int CODE_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;

    public final static int nameToCode(String name) {
        if (LEVEL_NONE.equalsIgnoreCase(name))
            return CODE_NONE;
        else if (LEVEL_READ_COMMITTED.equalsIgnoreCase(name))
            return CODE_READ_COMMITTED;
        else if (LEVEL_READ_UNCOMMITTED.equalsIgnoreCase(name))
            return CODE_READ_UNCOMMITTED;
        else if (LVEVL_REPEATABLE_READ.equalsIgnoreCase(name))
            return CODE_REPEATABLE_READ;
        else if (LEVEL_ERIALIZABLE.equalsIgnoreCase(name))
            return CODE_SERIALIZABLE;
        else
            return -999;
    }
}
