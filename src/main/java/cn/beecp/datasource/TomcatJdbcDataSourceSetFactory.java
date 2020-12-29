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

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

/*
 *  Tomcat-JDBC DataSource Configuration Set Factory
 *
 *  @author Chris.Liao
 */

public class TomcatJdbcDataSourceSetFactory extends BaseDataSourceSetFactory {

    /**
     * return config field
     */
    public Field[] getConfigFields() {
        return PoolProperties.class.getDeclaredFields();
    }

    /**
     * get Properties values from environment and set to dataSource
     *
     * @param ds           dataSource
     * @param configPrefix configured prefix name
     * @param environment  SpringBoot environment
     * @throws Exception when fail to set
     */
    public void setAttributes(Object ds, String configPrefix, Environment environment) throws Exception {
        PoolProperties p = new PoolProperties();
        super.setAttributes(p, configPrefix, environment);
        org.apache.tomcat.jdbc.pool.DataSource tds = (org.apache.tomcat.jdbc.pool.DataSource) ds;
        tds.setPoolProperties(p);
    }
}
