package com.jc.app.data.domain;

public interface AppRole {

   public final static String PUBLIC_USER_ROLE = "publicUser";
   public final static String SECURED_USER_ROLE = "securedUser";

   public Long getId();

   public void setId(Long id);

   public String getName();

   public void setName(String name);
}
