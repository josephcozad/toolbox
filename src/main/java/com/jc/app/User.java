package com.jc.app;

import java.io.Serializable;

import com.jc.shiro.ShiroUser;

public interface User extends ShiroUser, Serializable {

   public Long getUserId();

   public String getUsername();

   public boolean isAllowed(String permission);

   public boolean isRole(String role);

   public String getDatasource();
}
