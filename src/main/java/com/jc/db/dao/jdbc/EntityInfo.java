package com.jc.db.dao.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.jc.db.dao.EntityAssociation;
import com.jc.util.FileSystem;
import com.jc.util.ObjectFactory;

public class EntityInfo {

   private final Class<?> objClass;

   private String tableName;
   private String schemaName;

   private String idFieldName;

   private Map<String, String> columnPropFieldNameXREF; // key=columnProperty/value=entityFieldName
   private Map<String, FieldInfo> fieldInfoMap; // key=entityFieldName/value=entityFieldInfo

   private final JoinInfo selectInfo;
   private final Map<String, JoinInfo> joinInfoMap; // key=tableAlias/value=tableJoinInfo
   private final Map<String, String> tableNameAliasXREF; // key=tablename/value=tableAlias

   public EntityInfo(Class<?> objClass) {
      this.objClass = objClass;
      schemaName = ""; // Optional

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

      joinInfoMap = new LinkedHashMap<String, JoinInfo>();

      selectInfo = new JoinInfo(this);
      List<String> propKeys = getColumPropKeys();
      for (int i = 0; i < propKeys.size(); i++) {
         String columnProp = propKeys.get(i);
         FieldInfo fieldInfo = getFieldInfoByColumnProperty(columnProp);
         if (fieldInfo.hasEntityInfo()) {
            createJoinInfo(this, fieldInfo, columnProp);
         }
      }

      tableNameAliasXREF = new HashMap<String, String>();
      tableNameAliasXREF.put(selectInfo.pTableName.toUpperCase(), selectInfo.pTableAlias);

      if (!joinInfoMap.isEmpty()) {
         for (Map.Entry<String, JoinInfo> entry : joinInfoMap.entrySet()) {
            JoinInfo joinInfo = entry.getValue();
            String alias = "";
            String tableName = "";
            if (joinInfo.jTableAlias != null) {
               alias = joinInfo.jTableAlias;
               tableName = joinInfo.jTableName.toUpperCase();
            }
            else {
               alias = joinInfo.pTableAlias;
               tableName = joinInfo.pTableName.toUpperCase();
            }
            tableNameAliasXREF.put(tableName, alias);
         }
      }

      // Validation, cause everyone needs a little now and then...
      boolean error = false;
      StringBuilder sb = new StringBuilder("Error initializing EntityInfo for " + objClass.getName() + ". ");
      if (columnPropFieldNameXREF.size() != fieldInfoMap.size()) {
         sb.append("columnPropFieldNameXREF and fieldInfoMap are not the same size. ");
         error = true;
      }

      if ((tableNameAliasXREF.size() - 1) != joinInfoMap.size()) {
         sb.append("tableNameAliasXREF and joinInfoMap are not the same size. ");
         error = true;
      }

      for (Map.Entry<String, FieldInfo> entry : fieldInfoMap.entrySet()) {
         String key = entry.getKey();
         FieldInfo fieldInfo = entry.getValue();
         if (!fieldInfo.valid()) {
            sb.append("FieldInfo for '" + key + "' is invalid: " + fieldInfo.getInvalidMsg() + ".");
            error = true;
         }
      }

      if (error) {
         throw new IllegalStateException(sb.toString());
      }
   }

   public JoinInfo getJoinInfoForAlias(String alias) {
      if (alias.equals(selectInfo.pTableAlias)) {
         return selectInfo;
      }
      else {
         return joinInfoMap.get(alias);
      }
   }

   public JoinInfo getSelectInfo() {
      return selectInfo;
   }

   public Map<String, JoinInfo> getJoinInfo() {
      return joinInfoMap;
   }

   public String getAliasForTableName(String tableName) {
      return tableNameAliasXREF.get(tableName);
   }

   public String getEntityClassName() {
      return objClass.getName();
   }

   public String getSchemaName() {
      return schemaName;
   }

   public String getTableName() {
      return tableName;
   }

   public List<String> getColumPropKeys() {
      return new ArrayList<String>(columnPropFieldNameXREF.keySet());
   }

   public FieldInfo getFieldInfoByColumnProperty(String columnProp) {
      if (columnPropFieldNameXREF.containsKey(columnProp)) {
         String fieldName = columnPropFieldNameXREF.get(columnProp);
         if (fieldInfoMap.containsKey(fieldName)) {
            return fieldInfoMap.get(fieldName);
         }
         else {
            throw new IllegalArgumentException("No entity field information for the supplied column property of " + columnProp);
         }
      }
      else {
         throw new IllegalArgumentException("No entity field information for the supplied column property of " + columnProp);
      }
   }

   public boolean isColumnName(String entityField) {
      String columnPropTest = getTableName() + "." + entityField.toUpperCase();
      return columnPropFieldNameXREF.containsKey(columnPropTest);
   }

   public String getColumnNameByFieldName(String entityField) {
      FieldInfo fieldInfo = getFieldInfoByFieldName(entityField);

      String columnName = fieldInfo.getColumnName();
      String tableName = fieldInfo.getTableName();
      String alias = getAliasForTableName(tableName);
      return alias + "." + columnName;
   }

   public FieldInfo getFieldInfoByFieldName(String entityField) {
      // if entityField has dots, assume that it's a fieldName path using subEntities.
      if (entityField.contains(".")) {
         entityField = entityField.toLowerCase();

         String[] fieldNames = entityField.split("\\.");
         List<String> fieldNameList = new ArrayList<String>();

         for (int i = fieldNames.length - 1; i >= 0; i--) {
            fieldNameList.add(fieldNames[i]);
         }

         Stack<String> fieldNameStack = new Stack<String>();
         fieldNameStack.addAll(fieldNameList);

         FieldInfo fieldInfo = getFieldInfoForFieldNamePath(fieldNameStack, this);

         return fieldInfo;
      }
      else {
         FieldInfo fieldInfo = null;
         String testFieldName = entityField.toLowerCase();
         if (isFieldName(testFieldName)) { // see if fieldName...
            if (fieldInfoMap.containsKey(testFieldName)) {
               fieldInfo = fieldInfoMap.get(testFieldName);
            }
            else {
               throw new IllegalArgumentException("Unable to find an aliased column name for " + entityField);
            }
         }
         else if (isColumnName(entityField)) { // else try columnProperty...
            String columnPropertyKey = getTableName() + "." + entityField.toUpperCase();
            fieldInfo = getFieldInfoByColumnProperty(columnPropertyKey);
         }
         else {
            throw new IllegalArgumentException("Unable to find an aliased column name for " + entityField);
         }
         return fieldInfo;
      }
   }

   public FieldInfo getIdFieldInfo() {
      return fieldInfoMap.get(idFieldName);
   }

   public boolean isFieldName(String entityField) {
      return fieldInfoMap.containsKey(entityField);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(schemaName + "." + tableName + " --> " + objClass + FileSystem.NEWLINE);

      for (Map.Entry<String, FieldInfo> entry : fieldInfoMap.entrySet()) {
         String fieldName = entry.getKey();
         FieldInfo fieldInfo = entry.getValue();
         sb.append("     FIELDNAME[" + fieldName + "] INFO --> " + fieldInfo + FileSystem.NEWLINE);
      }

      return sb.toString();
   }

   // ------------------ Private Methods -------------------------------

   private void init(List<Class<?>> classList) {
      fieldInfoMap = new HashMap<String, FieldInfo>(); // initialize...
      columnPropFieldNameXREF = new HashMap<String, String>(); // initialize...

      for (int i = 0; i < classList.size(); i++) {
         Class<?> classObj = classList.get(i);

         assignSchemaAndTableName(classObj);

         // Search fields for annotation info...
         Field[] fields = classObj.getDeclaredFields();
         for (Field aField : fields) {
            int modfiers = aField.getModifiers();
            if (!Modifier.isStatic(modfiers)) {
               if (!aField.isAnnotationPresent(Transient.class)) { // Transient Annotation...
                  FieldInfo fieldInfo = new FieldInfo();
                  fieldInfo.setFieldName(aField.getName().toLowerCase());
                  fieldInfo.setTableName(tableName); // Assign default...
                  fieldInfo.setColumnName(fieldInfo.getFieldName()); // Assign default...

                  // seeks to find any annotated association between this entity and a sub-entity
                  fieldInfo = identifyAssociation(fieldInfo, aField);

                  fieldInfo = assignTableAndColumnInfo(fieldInfo, aField);

                  fieldInfo.setFieldClass(aField.getType());
                  fieldInfo.setIdField(aField.isAnnotationPresent(Id.class)); // Id Annotation...

                  String columnProp = fieldInfo.getColumnProperty();
                  columnPropFieldNameXREF.put(columnProp, fieldInfo.getFieldName());
                  fieldInfoMap.put(fieldInfo.getFieldName(), fieldInfo);

               }
               // else field marked transient, ignore.
            }
            // else static field, ignore.
         }

         // Search methods for annotation info...
         Method[] methods = classObj.getMethods();
         for (Method aMethod : methods) {
            boolean getterMethod = ObjectFactory.isGetter(aMethod);
            boolean setterMethod = ObjectFactory.isSetter(aMethod);

            if (getterMethod || setterMethod) { // Only process getter and setter methods.
               String fieldName = getFieldNameFromMethod(aMethod, getterMethod);

               if (!aMethod.isAnnotationPresent(Transient.class)) { // Transient Annotation...

                  if (fieldInfoMap.containsKey(fieldName)) {
                     FieldInfo fieldInfo = fieldInfoMap.get(fieldName);

                     if (getterMethod) {
                        fieldInfo.setGetterMethod(aMethod);
                     }
                     else {
                        fieldInfo.setSetterMethod(aMethod);
                     }

                     // Search the method annotations if any for relevant info...
                     Annotation[] methodAnns = aMethod.getAnnotations();
                     if (methodAnns.length > 0) {
                        // seeks to find any annotated association between this entity and a sub-entity
                        fieldInfo = identifyAssociation(fieldInfo, aMethod);

                        fieldInfo = assignTableAndColumnInfo(fieldInfo, aMethod);

                        if (aMethod.isAnnotationPresent(Id.class)) { // Id Annotation...
                           fieldInfo.setIdField(true);
                        }
                        // else ignore, it may have been already set...
                     }
                     // else method has no annotations, ignore.
                  }
                  // else ignore, could be that the field was marked transient and never added.
               }
               else { // else method marked transient...
                  // this field is marked Transient at the method level, look for related fieldInfo and remove...
                  if (fieldInfoMap.containsKey(fieldName)) {
                     FieldInfo fieldInfo = fieldInfoMap.get(fieldName);
                     String columnProp = fieldInfo.getColumnProperty();
                     columnPropFieldNameXREF.remove(columnProp);

                     fieldInfo = null; // make sure there's no reference...
                     fieldInfoMap.remove(fieldName);
                  }
               }
            }
            // else method not a getter or setter, ignore.
         }
      }

      // Check for sub-entities and create their fieldInfo as needed ....
      boolean idFieldFound = false;

      for (Map.Entry<String, FieldInfo> entry : fieldInfoMap.entrySet()) {
         FieldInfo fieldInfo = entry.getValue();

         if (fieldInfo.getFieldClass().isAnnotationPresent(Entity.class)) {
            EntityInfo entityInfo = new EntityInfo(fieldInfo.getFieldClass());
            fieldInfo.setEntityInfo(entityInfo);
         }

         if (fieldInfo.isIdField()) {
            if (idFieldName == null) {
               idFieldName = fieldInfo.getFieldName();
               idFieldFound = true;
            }
            else {
               throw (new IllegalArgumentException("More than one id field detected in entity: " + objClass.getName()));
            }
         }
      }

      if (!idFieldFound) {
         throw (new IllegalArgumentException("Unable to determine the id field from the supplied entity class: " + objClass.getName()));
      }
   }

   private void assignSchemaAndTableName(Class<?> classObj) {
      if (classObj.isAnnotationPresent(Table.class)) {
         Annotation ann = classObj.getAnnotation(Table.class);

         String name = ((Table) ann).name();
         if (name == null || name.isEmpty()) {
            name = classObj.getSimpleName(); // Use class name as table name by default
         }
         if (name.contains(".")) {
            int dotIndex = name.indexOf(".");
            String schema = name.substring(0, dotIndex);
            schemaName = schema.toUpperCase();
            name = name.substring(dotIndex + 1, name.length());
         }
         tableName = name.toUpperCase();

         String schema = ((Table) ann).schema();
         if (schema != null && !schema.isEmpty()) {
            schemaName = schema.toUpperCase();
         }
      }

      //double check tableName value...
      if (tableName == null || tableName.isEmpty()) {
         throw (new IllegalArgumentException("Unable to determine the table name from the supplied entity class: " + classObj.getName()));
      }
   }

   private FieldInfo identifyAssociation(FieldInfo fieldInfo, AccessibleObject accessibleObj) {
      // OneToOne Annotation...
      if (accessibleObj.isAnnotationPresent(OneToOne.class)) {
         fieldInfo.setAssociation(EntityAssociation.ONE_TO_ONE);
         Annotation ann = accessibleObj.getAnnotation(OneToOne.class);
         String mappedBy = ((OneToOne) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            fieldInfo.setMappedBy(mappedBy);
            fieldInfo.setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      else if (accessibleObj.isAnnotationPresent(ManyToOne.class)) {
         fieldInfo.setAssociation(EntityAssociation.MANY_TO_ONE);
         fieldInfo.setAssociationOwner(true);
      }
      else if (accessibleObj.isAnnotationPresent(OneToMany.class)) {
         fieldInfo.setAssociation(EntityAssociation.ONE_TO_MANY);
         Annotation ann = accessibleObj.getAnnotation(OneToMany.class);
         String mappedBy = ((OneToMany) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            fieldInfo.setMappedBy(mappedBy);
            fieldInfo.setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      else if (accessibleObj.isAnnotationPresent(ManyToMany.class)) {
         fieldInfo.setAssociation(EntityAssociation.MANY_TO_MANY);
         Annotation ann = accessibleObj.getAnnotation(ManyToMany.class);
         String mappedBy = ((ManyToMany) ann).mappedBy();
         if (mappedBy != null && !mappedBy.isEmpty()) {
            fieldInfo.setMappedBy(mappedBy);
            fieldInfo.setAssociationOwner(false);
         }
         // else use default associationOwner = true;
      }
      // else ignore...

      return fieldInfo;
   }

   private FieldInfo assignTableAndColumnInfo(FieldInfo fieldInfo, AccessibleObject accessibleObj) {
      String table = null;
      String colName = null;
      String refColName = null;

      if ((fieldInfo.isAssociationOwner())
            && (fieldInfo.getAssociation().equals(EntityAssociation.MANY_TO_ONE) || fieldInfo.getAssociation().equals(EntityAssociation.ONE_TO_ONE))) {

         if (accessibleObj.isAnnotationPresent(JoinColumn.class)) { // JoinColumn Annotation...
            Annotation ann = accessibleObj.getAnnotation(JoinColumn.class);
            table = ((JoinColumn) ann).table();
            colName = ((JoinColumn) ann).name();
            refColName = ((JoinColumn) ann).referencedColumnName();
         }
         //         else if (accessibleObj.isAnnotationPresent(JoinColumns.class)) {
         //            Annotation ann = accessibleObj.getAnnotation(JoinColumns.class);
         //
         //         }
         else if (accessibleObj.isAnnotationPresent(JoinTable.class)) {
            Annotation ann = accessibleObj.getAnnotation(JoinTable.class);

            fieldInfo.setXREFSchemaName(((JoinTable) ann).schema());
            fieldInfo.setXREFTableName(((JoinTable) ann).name());

            JoinColumn[] joinColumns = ((JoinTable) ann).joinColumns();
            fieldInfo.setXREFSrcColumn(joinColumns[0].name());

            JoinColumn[] inverseJoinColumns = ((JoinTable) ann).inverseJoinColumns();
            fieldInfo.setXREFDestColumn(inverseJoinColumns[0].name());
         }
      }
      //   else if (association.equals(EntityAssociation.NONE)) {
      else {
         // Column Annotation...
         if (accessibleObj.isAnnotationPresent(Column.class)) { // Column Annotation...
            Annotation ann = accessibleObj.getAnnotation(Column.class);
            table = ((Column) ann).table();
            colName = ((Column) ann).name();
         }
      }

      if (table != null && !table.isEmpty()) {
         fieldInfo.setTableName(table);
      }

      if (colName != null && !colName.isEmpty()) {
         fieldInfo.setColumnName(colName);
      }

      if (refColName != null && !refColName.isEmpty()) {
         fieldInfo.setRefColumnName(refColName);
      }
      return fieldInfo;
   }

   private String getFieldNameFromMethod(Method aMethod, boolean getterMethod) {
      String methodName = aMethod.getName();
      String fieldName = null;

      int length = 3; // for "get" or "set";
      if (getterMethod && methodName.startsWith("is")) {
         length = 2;
      }

      fieldName = methodName.substring(length, methodName.length());

      fieldName = fieldName.toLowerCase();
      return fieldName;
   }

   private void createJoinInfo(EntityInfo entityInfo, FieldInfo fieldInfo, String columnProp) {
      if (columnProp != null && !columnProp.isEmpty()) {
         JoinInfo joinInfo = new JoinInfo(entityInfo, fieldInfo, columnProp);
         addJoinInfoToMap(joinInfo);

         EntityInfo subEntityInfo = fieldInfo.getEntityInfo();
         List<String> propKeys = subEntityInfo.getColumPropKeys();
         for (int i = 0; i < propKeys.size(); i++) {
            String subColumnProp = propKeys.get(i);
            FieldInfo subFieldInfo = subEntityInfo.getFieldInfoByColumnProperty(subColumnProp);
            if (subFieldInfo.hasEntityInfo()) {
               createJoinInfo(subEntityInfo, subFieldInfo, subColumnProp);
            }
         }
      }
   }

   private void addJoinInfoToMap(JoinInfo joinInfo) {
      String tableAlias = joinInfo.jTableAlias;

      // first make sure that the alias doesn't match the select info alias;
      // if it does just add '1' to it...
      String selectAlias = selectInfo.pTableAlias;
      if (selectAlias.equals(tableAlias)) {
         tableAlias = tableAlias + 1;
         joinInfo.jTableAlias = tableAlias;
      }

      // now make sure that the alias doesn't match any other JoinInfo alias'.
      if (joinInfoMap.containsKey(tableAlias)) {
         String testAlias = tableAlias;
         boolean dupFound = false;
         for (int i = 0; joinInfoMap.containsKey(testAlias) && !dupFound; i++) {
            JoinInfo dupInfo = joinInfoMap.get(testAlias);
            if (!dupInfo.equals(joinInfo)) {
               testAlias = tableAlias + i;
            }
            else {
               dupFound = true;
            }
         }

         if (!dupFound) {
            joinInfo.jTableAlias = testAlias;
         }
      }

      joinInfoMap.put(joinInfo.jTableAlias, joinInfo);
   }

   private FieldInfo getFieldInfoForFieldNamePath(Stack<String> fieldNameStack, EntityInfo entityInfo) {
      FieldInfo fieldInfo = null;

      String fieldName = fieldNameStack.pop();
      if (entityInfo.isFieldName(fieldName)) { // see if entity info has field name...
         fieldInfo = entityInfo.getFieldInfoByFieldName(fieldName);
         if (!fieldNameStack.isEmpty()) {
            if (fieldInfo.hasEntityInfo()) {
               EntityInfo subEntityInfo = fieldInfo.getEntityInfo();
               fieldInfo = getFieldInfoForFieldNamePath(fieldNameStack, subEntityInfo);
            }
            else {
               throw new IllegalArgumentException("Unable to determine the EntityInfo for the field " + fieldInfo.getFieldName() + " which as a type of "
                     + fieldInfo.getFieldClass() + ".");
            }
         }
         // else, done this is the end...
      }
      else {
         // invalid field name...
         throw new IllegalArgumentException("Invalid field name, '" + fieldName + "', for enity.");
      }

      return fieldInfo;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((columnPropFieldNameXREF == null) ? 0 : columnPropFieldNameXREF.hashCode());
      result = prime * result + ((fieldInfoMap == null) ? 0 : fieldInfoMap.hashCode());
      result = prime * result + ((idFieldName == null) ? 0 : idFieldName.hashCode());
      result = prime * result + ((objClass == null) ? 0 : objClass.hashCode());
      result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
      result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
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
      if (!(obj instanceof EntityInfo)) {
         return false;
      }
      EntityInfo other = (EntityInfo) obj;
      if (columnPropFieldNameXREF == null) {
         if (other.columnPropFieldNameXREF != null) {
            return false;
         }
      }
      else if (!columnPropFieldNameXREF.equals(other.columnPropFieldNameXREF)) {
         return false;
      }
      if (fieldInfoMap == null) {
         if (other.fieldInfoMap != null) {
            return false;
         }
      }
      else if (!fieldInfoMap.equals(other.fieldInfoMap)) {
         return false;
      }
      if (idFieldName == null) {
         if (other.idFieldName != null) {
            return false;
         }
      }
      else if (!idFieldName.equals(other.idFieldName)) {
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
      if (schemaName == null) {
         if (other.schemaName != null) {
            return false;
         }
      }
      else if (!schemaName.equals(other.schemaName)) {
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
      return true;
   }
}
