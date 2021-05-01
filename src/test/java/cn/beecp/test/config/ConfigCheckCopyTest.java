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
package cn.beecp.test.config;

import cn.beecp.BeeDataSourceConfig;
import cn.beecp.BeeDataSourceConfigException;
import cn.beecp.test.Config;
import cn.beecp.test.TestCase;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ConfigCheckCopyTest extends TestCase {

    public void test() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        String url = Config.JDBC_URL;
        config.setJdbcUrl(url);
        config.setDriverClassName(Config.JDBC_DRIVER);
        config.setUsername(Config.JDBC_USER);
        config.setPassword(Config.JDBC_PASSWORD);
        BeeDataSourceConfig config2 = config.check();

        if (config2 == config) throw new Exception("Configuration check copy failed");

        List<String> excludeNames = new LinkedList<>();
        excludeNames.add("connectProperties");
        excludeNames.add("connectionFactory");

        //1:primitive type copy
        Field[] fields = BeeDataSourceConfig.class.getDeclaredFields();
        for (Field field : fields) {
            if (!excludeNames.contains(field.getName())) {
                field.setAccessible(true);
                if (!Objects.deepEquals(field.get(config), field.get(config2))) {
                    throw new BeeDataSourceConfigException("Failed to copy field[" + field.getName() + "],value is not equals");
                }
            }
        }

        //2:test 'connectProperties'
        Field connectPropertiesField = BeeDataSourceConfig.class.getDeclaredField("connectProperties");
        connectPropertiesField.setAccessible(true);
        if (connectPropertiesField.get(config) == connectPropertiesField.get(config2))
            throw new Exception("Configuration connectProperties check copy failed");
        if (!Objects.deepEquals(connectPropertiesField.get(config), connectPropertiesField.get(config2)))
            throw new Exception("Configuration connectProperties check copy failed");
    }

}