package org.stone.beecp.issue.HikariCP.issue2075;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.ProxyConnectionBase;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 1)Test Env:
 * Java: Java-1.8.0_65-b17
 * JDBC-Pool: stone-1.2.8
 * DB: derby-10.14.2.0.jar(engine)
 * Driver: derbytools-10.14.2.0.jar
 * <p>
 * 2)Test Machine
 * Os:Win7_64,CPU:2.6Hz*4(I5-4210M),Memory(12G)
 * <p>
 * 3)Test info
 * Chris Liao(Stone project owner)
 * Test Date: 2024/02/25 in China
 * Test Result:Passed
 */

public class NClobCreateTestOnDerby {

    public static void main(String[] args) throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setDefaultAutoCommit(false);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setUsername("test");
        config.setPassword("test");
        config.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        config.setJdbcUrl("jdbc:derby:testDB;create=true");
        BeeDataSource ds = new BeeDataSource(config);

        Field rawField = ProxyConnectionBase.class.getDeclaredField("raw");
        rawField.setAccessible(true);

        //test desc: test connection is whether broken when failed to create NClob
        Connection con1 = null;
        Object rawCon1 = null;

        try {
            con1 = ds.getConnection();
            rawCon1 = rawField.get(con1);
            con1.createNClob();//SQLFeatureNotSupportedException will thrown,because this feature not implemented in derby
        } catch (SQLException e) {//failed on HikariCP,because of a  ERROR 0A000(hardcode in source code)
            e.printStackTrace();
        } finally {
            if (con1 != null) con1.close();
        }

        Connection con2 = null;
        Object rawCon2 = null;
        try {
            con2 = ds.getConnection();
            rawCon2 = rawField.get(con2);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con2 != null) con2.close();
        }

        if (rawCon1 != rawCon2) throw new SQLException("Connection has broken");
        System.out.println("Test case passed for creating NClob on derby");
    }

}
