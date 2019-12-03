package cn.bee.dbcp.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Some JDBC info
 * 
 * @author Administrator
 */
public class JdbcConfig {
	public static String JDBC_USER;
	public static String JDBC_PASSWORD;
	public static String JDBC_DRIVER;
	public static String JDBC_URL;
	
	public static int POOL_MAX_ACTIVE;
	public static int POOL_MIN_ACTIVE;
	public static int POOL_INIT_SIZE;
	public static int REQUEST_TIMEOUT=8000;
	public static String LINK_FILE = "JdbcConfig.properties";
	
	static{
		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void loadConfig() throws Exception {
		InputStream fileStream = null;

		try {
			fileStream = JdbcConfig.class.getClassLoader().getResourceAsStream(LINK_FILE);
			if (fileStream == null)fileStream = JdbcConfig.class.getResourceAsStream(LINK_FILE);
			if(fileStream==null)throw new IOException("Can't find file:'JdbcConfig.properties' in classpath");
			Properties prop = new Properties();
			prop.load(fileStream);

			JDBC_USER = prop.getProperty("JDBC_USER");
			JDBC_PASSWORD = prop.getProperty("JDBC_PASSWORD");
			JDBC_DRIVER = prop.getProperty("JDBC_DRIVER");
			JDBC_URL = prop.getProperty("JDBC_URL");

			POOL_MAX_ACTIVE = Integer.parseInt(prop.getProperty("POOL_MAX_ACTIVE"));
			POOL_MIN_ACTIVE = Integer.parseInt(prop.getProperty("POOL_MIN_ACTIVE"));
			POOL_INIT_SIZE = Integer.parseInt(prop.getProperty("POOL_INIT_SIZE"));
			try{
				REQUEST_TIMEOUT = Integer.parseInt(prop.getProperty("REQUEST_TIMEOUT"));
			}catch(Exception e){
			}
			
			if (JDBC_USER == null || JDBC_USER.trim().length() == 0)
				throw new Exception("'USER_ID' missed");
			if (JDBC_DRIVER == null || JDBC_DRIVER.trim().length() == 0)
				throw new Exception("'JDBC_DRIVER' missed");
			if (JDBC_URL == null || JDBC_URL.trim().length() == 0)
				throw new Exception("'JDBC_URL' missed");
			if (POOL_MAX_ACTIVE == 0)
				throw new Exception("'POOL_MAX_ACTIVE' missed");
		} finally {
			if (fileStream != null) {
				try {
					fileStream.close();
				} catch (Exception e) {
				}
			}
		}
	}
}