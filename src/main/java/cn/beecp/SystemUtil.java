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
package cn.beecp.boot;

/*
 *  Util
 *
 *  @author Chris.Liao
 */
public class SystemUtil {

    //Spring dataSource configuration prefix-key name
    public static final String Spring_DS_Prefix = "spring.datasource";

    //Spring dataSource configuration key name
    public static final String Spring_DS_KEY_NameList = "nameList";

    //Spring jndi dataSource configuration key name
    public static final String Spring_DS_KEY_Jndi = "jndiName";

    //indicator:Spring dataSource register as primary datasource
    public static final String Spring_DS_KEY_Primary = "primary";

    //Datasource class name
    public static final String Spring_DS_KEY_DatasourceType = "datasourceType";

    //Datasource attribute set factory
    public static final String Spring_DS_KEY_PropertySetFactory = "propertySetFactory";

    //Default DataSourceName
    public static final String Default_DS_Class_Name = "cn.beecp.BeeDataSource";

    //Separator MiddleLine
    public static final String Separator_MiddleLine = "-";

    //Separator UnderLine
    public static final String Separator_UnderLine = "_";


    public static final boolean isBlank(String str) {
        if (str == null) return true;
        int strLen = str.length();
        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static final String propertyToField(String property, String separator) {
        if (property == null) {
            return "";
        }

        char[] chars = property.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                sb.append(separator + Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
