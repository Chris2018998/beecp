/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU General Public License version 3.0.
 */
package cn.beecp;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import static cn.beecp.pool.PoolStaticCenter.*;

/**
 * BeeDataSource factory
 *
 * @author Chris.Liao
 * @version 1.0
 */
public final class BeeDataSourceFactory implements ObjectFactory {

    private final static String getConfigValue(Reference ref, String propertyName) {
        String value = readConfig(ref, propertyName);
        if (isBlank(value))
            value = readConfig(ref, propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_MiddleLine));
        if (isBlank(value))
            value = readConfig(ref, propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_UnderLine));
        return value;
    }

    private final static String readConfig(Reference ref, String propertyName) {
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
     * @throws Exception if this object factory encountered an exception while
     *                   attempting to create an object, and no other object
     *                   factories are to be tried.
     * @see NamingManager#getObjectInstance
     * @see NamingManager#getURLContext
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws Exception {

        Reference ref = (Reference) obj;
        //1:create datasource config instance
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //2:get all properties set methods
        Map<String, Method> setMethodMap = getSetMethodMap(config.getClass());
        //3:create properties to collect config value
        Map<String, Object> setValueMap = new HashMap<String, Object>(setMethodMap.size());
        //4:loop to find out properties config value by set methods
        Iterator<String> iterator = setMethodMap.keySet().iterator();
        while (iterator.hasNext()) {
            String propertyName = iterator.next();
            String configVal = getConfigValue(ref, propertyName);

            if (isBlank(configVal)) continue;
            setValueMap.put(propertyName, configVal);
        }
        //5:inject found config value to ds config object
        setPropertiesValue(config, setMethodMap, setValueMap);

        //6:try to find 'connectProperties' config value and put to ds config object
        config.addConnectProperty(getConfigValue(ref, "connectProperties"));
        String connectPropertiesCount = getConfigValue(ref, "connectProperties.count");
        if (!isBlank(connectPropertiesCount)) {
            int count = 0;
            try {
                count = Integer.parseInt(connectPropertiesCount.trim());
            } catch (Throwable e) {
            }
            for (int i = 1; i <= count; i++)
                config.addConnectProperty(getConfigValue(ref, "connectProperties." + i));
        }

        //7:create dataSource by config
        return new BeeDataSource(config);
    }
}
