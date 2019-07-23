package com.example.click_me.util;

import com.example.click_me.entity.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil {
    private static String driver = "com.mysql.jdbc.Driver";
    private static String user = "root";// 用户名
    private static String password = "Android123";// 密码

    //连接数据库
    private static Connection getConn(String dbName){

        Connection connection = null;
        try{
            Class.forName(driver);// 动态加载类
            String ip = "rm-uf6spv68ei53668q7ao.mysql.rds.aliyuncs.com";// 写成本机地址，不能写成localhost，同时手机和电脑连接的网络必须是同一个

            // 尝试建立到给定数据库URL的连接
            connection = DriverManager.getConnection("jdbc:mysql://" + ip + ":3306/" + dbName,
                    user, password);

        }catch (SQLException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return connection;
    }

    public static User queryUser(String id) throws SQLException{
        Connection connection = getConn("schindler");
        if (connection != null) {
            String sql = "select * from test where Id=?";
            PreparedStatement ps = connection.prepareStatement(sql);
            if (ps != null) {
                ps.setString(1,id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getString("Id"));
                    user.setName(rs.getString("name"));
                    ps.close();
                    connection.close();
                    return user;
                } else {
                    ps.close();
                    connection.close();
                    return null;
                }
            } else {
                throw new SQLException();
            }
        } else {
            throw new SQLException();
        }
    }
}
