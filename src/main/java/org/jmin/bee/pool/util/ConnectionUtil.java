/*
 * Copyright Chris Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jmin.bee.pool.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Util class to close database connection,Statement,ResultSet.
 * 
 * @author Chris
 */

public final class ConnectionUtil {
	public static boolean isNull(String value) {
		return (value == null || value.trim().length() == 0);
	}

	public static void oclose(Connection connection) {
		try {
			if (connection != null)
				connection.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void oclose(Statement statement) {
		try {
			if (statement != null)
				statement.close();
		} catch (Throwable e) {
		}
	}

	public static void oclose(ResultSet resultSet) {
		try {
			if (resultSet != null)
				resultSet.close();
		} catch (Throwable e) {
		}
	}
}
