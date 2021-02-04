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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
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

    public DataSource create(BeeDataSourceConfig config) {
        return new BeeDataSource(config);
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
            RefAddr ra = ref.get(propertyName);
            if (ra == null) ra = ref.get(propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_MiddleLine));
            if (ra == null) ra = ref.get(propertyNameToFieldId(propertyName, DS_Config_Prop_Separator_UnderLine));
            if (ra == null) continue;

            String configVal = ra.getContent().toString();
            if (isBlank(configVal)) continue;
            setValueMap.put(propertyName, configVal.trim());
        }
        //5:inject found config value to ds config object
        setPropertiesValue(config, setMethodMap, setValueMap);

        //6:try to find 'connectProperties' config value and put to ds config object
        String connectPropName = "connectProperties";
        RefAddr ra = ref.get(connectPropName);
        if (ra == null) ra = ref.get(propertyNameToFieldId(connectPropName, DS_Config_Prop_Separator_MiddleLine));
        if (ra == null) ra = ref.get(propertyNameToFieldId(connectPropName, DS_Config_Prop_Separator_UnderLine));
        String configVal = ra.getContent().toString();
        if (!isBlank(configVal)) {
            configVal = configVal.trim();
            String[] attributeArray = configVal.split("&");
            for (String attribute : attributeArray) {
                String[] pairs = attribute.split("=");
                if (pairs.length == 2)
                    config.addConnectProperty(pairs[0].trim(), pairs[1].trim());
            }
        }

        //7:create dataSource by config
        return new BeeDataSource(config);
    }
}
