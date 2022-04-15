/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp;

import cn.beecp.jta.BeeJtaDataSource;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * BeeDataSource factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class BeeDataSourceFactory implements ObjectFactory {

    private static String getConfigValue(Reference ref, final String propertyName) {
        String value = readConfig(ref, propertyName);
        if (value != null) return value;

        String newPropertyName = propertyName.substring(0, 1).toLowerCase(Locale.US) + propertyName.substring(1);
        value = readConfig(ref, newPropertyName);
        if (value != null) return value;

        value = readConfig(ref, propertyNameToFieldId(newPropertyName, Separator_MiddleLine));
        if (value != null) return value;

        return readConfig(ref, propertyNameToFieldId(newPropertyName, Separator_UnderLine));
    }

    private static String readConfig(Reference ref, String propertyName) {
        RefAddr refAddr = ref.get(propertyName);
        if (refAddr != null) {
            Object refObject = refAddr.getContent();
            if (refObject == null) return null;
            String value = refObject.toString().trim();
            if (!isBlank(value)) {
                CommonLog.info("beecp.{}={}", propertyName, value);
                return value;
            }
        }
        return null;
    }

    /**
     * @param obj         The possibly null object containing location or reference
     *                    information that can be used in creating an object.
     * @param name        The name of this object relative to <code>nameCtx</code>, or
     *                    null if no name is specified.
     * @param nameCtx     The context relative to which the <code>name</code> parameter
     *                    is specified, or null if <code>name</code> is relative to the
     *                    default initial context.
     * @param environment The possibly null environment that is used in creating the
     *                    object.
     * @return The object created; null if an object cannot be created.
     * @see NamingManager#getObjectInstance
     * @see NamingManager#getURLContext
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference) obj;
        //1:try to lookup transactionManager if configured
        TransactionManager tm = null;
        String tmJndiName = getConfigValue(ref, CONFIG_TM_JNDI);
        if (!isBlank(tmJndiName) && nameCtx != null) {
            tm = (TransactionManager) nameCtx.lookup(tmJndiName);
        }

        //2:create dsnode config instance
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //3:get all properties set methods
        Map<String, Method> setMethodMap = getClassSetMethodMap(config.getClass());
        //4:create properties to collect config value
        Map<String, Object> setValueMap = new HashMap<String, Object>(setMethodMap.size());
        //5:loop to find out properties config value by set methods
        for (String propertyName : setMethodMap.keySet()) {
            String configVal = getConfigValue(ref, propertyName);
            if (isBlank(configVal)) continue;
            setValueMap.put(propertyName, configVal);
        }
        //6:inject found config value to ds config object
        setPropertiesValue(config, setMethodMap, setValueMap);

        //7:try to find 'connectProperties' config value and put to ds config object
        config.addConnectProperty(getConfigValue(ref, CONFIG_CONNECT_PROP));
        String connectPropertiesCount = getConfigValue(ref, CONFIG_CONNECT_PROP_SIZE);
        if (!isBlank(connectPropertiesCount)) {
            int count = Integer.parseInt(connectPropertiesCount.trim());
            for (int i = 1; i <= count; i++)
                config.addConnectProperty(getConfigValue(ref, CONFIG_CONNECT_PROP_KEY_PREFIX + i));
        }

        //8:create dataSource by config
        BeeDataSource ds = new BeeDataSource(config);
        return (tm != null) ? new BeeJtaDataSource(ds, tm) : ds;
    }
}
