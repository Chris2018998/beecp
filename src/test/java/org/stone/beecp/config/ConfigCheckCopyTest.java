/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.config;

import org.stone.base.TestCase;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.JdbcConfig;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class ConfigCheckCopyTest extends TestCase {

    private boolean equals(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return deepEquals0(a, b);
    }

    private boolean deepEquals0(Object e1, Object e2) {
        assert e1 != null;
        boolean eq;
        if (e1 instanceof Object[] && e2 instanceof Object[])
            eq = equals(e1, e2);
        else if (e1 instanceof byte[] && e2 instanceof byte[])
            eq = Arrays.equals((byte[]) e1, (byte[]) e2);
        else if (e1 instanceof short[] && e2 instanceof short[])
            eq = Arrays.equals((short[]) e1, (short[]) e2);
        else if (e1 instanceof int[] && e2 instanceof int[])
            eq = Arrays.equals((int[]) e1, (int[]) e2);
        else if (e1 instanceof long[] && e2 instanceof long[])
            eq = Arrays.equals((long[]) e1, (long[]) e2);
        else if (e1 instanceof char[] && e2 instanceof char[])
            eq = Arrays.equals((char[]) e1, (char[]) e2);
        else if (e1 instanceof float[] && e2 instanceof float[])
            eq = Arrays.equals((float[]) e1, (float[]) e2);
        else if (e1 instanceof double[] && e2 instanceof double[])
            eq = Arrays.equals((double[]) e1, (double[]) e2);
        else if (e1 instanceof boolean[] && e2 instanceof boolean[])
            eq = Arrays.equals((boolean[]) e1, (boolean[]) e2);
        else
            eq = e1.equals(e2);
        return eq;
    }

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        String url = JdbcConfig.JDBC_URL;
        config.setJdbcUrl(url);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        BeeDataSourceConfig config2 = config.check();

        if (config2 == config) throw new BeeDataSourceConfigException("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<String>();
        excludeNames.add("poolName");
        excludeNames.add("connectProperties");
        excludeNames.add("connectionFactory");
        excludeNames.add("threadFactory");

        //1:primitive type copy
        Field[] fields = BeeDataSourceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if (!excludeNames.contains(field.getName())) {
                field.setAccessible(true);
                if (!equals(field.get(config), field.get(config2))) {
                    throw new BeeDataSourceConfigException("Failed to copy field[" + field.getName() + "]value is not equalsString");
                }
            }
        }

        //2:test 'connectProperties'
        Field connectPropertiesField = BeeDataSourceConfig.class.getDeclaredField("connectProperties");
        connectPropertiesField.setAccessible(true);
        if (connectPropertiesField.get(config) == connectPropertiesField.get(config2))
            throw new BeeDataSourceConfigException("Configuration connectProperties check copy failed");
        if (!equals(connectPropertiesField.get(config), connectPropertiesField.get(config2)))
            throw new BeeDataSourceConfigException("Configuration connectProperties check copy failed");
    }
}