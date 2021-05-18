package cn.beecp.test;

import cn.beecp.BeeDataSource;
import cn.beecp.BeeDataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class DbDownTest {
    public static String driver = "com.mysql.jdbc.Driver";
    public static String url = "jdbc:mysql://localhost/test?connectTimeout=1000&socketTimeout=1000";
    public static String user = "root";
    public static String password = "";
    public static int size = 5;
    private static DataSource beeDs;
    private static DataSource hikariDs;
    private static Logger log = LoggerFactory.getLogger(DbDownTest.class);

    public static void main(String[] args) throws Exception {
        beeDs = createBeeCP();
        hikariDs = createHikari();
        new Timer(true).schedule(new TestTask("beeDs",beeDs), 5000, 2000);
        new Timer(true).schedule(new TestTask("hikariDs",hikariDs), 5000, 2000);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(300));
    }

    public static DataSource createBeeCP() {
        BeeDataSourceConfig config = new BeeDataSourceConfig(driver, url, user, password);
        config.setInitialSize(size);
        config.setMaxActive(size);
        config.setMaxWait(5000);
        config.setIdleCheckTimeInterval(30000);
        return new BeeDataSource(config);
    }

    public static DataSource createHikari() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(size);
        config.setMinimumIdle(size);
        return new HikariDataSource(config);
    }

    static final class TestTask extends TimerTask {
        private String name;
        private DataSource ds;
        public TestTask(String name,DataSource ds) {
            this.ds = ds;
            this.name=name;
        }

        public void run() {
            Connection con = null;
            long startTime = System.currentTimeMillis();
            try {
                con = ds.getConnection();
                PreparedStatement ps = null;
                try {
                    ps = con.prepareStatement("select 1 from dual");
                    ps.execute();
                } catch (Exception e) {
                } finally {
                    if (ps != null) try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
            } catch (Throwable e) {
                log.error("{}-Failed to getConnection,took time:{}ms",name,System.currentTimeMillis() - startTime);
            } finally {
                if (con != null) try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
    }
}





