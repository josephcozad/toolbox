package com.jc.db.dao.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.jc.db.dao.EntityAssociation;
import com.jc.util.FileSystem;

public class FieldInfo {

   private EntityInfo entityInfo; // the entity containing this field...

   private String tableName;
   private String columnName;
   private String refColumnName;

   private String fieldName;
   private Class<?> fieldClass;
   private Method getterMethod;
   private Method setterMethod;

   private EntityAssociation association = EntityAssociation.NONE;
   private boolean associationOwner = true;
   private String mappedBy = "";

   private String xrefSchema;
   private String xrefTable;
   private String xrefSrcColumn;
   private String xrefDestColumn;

   private boolean idField;

   private String invalidMsg;

   public String getTableName() {
      return tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public String getColumnName() {
      return columnName;
   }

   public void setColumnName(String columnName) {
      this.columnName = columnName;
   }

   public String getRefColumnName() {
      return refColumnName;
   }

   public void setRefColumnName(String refColumnName) {
      this.refColumnName = refColumnName;
   }

   public EntityAssociation getAssociation() {
      return association;
   }

   public void setAssociation(EntityAssociation association) {
      this.association = association;
   }

   public boolean isAssociationOwner() {
      return associationOwner;
   }

   public void setAssociationOwner(boolean associationOwner) {
      this.associationOwner = associationOwner;
   }

   public String getMappedBy() {
      return mappedBy;
   }

   public void setMappedBy(String mappedBy) {
      this.mappedBy = mappedBy;
   }

   public String getXREFSchemaName() {
      return xrefSchema;
   }

   public void setXREFSchemaName(String xrefSchema) {
      this.xrefSchema = xrefSchema;
   }

   public String getXREFTableName() {
      return xrefTable;
   }

   public void setXREFTableName(String xrefTable) {
      this.xrefTable = xrefTable;
   }

   public String getXREFSrcColumn() {
      return xrefSrcColumn;
   }

   public void setXREFSrcColumn(String xrefSrcColumn) {
      this.xrefSrcColumn = xrefSrcColumn;
   }

   public String getXREFDestColumn() {
      return xrefDestColumn;
   }

   public void setXREFDestColumn(String xrefDestColumn) {
      this.xrefDestColumn = xrefDestColumn;
   }

   public boolean isIdField() {
      return idField;
   }

   public void setIdField(boolean idField) {
      this.idField = idField;
   }

   public String getFieldName() {
      return fieldName;
   }

   public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
   }

   public Class<?> getFieldClass() {
      return fieldClass;
   }

   public void setFieldClass(Class<?> fieldClass) {
      this.fieldClass = fieldClass;
   }

   public Method getGetterMethod() {
      return getterMethod;
   }

   public void setGetterMethod(Method getterMethod) {
      this.getterMethod = getterMethod;
   }

   public Method getSetterMethod() {
      return setterMethod;
   }

   public void setSetterMethod(Method setterMethod) {
      this.setterMethod = setterMethod;
   }

   public EntityInfo getEntityInfo() {
      return entityInfo;
   }

   public void setEntityInfo(EntityInfo entityInfo) {
      this.entityInfo = entityInfo;
   }

   public String getColumnProperty() {
      return (tableName + "." + columnName);
   }

   public Object getEntityValueForField(Object entityObj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      //     try {
      Object value = getterMethod.invoke(entityObj);
      return value;
      //      }
      //      catch (NullPointerException npe) {
      //         return null;
      //      }
   }

   public boolean hasEntityInfo() {
      return entityInfo != null;
   }

   public boolean hasXREFTable() {
      return xrefTable != null && !xrefTable.isEmpty();
   }

   public String getInvalidMsg() {
      return invalidMsg;
   }

   public boolean valid() {
      boolean valid = true;
      //      if (entityInfo != null) {
      if (tableName != null && !tableName.isEmpty()) {
         if (columnName != null && !columnName.isEmpty()) {
            if (fieldName != null && !fieldName.isEmpty()) {
               if (fieldClass != null) {
                  if (getterMethod != null) {
                     if (setterMethod == null) {
                        invalidMsg = "no setter method detected for " + fieldName;
                        valid = false;
                     }
                  }
                  else {
                     invalidMsg = "no getter method detected for " + fieldName;
                     valid = false;
                  }
               }
               else {
                  invalidMsg = "no field class detected for " + fieldName;
                  valid = false;
               }
            }
            else {
               invalidMsg = "no field name detected for " + columnName;
               valid = false;
            }
         }
         else {
            invalidMsg = "no column name detected";
            valid = false;
         }
      }
      else {
         invalidMsg = "no table name detected";
         valid = false;
      }
      //      }
      //      else {
      //         invalidMsg = "no parent entity info detected";
      //         valid = false;
      //      }
      return valid;
   }

   @Override
   public String toString() {
      String value = "COLPROP[" + getColumnProperty() + "] GETTER[" + getterMethod.getName() + "] SETTER[" + setterMethod.getName() + "] ENTITY_INFO[null]";
      if (entityInfo != null) {
         String entityInfoStr = entityInfo != null ? entityInfo.toString() : null;
         value = "COLPROP[" + getColumnProperty() + "] GETTER[" + getterMethod.getName() + "] SETTER[" + setterMethod.getName() + "] ENTITY_INFO:"
               + FileSystem.NEWLINE;
         value += "          " + entityInfoStr;
      }

      return value;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((association == null) ? 0 : association.hashCode());
      result = prime * result + (associationOwner ? 1231 : 1237);
      result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
      result = prime * result + ((entityInfo == null) ? 0 : entityInfo.hashCode());
      result = prime * result + ((fieldClass == null) ? 0 : fieldClass.hashCode());
      result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
      result = prime * result + ((getterMethod == null) ? 0 : getterMethod.hashCode());
      result = prime * result + (idField ? 1231 : 1237);
      result = prime * result + ((mappedBy == null) ? 0 : mappedBy.hashCode());
      result = prime * result + ((setterMethod == null) ? 0 : setterMethod.hashCode());
      result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
      result = prime * result + ((xrefDestColumn == null) ? 0 : xrefDestColumn.hashCode());
      result = prime * result + ((xrefSchema == null) ? 0 : xrefSchema.hashCode());
      result = prime * result + ((xrefSrcColumn == null) ? 0 : xrefSrcColumn.hashCode());
      result = prime * result + ((xrefTable == null) ? 0 : xrefTable.hashCode());
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
      if (!(obj instanceof FieldInfo)) {
         return false;
      }
      FieldInfo other = (FieldInfo) obj;
      if (association != other.association) {
         return false;
      }
      if (associationOwner != other.associationOwner) {
         return false;
      }
      if (columnName == null) {
         if (other.columnName != null) {
            return false;
         }
      }
      else if (!columnName.equals(other.columnName)) {
         return false;
      }
      if (entityInfo == null) {
         if (other.entityInfo != null) {
            return false;
         }
      }
      else if (!entityInfo.equals(other.entityInfo)) {
         return false;
      }
      if (fieldClass == null) {
         if (other.fieldClass != null) {
            return false;
         }
      }
      else if (!fieldClass.getName().equals(other.fieldClass.getName())) {
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
      if (getterMethod == null) {
         if (other.getterMethod != null) {
            return false;
         }
      }
      else if (!getterMethod.equals(other.getterMethod)) {
         return false;
      }
      if (idField != other.idField) {
         return false;
      }
      if (mappedBy == null) {
         if (other.mappedBy != null) {
            return false;
         }
      }
      else if (!mappedBy.equals(other.mappedBy)) {
         return false;
      }
      if (setterMethod == null) {
         if (other.setterMethod != null) {
            return false;
         }
      }
      else if (!setterMethod.equals(other.setterMethod)) {
         return false;
      }
      if (tableName == null) {
         if (other.tableName != null) {
            return false;
         }
      }
      else if (!tableName.equals(other.tableName)) {
         return false;
      }
      if (xrefDestColumn == null) {
         if (other.xrefDestColumn != null) {
            return false;
         }
      }
      else if (!xrefDestColumn.equals(other.xrefDestColumn)) {
         return false;
      }
      if (xrefSchema == null) {
         if (other.xrefSchema != null) {
            return false;
         }
      }
      else if (!xrefSchema.equals(other.xrefSchema)) {
         return false;
      }
      if (xrefSrcColumn == null) {
         if (other.xrefSrcColumn != null) {
            return false;
         }
      }
      else if (!xrefSrcColumn.equals(other.xrefSrcColumn)) {
         return false;
      }
      if (xrefTable == null) {
         if (other.xrefTable != null) {
            return false;
         }
      }
      else if (!xrefTable.equals(other.xrefTable)) {
         return false;
      }
      return true;
   }
}
