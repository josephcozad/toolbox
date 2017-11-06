package com.jc.app.view;

import java.io.Serializable;

public class SelectOption implements Serializable {

   private static final long serialVersionUID = -4918854351465632968L;

   private String label; // What's displayed
   private String value; // The key by which to select option.
   private boolean disabled;
   private Object object; // Optional Object representation of the selected option.

   public SelectOption() {
      label = "";
      value = "";
      disabled = false;
   }

   public SelectOption(String l, String v) {
      label = (null == l ? "" : l);
      value = (null == v ? "" : v);
   }

   public boolean isDisabled() {
      return disabled;
   }

   public void setDisabled(boolean value) {
      disabled = value;
   }

   public String getLabel() {
      return (label);
   }

   public void setLabel(String l) {
      label = null == l ? "" : l;
   }

   public String getValue() {
      return (value);
   }

   public void setValue(String v) {
      value = null == v ? "" : v;
   }

   public Object getObject() {
      return object;
   }

   public void setObject(Object object) {
      this.object = object;
   }

   public boolean hasObject() {
      return object != null;
   }

   @Override
   public String toString() {
      return label;
      //      return "Label[" + label + "] Value[" + value + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (disabled ? 1231 : 1237);
      result = prime * result + ((label == null) ? 0 : label.hashCode());
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
      if (getClass() != obj.getClass()) {
         return false;
      }
      SelectOption other = (SelectOption) obj;
      if (label == null) {
         if (other.label != null) {
            return false;
         }
      }
      else if (!label.equals(other.label)) {
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
}
