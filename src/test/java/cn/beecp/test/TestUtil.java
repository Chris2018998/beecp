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
package cn.beecp.test;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import cn.beecp.pool.ConnectionPool;
import cn.beecp.pool.FastConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class TestUtil {
    private final static Logger log = LoggerFactory.getLogger(TestUtil.class);

    public static ConnectionPool getPool(final BeeDataSource ds) {
        try {
            Field field = ds.getClass().getDeclaredField("pool");
            field.setAccessible(true);
            return (ConnectionPool) field.get(ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BeeDataSourceConfig getPoolConfig(FastConnectionPool pool) {
        try {
            Field field = pool.getClass().getDeclaredField("poolConfig");
            field.setAccessible(true);
            return (BeeDataSourceConfig) field.get(pool);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertError(String message) {
        throw new AssertionError(message);
    }

    public static void assertError(String message, Object expect, Object current) {
        throw new AssertionError(String.format(message, String.valueOf(expect), String.valueOf(current)));
    }

    public final static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing resultSet:", e);
        }
    }

    public final static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing statement:", e);
        }
    }

    public final static void oclose(Connection c) {
        try {
            c.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing connection:", e);
        }
    }

}
