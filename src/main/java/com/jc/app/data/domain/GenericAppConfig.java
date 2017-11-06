package com.jc.app.data.domain;

public class GenericAppConfig implements AppConfig {

   private Long id;
   private String key;
   private String value;
   private boolean active;

   public GenericAppConfig() {}

   public GenericAppConfig(Long id) {
      this.id = id;
   }

   public GenericAppConfig(String key, String value) {
      this.key = key;
      this.value = value;
      this.active = true;
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
   public String getKey() {
      return key;
   }

   @Override
   public void setKey(String key) {
      this.key = key;
   }

   @Override
   public String getValue() {
      return value;
   }

   @Override
   public void setValue(String value) {
      this.value = value;
   }

   @Override
   public boolean isActive() {
      return active;
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (active ? 1231 : 1237);
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
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
      if (!(obj instanceof GenericAppConfig)) {
         return false;
      }
      GenericAppConfig other = (GenericAppConfig) obj;
      if (active != other.active) {
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
      if (key == null) {
         if (other.key != null) {
            return false;
         }
      }
      else if (!key.equals(other.key)) {
         return false;
      }
      if (value == null) {
         if (other.value != null) {
            return false;
         }
      }
      else if (!value.equals(other.value)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return getClass().getName() + "[ key=" + getKey() + "/value=" + getValue() + " ]";
   }
}
