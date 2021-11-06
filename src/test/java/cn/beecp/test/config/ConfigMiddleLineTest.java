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
import cn.beecp.test.TestCase;

import java.net.URL;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ConfigMiddleLineTest extends TestCase {
    public void test() throws Exception {
        String filename = "config1.properties";
        URL url = ConfigMiddleLineTest.class.getResource(filename);
        url = ConfigMiddleLineTest.class.getClassLoader().getResource(filename);

        BeeDataSourceConfig testConfig = new BeeDataSourceConfig();
        testConfig.loadFromPropertiesFile(url.getFile());

        if (!"test1".equals(testConfig.getDefaultCatalog()))
            throw new BeeDataSourceConfigException("defaultCatalog error");
        if (!testConfig.isDefaultAutoCommit()) throw new BeeDataSourceConfigException("defaultAutoCommit error");
        if (!testConfig.isFairMode()) throw new BeeDataSourceConfigException("fairMode error");
        if (testConfig.getInitialSize() != 1) throw new BeeDataSourceConfigException("initialSize error");
        if (testConfig.getMaxActive() != 10) throw new BeeDataSourceConfigException("maxActive error");
    }
}