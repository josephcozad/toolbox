package com.jc.shiro;

import java.util.List;

public interface ShiroUser {

   public boolean resetPassword();

   public List<String> getRoles();

   public List<String> getPermissions();
}
