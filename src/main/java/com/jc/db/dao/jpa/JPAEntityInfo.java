package com.jc.db.dao.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import com.jc.util.FileSystem;
import com.jc.util.ObjectFactory;

public class JPAEntityInfo {

   public final static String ALIAS_LIST = "ALIAS_LIST";

   private final Class<?> objClass;

   private String entityName;
   private JPAJoinInfo selectInfo;

   private Map<String, JPAFieldInfo> fieldInfoMap; // key=entity field name/value=entity field info
   private Map<String, JPAJoinInfo> joinInfoMap; // key=tableAlias/value=table join info

   public JPAEntityInfo(Class<?> objClass) throws Exception {
      this.objClass = objClass;

      // Create a list of classes from the supplied object class up the hierarchy  
      // to the top most parent class that is not of type Object.class
      List<Class<?>> classList = new ArrayList<Class<?>>();
      classList.add(objClass);

      Class<?> parentClass = objClass.getSuperclass();
      while (!parentClass.equals(Object.class)) {
         classList.add(parentClass);
         parentClass = parentClass.getSuperclass();
      }

      // Using the list of classes, create the colMethodXRef
      init(classList);

      initJoinInfo();
   }

   public String getSelectStatement(List<String> joinOn, boolean includeCount) throws Exception {
      String alias = getSelectAlias();

      // Figure out if joinOn fields contain an element collection, if so wrap alias in a distinct()
      if (hasElementCollection(joinOn)) {
         alias = "distinct " + alias + "";
      }

      if (includeCount) {
         alias = "count( " + alias + ")";
      }

      String statement = selectInfo.toString();
      statement = statement.replace(JPAEntityInfo.ALIAS_LIST, alias);
      return statement;
   }

   public String getSelectAlias() {
      return selectInfo.getEntityAlias();
   }

   public JPAJoinInfo getJoinInfoFor(String entityField) throws Exception {
      // if entityField has dots, assume that it's a fieldName path using subEntities.
      if (entityField.contains(".")) {
         entityField = entityField.substring(0, entityField.lastIndexOf("."));
      }
      return joinInfoMap.get(getEntityFieldName(entityField));
   }

   public String getAliasedFieldName(String entityField) throws Exception {
      String fieldName = getEntityFieldName(entityField);
      JPAJoinInfo joinInfo = getJoinInfoFor(entityField);
      if (joinInfo != null) {
         if (joinInfo.isElementCollection()) {
            fieldName = joinInfo.getFieldAlias();
         }
         else {
            fieldName = entityField.substring(entityField.lastIndexOf(".") + 1, entityField.length());
            fieldName = joinInfo.getFieldAlias() + "." + fieldName;
         }
      }
      else {
         fieldName = entityField.substring(entityField.lastIndexOf(".") + 1, entityField.length());
         fieldName = selectInfo.getEntityAlias() + "." + fieldName;
      }
      return fieldName;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(entityName + " --> " + objClass + FileSystem.NEWLINE);

      for (Map.Entry<String, JPAFieldInfo> entry : fieldInfoMap.entrySet()) {
         String fieldName = entry.getKey();
         JPAFieldInfo fieldInfo = entry.getValue();
         sb.append("     FIELDNAME[" + fieldName + "] INFO --> " + fieldInfo + FileSystem.NEWLINE);
      }

      return sb.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fieldInfoMap == null) ? 0 : fieldInfoMap.hashCode());
      result = prime * result + ((objClass == null) ? 0 : objClass.hashCode());
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
      if (!(obj instanceof JPAEntityInfo)) {
         return false;
      }
      JPAEntityInfo other = (JPAEntityInfo) obj;
      if (fieldInfoMap == null) {
         if (other.fieldInfoMap != null) {
            return false;
         }
      }
      else if (!fieldInfoMap.equals(other.fieldInfoMap)) {
         return false;
      }
      if (objClass == null) {
         if (other.objClass != null) {
            return false;
         }
      }
      else if (!objClass.getName().equals(other.objClass.getName())) {
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

   private String getEntityFieldName(String fieldName) {
      return fieldName;
   }

   private JPAFieldInfo getFieldInfoByName(String fieldName) throws Exception {
      if (fieldInfoMap.containsKey(fieldName)) {
         return fieldInfoMap.get(fieldName);
      }
      else {
         throw new Exception("No entity field information for the supplied field name of " + fieldName);
      }
   }

   private void init(List<Class<?>> classList) throws Exception {
      fieldInfoMap = new HashMap<String, JPAFieldInfo>(); // initialize...

      for (int i = 0; i < classList.size(); i++) {
         Class<?> classObj = classList.get(i);

         if (entityName == null) {
            entityName = classObj.getSimpleName();
         }

         // Create field info for each field in the entity...
         Field[] fields = classObj.getDeclaredFields();
         for (Field aField : fields) {
            int modfiers = aField.getModifiers();
            if (!Modifier.isStatic(modfiers)) {
               if (!aField.isAnnotationPresent(Transient.class)) { // Transient Annotation...
                  JPAFieldInfo fieldInfo = new JPAFieldInfo(entityName, aField);
                  fieldInfoMap.put(fieldInfo.getFieldName(), fieldInfo);
               }
               // else field marked transient, ignore.
            }
            // else static field, ignore.
         }

         // Search methods to make sure that the field is not marked Transient...
         Method[] methods = classObj.getMethods();
         for (Method aMethod : methods) {
            if (aMethod.isAnnotationPresent(Transient.class)) { // method marked transient...
               boolean getterMethod = ObjectFactory.isGetter(aMethod);
               boolean setterMethod = ObjectFactory.isSetter(aMethod);
               if (getterMethod || setterMethod) { // Only process getter and setter methods.
                  // this field is marked Transient at the method level, look for related fieldInfo and remove...
                  String fieldName = getFieldNameFromMethod(aMethod, getterMethod);
                  if (fieldInfoMap.containsKey(fieldName)) {
                     fieldInfoMap.remove(fieldName);
                  }
               }
            }
         }
      }
   }

   private String getFieldNameFromMethod(Method aMethod, boolean getterMethod) {
      String methodName = aMethod.getName();
      String fieldName = null;
      if (getterMethod) {
         fieldName = methodName.replace("get", ""); // strip off 'get'
      }
      else {
         fieldName = methodName.replace("set", ""); // strip off 'set'
      }
      fieldName = fieldName.toLowerCase();
      return fieldName;
   }

   private void initJoinInfo() throws Exception {
      selectInfo = new JPAJoinInfo();
      selectInfo.setEntityName(entityName);

      joinInfoMap = new HashMap<String, JPAJoinInfo>();

      List<String> propKeys = new ArrayList<String>(fieldInfoMap.keySet());
      for (int i = 0; i < propKeys.size(); i++) {
         String fieldName = propKeys.get(i);
         JPAFieldInfo fieldInfo = getFieldInfoByName(fieldName);
         if (fieldInfo.hasEntityInfo() || fieldInfo.isElementCollection()) {
            List<JPAJoinInfo> joinList = getJoinInfo(this, fieldInfo, fieldName);
            for (JPAJoinInfo joinInfo : joinList) {
               String mapkey = joinInfo.getJoinEntityPath();
               mapkey = mapkey.substring(mapkey.indexOf(".") + 1, mapkey.length());
               joinInfoMap.put(mapkey, joinInfo);
            }
         }
      }
   }

   private List<JPAJoinInfo> getJoinInfo(JPAEntityInfo entityInfo, JPAFieldInfo jFieldInfo, String fieldName) throws Exception {
      List<JPAJoinInfo> joinList = new ArrayList<JPAJoinInfo>();

      JPAJoinInfo joinInfo = new JPAJoinInfo();
      if (fieldName != null && !fieldName.isEmpty()) {

         joinInfo.setEntityName(jFieldInfo.getEntityName());
         joinInfo.setFieldName(jFieldInfo.getFieldName());
         joinInfo.setAssociationType(jFieldInfo.getAssociation());

         joinList.add(joinInfo);

         if (jFieldInfo.hasEntityInfo()) {
            JPAEntityInfo jEntityInfo = jFieldInfo.getEntityInfo();
            for (JPAJoinInfo info : jEntityInfo.joinInfoMap.values()) {
               if (!info.isSelectStatement()) {
                  String alias = joinInfo.getEntityAlias() + "." + joinInfo.getFieldName();
                  String newAlias = info.getEntityAlias().replace(joinInfo.getFieldAlias(), alias);

                  JPAJoinInfo newInfo = new JPAJoinInfo();
                  newInfo.setEntityName(info.getEntityName());
                  newInfo.setEntityAlias(newAlias);
                  newInfo.setFieldName(info.getFieldName());
                  newInfo.setFieldAlias(info.getFieldAlias());
                  newInfo.setAssociationType(info.getAssociationType());
                  joinList.add(newInfo);
               }
            }
         }
      }
      else {
         joinInfo.setEntityName(entityName);
         joinList.add(joinInfo);
      }
      return joinList;
   }

   private boolean hasElementCollection(List<String> joinOn) throws Exception {
      boolean value = false;
      // joinOn contains a list of entity info fieldnames....

      for (String fieldName : joinOn) {
         JPAJoinInfo joinInfo = getJoinInfoFor(fieldName);
         value = joinInfo.isACollection();
         if (value) {
            break;
         }
      }

      return value;
   }
}
