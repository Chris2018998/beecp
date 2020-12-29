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
package cn.beecp.boot.datasource;

import cn.beecp.BeeDataSourceConfig;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/*
 *  Bee Data Source Attribute Set Factory
 *
 *  spring.datasource.d1.poolName=BeeCP1
 *  spring.datasource.d1.username=root
 *  spring.datasource.d1.password=root
 *  spring.datasource.d1.jdbcUrl=jdbc:mysql://localhost:3306/test
 *  spring.datasource.d1.driverClassName=com.mysql.cj.jdbc.Driver
 *
 *  @author Chris.Liao
 */

public class BeeDataSourceSetFactory extends BaseDataSourceSetFactory {

    /**
     * return config field
     */
    public Field[] getConfigFields() {
        List<Field> attributeList = new LinkedList<Field>();
        Class configClass = BeeDataSourceConfig.class;
        Field[] fields = configClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("checked".equals(fieldName)
                    || "connectionFactory".equals(fieldName))
                continue;
            attributeList.add(field);
        }

        return attributeList.toArray(new Field[attributeList.size()]);
    }

    /**
     * get Properties values from environment and set to dataSource
     *
     * @param ds             dataSource
     * @param field          attributeFiled
     * @param attributeValue SpringBoot environment
     * @throws Exception when fail to set
     */
    protected void setAttribute(Object ds, Field field, String attributeValue, Environment environment) throws Exception {
        if ("connectProperties".equals(field.getName())) {
            Properties connectProperties = new Properties();
            attributeValue = attributeValue.trim();
            String[] attributeArray = attributeValue.split(";");
            for (String attribute : attributeArray) {
                String[] pairs = attribute.split("=");
                if (pairs.length == 2)
                    connectProperties.put(pairs[0].trim(), pairs[1].trim());
            }
            field.set(ds, new Object[]{connectProperties});
        }
    }
}
