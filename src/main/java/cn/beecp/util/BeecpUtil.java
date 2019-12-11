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
package cn.beecp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Util class to close database connection,Statement,ResultSet.
 *
 * @author Chris.Liao
 */
public final class BeecpUtil {
	private static Logger log = LoggerFactory.getLogger(BeecpUtil.class);

	public static void oclose(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (Throwable e) {
				log.debug("Error at closing resultSet:", e);
			}
		}
	}

	public static void oclose(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (Throwable e) {
				log.debug("Error at closing statement:", e);
			}
		}
	}

	public static void oclose(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (Throwable e) {
				log.debug("Error at closing connection:", e);
			}
		}
	}

	public static boolean isNullText(String value) {
		return (value == null || value.trim().length() == 0);
	}

	public static boolean equalsText(String a, String b) {
		return (a==null &&b==null)||(a!= null && a.equals(b));
	}
}
