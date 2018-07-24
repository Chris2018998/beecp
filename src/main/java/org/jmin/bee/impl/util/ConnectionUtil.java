/*
 * Copyright (C) Chris Liao
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
package org.jmin.bee.impl.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Util class to close database connection,Statement,ResultSet.
 * 
 * @author Chris
 */

public final class ConnectionUtil {

	/**
	 * getTimeMillis
	 */
	public static long getTimeMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * getNanoTime
	 */
	public static long getNanoTime() {
		return System.nanoTime();
	}

	/**
	 * check string is whether empty or null
	 */
	public static boolean isNull(String value) {
		return (value == null || value.trim().length() == 0);
	}

	/**
	 * close connectio
	 */
	public static void close(Connection connection) {
		try {
			if (connection != null)
				connection.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * close statement
	 */
	public static void close(Statement statement) {
		try {
			if (statement != null)
				statement.close();
		} catch (Throwable e) {
		}
	}

	/**
	 * close result set
	 */
	public static void close(ResultSet resultSet) {
		try {
			if (resultSet != null)
				resultSet.close();
		} catch (Throwable e) {
		}
	}

	/**
	 * close InputStreamt
	 */
	public static void close(Reader reader) {
		try {
			if (reader != null)
				reader.close();
		} catch (Throwable e) {
		}
	}

	/**
	 * close InputStreamt
	 */
	public static void close(Writer writer) {
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable e) {
		}
	}

	/**
	 * close InputStreamt
	 */
	public static void close(InputStream inputStream) {
		try {
			if (inputStream != null)
				inputStream.close();
		} catch (Throwable e) {
		}
	}

	/**
	 * close outputStream
	 */
	public static void close(OutputStream outputStream) {
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Throwable e) {
		}
	}
}
