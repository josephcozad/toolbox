package com.jc.app.data.domain;

import com.jc.db.JDBCDriverInfo;

public interface AppDatabase {

   public Long getId();

   public void setId(Long id);

   public String getName();

   public void setName(String name);

   public String getIp();

   public void setIp(String ip);

   public String getUsername();

   public void setUsername(String username);

   public String getPassword();

   public void setPassword(String password);

   public String getSchema();

   public void setSchema(String schema);

   public boolean isEnabled();

   public void setEnabled(boolean enabled);

   public void setDatasource(String datasource);

   public String getDatasource();

   //  public void setJDBCDriverInfo(JDBCDriverInfo driverInfo);

   public JDBCDriverInfo getJDBCDriverInfo();
}
