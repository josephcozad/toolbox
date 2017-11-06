package com.jc.app.data.domain;

import java.io.Serializable;

import javax.persistence.Transient;

import com.jc.db.JDBCDriverInfo;
import com.jc.db.MySQLDriverInfo;
import com.jc.db.dao.DAOTransaction;
import com.jc.util.ConfigInfo;

public class GenericDatabase implements AppDatabase, Serializable, Comparable<GenericDatabase> {

   private Long id;
   private String name;
   private String ip;
   private String username;
   private String password;
   private String schema;
   private boolean enabled;

   public GenericDatabase() {}

   public GenericDatabase(Long id) {
      this.id = id;
   }

   public GenericDatabase(ConfigInfo configInfo) {
      String dpIp = configInfo.getProperty("serviceAccount.dbIp");
      String schema = configInfo.getProperty("serviceAccount.schema");
      String pwd = configInfo.getProperty("serviceAccount.pwd");
      String uid = configInfo.getProperty("serviceAccount.uid");

      this.setEnabled(true); // assume so...
      this.setId(-2l);
      this.setDatasource(DAOTransaction.DEFAULT_DATASOURCE);
      this.setIp(dpIp);
      this.setName("SERVICE_ACCOUNT_DB");
      this.setPassword(pwd);
      this.setSchema(schema);
      this.setUsername(uid);
   }

   @Override
   public Long getId() {
      return id;
   }

   @Override
   public void setId(Long id) {
      this.id = id;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public void setName(String name) {
      this.name = name;
   }

   @Override
   public String getIp() {
      return ip;
   }

   @Override
   public void setIp(String ip) {
      this.ip = ip;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public void setUsername(String username) {
      this.username = username;
   }

   @Override
   public String getPassword() {
      return password;
   }

   @Override
   public void setPassword(String password) {
      this.password = password;
   }

   @Override
   public String getSchema() {
      return schema;
   }

   @Override
   public void setSchema(String schema) {
      this.schema = schema;
   }

   @Override
   public boolean isEnabled() {
      return enabled;
   }

   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (enabled ? 1231 : 1237);
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((ip == null) ? 0 : ip.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((password == null) ? 0 : password.hashCode());
      result = prime * result + ((schema == null) ? 0 : schema.hashCode());
      result = prime * result + ((username == null) ? 0 : username.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof GenericDatabase)) {
         return false;
      }
      GenericDatabase other = (GenericDatabase) obj;
      if (enabled != other.enabled) {
         return false;
      }
      if (id == null) {
         if (other.id != null) {
            return false;
         }
      }
      else if (!id.equals(other.id)) {
         return false;
      }
      if (ip == null) {
         if (other.ip != null) {
            return false;
         }
      }
      else if (!ip.equals(other.ip)) {
         return false;
      }
      if (name == null) {
         if (other.name != null) {
            return false;
         }
      }
      else if (!name.equals(other.name)) {
         return false;
      }
      if (password == null) {
         if (other.password != null) {
            return false;
         }
      }
      else if (!password.equals(other.password)) {
         return false;
      }
      if (schema == null) {
         if (other.schema != null) {
            return false;
         }
      }
      else if (!schema.equals(other.schema)) {
         return false;
      }
      if (username == null) {
         if (other.username != null) {
            return false;
         }
      }
      else if (!username.equals(other.username)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return getClass().getName() + "[ name=" + getName() + " ]";
   }

   @Override
   public int compareTo(GenericDatabase other) {
      return (this.getName()).compareToIgnoreCase(other.getName());
   }

   // -------------------------- TRANSIENT ---------------------------

   @Transient
   private String datasource;

   @Transient
   private JDBCDriverInfo driverInfo;

   @Override
   public void setDatasource(String datasource) {
      this.datasource = datasource;
   }

   @Override
   public String getDatasource() {
      return datasource;
   }

   @Override
   public JDBCDriverInfo getJDBCDriverInfo() {
      if (driverInfo == null) {
         driverInfo = new MySQLDriverInfo(getIp(), getSchema());
      }
      return driverInfo;
   }
}
