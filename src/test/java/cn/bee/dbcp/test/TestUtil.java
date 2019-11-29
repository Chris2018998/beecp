package cn.bee.dbcp.test;

import java.lang.reflect.Field;

import cn.bee.dbcp.BeeDataSource;
import cn.bee.dbcp.pool.ConnectionPool;

public class TestUtil {
	public static ConnectionPool getPool(final BeeDataSource ds) {
		try {
			Field field = ds.getClass().getDeclaredField("pool");
			field.setAccessible(true);
			return (ConnectionPool) field.get(ds);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
