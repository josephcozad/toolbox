package com.jc.db;

import java.io.Serializable;

public class MySQLDriverInfo implements JDBCDriverInfo, Serializable {

   private static final long serialVersionUID = -7471054844472476315L;

   private final String ip;
   private final String schema;

   public MySQLDriverInfo(String ip, String schema) {
      this.ip = ip;
      this.schema = schema;
   }

   @Override
   public String getJDBCDriver() {
      String jdbcDriver = "com.mysql.jdbc.Driver";
      return jdbcDriver;
   }

   @Override
   public String getJDBCUrl() {
      String dbUrl = "jdbc:mysql://" + ip + "/" + schema + "?zeroDateTimeBehavior=convertToNull";
      return dbUrl;
   }

}
