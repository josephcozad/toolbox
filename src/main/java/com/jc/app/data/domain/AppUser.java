package com.jc.app.data.domain;

import java.util.Date;
import java.util.List;

public abstract class AppUser {

   private Long id;
   private String username;
   private String appKey;
   private String password;
   private boolean expired;
   private int failedLogins;
   private boolean passwordReset;
   private Date createDateTime;
   private Date lastUpdateDateTime;
   private List<AppPermission> permissions;
   private List<AppRole> roles;
   private List<AppDatabase> databases;

   public AppUser() {}

   public AppUser(Long id) {
      this.id = id;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getUserName() {
      return username;
   }

   public void setUserName(String username) {
      this.username = username;
   }

   public String getAppKey() {
      return appKey;
   }

   public void setAppKey(String appKey) {
      this.appKey = appKey;
   }

   public Date getLastUpdateDateTime() {
      return lastUpdateDateTime;
   }

   public void setLastUpdateDateTime(Date lastUpdateDateTime) {
      this.lastUpdateDateTime = lastUpdateDateTime;
   }

   public Date getCreateDateTime() {
      return createDateTime;
   }

   public void setCreateDateTime(Date createDateTime) {
      this.createDateTime = createDateTime;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean isExpired() {
      return expired;
   }

   public void setExpired(boolean expired) {
      this.expired = expired;
   }

   public boolean isPasswordReset() {
      return passwordReset;
   }

   public void setPasswordReset(boolean reset) {
      this.passwordReset = reset;
   }

   public int getFailedLogins() {
      return failedLogins;
   }

   public void setFailedLogins(int numFailed) {
      this.failedLogins = numFailed;
   }

   public List<AppPermission> getPermissions() {
      return permissions;
   }

   public void setPermissions(List<AppPermission> permissions) {
      this.permissions = permissions;
   }

   public List<AppRole> getRoles() {
      return roles;
   }

   public void setRoles(List<AppRole> roles) {
      this.roles = roles;
   }

   public List<AppDatabase> getDatabases() {
      return databases;
   }

   public void setDatabases(List<AppDatabase> selectedDBs) {
      this.databases = selectedDBs;
   }

   public String getFormattedUpdateTime() {
      if (lastUpdateDateTime != null) {
         return lastUpdateDateTime.toString();
      }
      else {
         return null;
      }
   }

   public String getFormattedCreateTime() {
      if (createDateTime != null) {
         return createDateTime.toString();
      }
      else {
         return null;
      }
   }

   @Override
   public String toString() {
      return getClass().getName() + "[ id=" + id + " ]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      // TODO: Warning - this method won't work in the case the id fields are not set
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof AppUser)) {
         return false;
      }
      AppUser other = (AppUser) obj;
      if (id == null) {
         if (other.id != null) {
            return false;
         }
      }
      else if (!id.equals(other.id)) {
         return false;
      }
      return true;
   }
}
