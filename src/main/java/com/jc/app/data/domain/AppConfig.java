package com.jc.app.data.domain;

public interface AppConfig {

   public Long getId();

   public void setId(Long id);

   public String getKey();

   public void setKey(String key);

   public String getValue();

   public void setValue(String value);

   public boolean isActive();

   public void setActive(boolean active);
}
