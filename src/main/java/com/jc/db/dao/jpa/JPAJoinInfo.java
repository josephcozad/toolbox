package com.jc.db.dao.jpa;

import com.jc.db.dao.EntityAssociation;

public class JPAJoinInfo {

   private final static int ALIAS_SIZE = 4;

   private EntityAssociation association;

   private String entityName;
   private String entityAlias; // Table Alias joining to

   private String fieldName; // Table Name joining on
   private String fieldNameAlias; // Table Alias joining on

   public void setAssociationType(EntityAssociation association) {
      this.association = association;
   }

   public EntityAssociation getAssociationType() {
      return association;
   }

   // Indicates that the JoinInfo is an ElementCollection...
   public boolean isElementCollection() {
      return association.equals(EntityAssociation.ELEMENT_COLLECTION);
   }

   // Indicates that the JoinInfo is a Collection or Map including an ElementCollection...
   public boolean isACollection() {
      return association.equals(EntityAssociation.MANY_TO_MANY) || association.equals(EntityAssociation.ONE_TO_MANY)
            || association.equals(EntityAssociation.ELEMENT_COLLECTION);
   }

   public void setEntityName(String entityName) {
      this.entityName = entityName;
      String alias = this.entityName.substring(0, ALIAS_SIZE);
      setEntityAlias(alias);
   }

   public String getEntityName() {
      return entityName;
   }

   public void setEntityAlias(String entityAlias) {
      this.entityAlias = entityAlias.toLowerCase();
   }

   public String getEntityAlias() {
      return entityAlias;
   }

   public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
      String alias = this.fieldName.substring(0, ALIAS_SIZE);
      setFieldAlias(alias);
   }

   public String getFieldName() {
      return fieldName;
   }

   public void setFieldAlias(String fieldAlias) {
      fieldNameAlias = fieldAlias.toLowerCase();
   }

   public String getFieldAlias() {
      return fieldNameAlias;
   }

   boolean isSelectStatement() {
      return fieldName == null;
   }

   String getJoinEntityPath() {
      return entityAlias + "." + fieldName;
   }

   @Override
   public String toString() {
      String statement = "";
      if (isSelectStatement()) {
         statement = "SELECT ALIAS_LIST FROM " + entityName + " " + entityAlias;
      }
      else if (isElementCollection()) {
         statement = ", in (" + getJoinEntityPath() + ") " + fieldNameAlias;
      }
      else {
         statement = " JOIN " + getJoinEntityPath() + " " + fieldNameAlias;
      }
      return statement;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fieldNameAlias == null) ? 0 : fieldNameAlias.hashCode());
      result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
      result = prime * result + ((entityAlias == null) ? 0 : entityAlias.hashCode());
      result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
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
      if (!(obj instanceof JPAJoinInfo)) {
         return false;
      }
      JPAJoinInfo other = (JPAJoinInfo) obj;
      if (fieldNameAlias == null) {
         if (other.fieldNameAlias != null) {
            return false;
         }
      }
      else if (!fieldNameAlias.equals(other.fieldNameAlias)) {
         return false;
      }
      if (fieldName == null) {
         if (other.fieldName != null) {
            return false;
         }
      }
      else if (!fieldName.equals(other.fieldName)) {
         return false;
      }
      if (entityAlias == null) {
         if (other.entityAlias != null) {
            return false;
         }
      }
      else if (!entityAlias.equals(other.entityAlias)) {
         return false;
      }
      if (entityName == null) {
         if (other.entityName != null) {
            return false;
         }
      }
      else if (!entityName.equals(other.entityName)) {
         return false;
      }
      return true;
   }
}
