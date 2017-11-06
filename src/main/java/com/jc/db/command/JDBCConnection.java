package com.jc.db.command;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCConnection implements ConnectionCommand {

   private String datasource;
   private final String username;
   private final String password;
   private final String driver;
   private String url;

   public JDBCConnection(String username, String password, String driver, String url) {
      this(username, password, driver);
      this.url = url;
   }

   public JDBCConnection(String username, String password, String driver) {
      this.username = username;
      this.password = password;
      this.driver = driver;
   }

   @Override
   public Connection createConnection() throws ClassNotFoundException, SQLException {
      Properties props = new Properties();
      props.put("user", username);
      props.put("password", password);
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, props);
      return (conn);
   }

   public String getUserName() {
      return username;
   }

   public void setDatasource(String datasource) {
      this.datasource = datasource;
   }

   public String getDatasource() {
      return datasource;
   }
}
