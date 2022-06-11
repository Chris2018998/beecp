package cn.beecp.test.other;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MysqlAutoChangeTest {

    public static void main(String[]args)throws Exception{
        String driver ="com.mysql.jdbc.Driver";
        String url ="JDBC:MYSQL://localhost/test";
        String user="root";
        String password="";

        Connection con1=null;
        Statement statement1=null;

        Connection con2=null;
        Statement statement2=null;
        ResultSet resultSet2=null;
        try{
            Class.forName(driver);
            //1：建立测试性连接
            con1= DriverManager.getConnection(url,user,password);

            //2：先清除表中所有数据
            statement1=con1.createStatement();
            statement1.execute("delete from test");

            //3: AutoCommit 由false 切换为 true
            con1.setAutoCommit(false);
            String testValue="123456";
            statement1.execute("insert into test values("+testValue+")");
            con1.setAutoCommit(true);//<----会触发自动提交，不建议随意切换

            //4:建立一个新连接，检查第3步是否已经提交到库里
            con2= DriverManager.getConnection(url,user,password);
            statement2= con2.createStatement();
            resultSet2=statement2.executeQuery("select id from test where id="+testValue);
            while(resultSet2.next())
               System.out.println(resultSet2.getString("id"));
        }finally {
            if(statement1!=null)statement1.close();
            if(resultSet2!=null)resultSet2.close();
            if(statement2!=null)statement2.close();

            if(con1!=null)con1.close();
            if(con2!=null)con2.close();
        }
    }
}
