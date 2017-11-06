package com.jc.db.dao.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.jc.db.dao.EntityAssociation;

public class JPAFieldInfo {

   private Field fieldObj;

   private String entityName; // name of the entity owning the field
   private String fieldName;
   private Class<?> fieldClass;
   private boolean idField;

   private JPAEntityInfo entityInfo;

   private EntityAssociation association;
   private boolean associationOwner;
   private String mappedBy;

   private JPAFieldInfo() {
      association = EntityAssociation.NONE;
      associationOwner = true;
      mappedBy = "";
   }

   JPAFieldInfo(String entityName, Field aField) throws Exception {
      this();

      fieldObj = aField;

      setFieldName(aField.getName());
      setEntityName(entityName); // Assign default...

      setFieldClass(aField.getType());

      // seeks to find any annotated association between this entity and a sub-entity
      identifyAssociation(aField);

      setIdField(aField.isAnnotationPresent(Id.class)); // Id Annotation...

      if (!association.equals(EntityAssociation.NONE) && !association.equals(EntityAssociation.ELEMENT_COLLECTION)) { // this field has some association...
         Class<?> fieldClass = getFieldClass();
         JPAEntityInfo entityInfo = new JPAEntityInfo(fieldClass);
         setEntityInfo(entityInfo);
      }
   }

   // Indicates that the field is an ElementCollection.
   public boolean isElementCollection() {
      return association.equals(EntityAssociation.ELEMENT_COLLECTION);
   }

   // Indicates that the field is a Collection or Map including an ElementCollection
   public boolean isACollection() {
      return association.equals(EntityAssociation.MANY_TO_MANY) || association.equals(EntityAssociation.ONE_TO_MANY)
            || association.equals(EntityAssociation.ELEMENT_COLLECTION);
   }

   public Field getFieldObject() {
      return fieldObj;
   }

   public String getEntityName() {
      return entityName;
   }

   public void setEntityName(String entityName) {
      this.entityName = entityName;
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
      Class<?> aClass = fieldClass;
      // if the class is a type of Collection or Map get the type of collection...
      if (Collection.class.isAssignableFrom(fieldClass) || Map.class.isAssignableFrom(fieldClass)) {
         ParameterizedType fieldType = (ParameterizedType) fieldObj.getGenericType();
         aClass = (Class<?>) fieldType.getActualTypeArguments()[0];
      }
      return aClass;
   }

   public void setFieldClass(Class<?> fieldClass) {
      this.fieldClass = fieldClass;
   }

   public JPAEntityInfo getEntityInfo() {
      return entityInfo;
   }

   public void setEntityInfo(JPAEntityInfo entityInfo) {
      this.entityInfo = entityInfo;
   }

   public boolean hasEntityInfo() {
      return entityInfo != null;
   }

   //   @Override
   //   public String toString() {
   //      String value = "COLPROP[" + getEntityName() + "] GETTER[" + getterMethod.getName() + "] SETTER[" + setterMethod.getName() + "] ENTITY_INFO[null]";
   //      if (entityInfo != null) {
   //         String entityInfoStr = entityInfo != null ? entityInfo.toString() : null;
   //         value = "COLPROP[" + getEntityName() + "] GETTER[" + getterMethod.getName() + "] SETTER[" + setterMethod.getName() + "] ENTITY_INFO:"
   //               + FileSystem.NEWLINE;
   //         value += "          " + entityInfoStr;
   //      }
   //
   //      return value;
   //   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((association == null) ? 0 : association.hashCode());
      result = prime * result + (associationOwner ? 1231 : 1237);
      result = prime * result + ((entityInfo == null) ? 0 : entityInfo.hashCode());
      result = prime * result + ((fieldClass == null) ? 0 : fieldClass.hashCode());
      result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
      result = prime * result + (idField ? 1231 : 1237);
      result = prime * result + ((mappedBy == null) ? 0 : mappedBy.hashCode());
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
      if (!(obj instanceof JPAFieldInfo)) {
         return false;
      }
      JPAFieldInfo other = (JPAFieldInfo) obj;
      if (association != other.association) {
         return false;
      }
      if (associationOwner != other.associationOwner) {
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

   private void identifyAssociation(AccessibleObject accessibleObj) {
      // OneToOne Annotation...
      if (accessibleObj.isAnnotationPresent(OneToOne.class)) {
         setAssociation(EntityAssociation.ONE_TO_ONE);
         Annotation ann = accessibleObj.getAnnotation(OneToOne.class);
         String mappedBy = ((OneToOne) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            setMappedBy(mappedBy);
            setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      else if (accessibleObj.isAnnotationPresent(ManyToOne.class)) {
         setAssociation(EntityAssociation.MANY_TO_ONE);
         setAssociationOwner(true);
      }
      else if (accessibleObj.isAnnotationPresent(OneToMany.class)) {
         setAssociation(EntityAssociation.ONE_TO_MANY);
         Annotation ann = accessibleObj.getAnnotation(OneToMany.class);
         String mappedBy = ((OneToMany) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            setMappedBy(mappedBy);
            setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      else if (accessibleObj.isAnnotationPresent(ManyToMany.class)) {
         setAssociation(EntityAssociation.MANY_TO_MANY);
         Annotation ann = accessibleObj.getAnnotation(ManyToMany.class);
         String mappedBy = ((ManyToMany) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            setMappedBy(mappedBy);
            setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      else if (accessibleObj.isAnnotationPresent(ElementCollection.class)) {
         setAssociation(EntityAssociation.ELEMENT_COLLECTION);
         setAssociationOwner(true);
      }
      // else ignore...
   }
}
