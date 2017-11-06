package com.jc.app.rest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jc.app.rest.annotations.Id;
import com.jc.app.rest.annotations.RDMetadata;
import com.jc.app.rest.annotations.RDMetadata.Attribute;
import com.jc.app.rest.annotations.RDMetadata.BOOLEAN;
import com.jc.app.rest.annotations.Transient;

public class RESTMetadata {

   private String fieldName;
   private String description;
   private boolean idField; // DEFAULT "false"; if true indicates that the field acts as an id field.
   private boolean filterable; // DEFAULT "true"; if true, can be used in search operations.
   private boolean sortable; // DEFAULT "true"; if true, can be used in sort operations.
   private Class<?> fieldType; // Data type... long, int, String, boolean, etc.

   private Map<String, List<RESTMetadata>> fieldTypeMetadata; // if field type is com.jc type, then include added info

   private int addDataAttribute;
   private int modifyDataAttribute;

   private List<Map<String, Object>> valueOptions; // A list of valid values for this field.
   private String defaultValue; // A string representation of the default value for this field: "1"

   private RESTMetadata() {}

   public static List<RESTMetadata> getFieldMetadata(Class<?> aclass) throws Exception {
      boolean isSubDataType = false;
      //      Map<String, List<MetadataNEW>> metadataMap = getFieldMetadata(aclass, isSubDataType);
      //      return metadataMap.get(SINGLE_METADATA_TYPE_KEY);
      return getFieldMetadata(aclass, isSubDataType);
   }

   public static List<RESTMetadata> getFieldMetadata(Class<?> aclass, boolean isSubDataType) throws Exception {
      List<RESTMetadata> superClassFieldnames = new ArrayList<>();

      Class<?> superClass = aclass.getSuperclass();
      if (superClass != null && !(superClass.equals(Object.class))) {
         superClassFieldnames = getFieldMetadata(superClass);
      }

      List<RESTMetadata> metadataList = new ArrayList<>();
      if (!superClassFieldnames.isEmpty()) {
         metadataList.addAll(superClassFieldnames);
      }

      Field[] fields = aclass.getDeclaredFields();
      for (Field field : fields) {
         if (!metadataList.contains(field)) {
            field.setAccessible(true);

            // if the field is not static and not marked transient by annotation
            Transient transientAnn = field.getAnnotation(Transient.class);
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && transientAnn == null) {

               boolean filterable = true; // can be used in searches...
               boolean sortable = true;
               boolean idField = false; // assume field is not annotated as an id field...

               Attribute addDataAttrCode = Attribute.FIELD_OPTIONAL_MODIFIABLE;
               Attribute modDataAttrCode = Attribute.FIELD_OPTIONAL_MODIFIABLE;

               String description = "";
               String defaultValue = "";
               List<Map<String, Object>> valueOptions = null;

               // Check the class' field annotations and adjust metadata parameters as needed.

               // if there's an id annotation that indicates that the field is an id field,
               // then change idField to true and require it.
               Id idAnn = field.getAnnotation(Id.class);
               if (idAnn != null) { // if field has an id annotation check, mark as such...
                  idField = true;
                  addDataAttrCode = idAnn.addData();
                  modDataAttrCode = idAnn.modifyData();
                  if (isSubDataType) {
                     addDataAttrCode = idAnn.subMetadataAttributes().addData();
                     modDataAttrCode = idAnn.subMetadataAttributes().modifyData();
                  }
               }

               // if the field is a sub-entity in the 'com.jc' package, then
               // get it's metadata...
               Map<String, List<RESTMetadata>> subDataMetadataList = new HashMap<>();
               Class<?> fieldClass = field.getType();
               // if fieldClass is a type of Collection or Map, find out what the generic type is....
               if (Collection.class.isAssignableFrom(fieldClass) || Map.class.isAssignableFrom(fieldClass)) {
                  ParameterizedType fieldType = (ParameterizedType) field.getGenericType();
                  fieldClass = (Class<?>) fieldType.getActualTypeArguments()[0];
               }

               if (fieldClass.getName().contains("com.jc")) {
                  if (Modifier.isAbstract(fieldClass.getModifiers())) {
                     RDMetadata metadataAnn = field.getAnnotation(RDMetadata.class);
                     if (metadataAnn != null && metadataAnn.extendingClasses().length > 0) {
                        String[] classList = metadataAnn.extendingClasses();
                        for (String className : classList) {
                           Class<?> aClass = Class.forName(className);
                           List<RESTMetadata> subEntityMetadataList = getFieldMetadata(aClass, true);
                           subDataMetadataList.put(aClass.getSimpleName(), subEntityMetadataList);
                        }
                     }
                     else {
                        List<RESTMetadata> subEntityMetadataList = getFieldMetadata(fieldClass, true);
                        subDataMetadataList.put(fieldClass.getSimpleName(), subEntityMetadataList);
                     }
                  }
                  else {
                     List<RESTMetadata> subEntityMetadataList = getFieldMetadata(fieldClass, true);
                     subDataMetadataList.put(fieldClass.getSimpleName(), subEntityMetadataList);
                  }
               }

               // Now check if there is an @RDMetadata annotation and override any defaults 
               // established above...
               RDMetadata metadataAnn = field.getAnnotation(RDMetadata.class);
               if (metadataAnn != null) {

                  // if it's not a sub-entity and the field's metadata filterable has been set
                  if (!metadataAnn.filterable().equals(BOOLEAN.UNKNOWN)) {
                     filterable = metadataAnn.filterable().equals(BOOLEAN.YES);
                  }

                  // if it's not a sub-entity and the field's metadata sortable has been set
                  if (!metadataAnn.filterable().equals(BOOLEAN.UNKNOWN)) {
                     sortable = metadataAnn.sortable().equals(BOOLEAN.YES);
                  }

                  addDataAttrCode = metadataAnn.addData();
                  modDataAttrCode = metadataAnn.modifyData();
                  if (isSubDataType) {
                     addDataAttrCode = metadataAnn.subMetadataAttributes().addData();
                     modDataAttrCode = metadataAnn.subMetadataAttributes().modifyData();
                  }

                  description = metadataAnn.description();
                  defaultValue = metadataAnn.defaultValue();

                  String valueOptionLoaderClass = metadataAnn.valueOptionLoaderClass();
                  if (valueOptionLoaderClass != null && !valueOptionLoaderClass.isEmpty()) {
                     Class<?> classObj = Class.forName(valueOptionLoaderClass);
                     Class<?> superClassObj = classObj.getSuperclass(); // the extending class...
                     if (superClassObj == ValueOptionsLoader.class) {
                        Constructor<?> constructor = ((Class<ValueOptionsLoader>) classObj).getConstructor((Class<?>[]) null);
                        ValueOptionsLoader valueOptionsLoader = (ValueOptionsLoader) constructor.newInstance((Object[]) null);
                        valueOptions = valueOptionsLoader.getValueOptions();
                     }
                     else {
                        // else silently do nothing special... no error
                        //  valueOptions = metadataAnn.valueOptions();
                     }
                  }
                  else {
                     String[] options = metadataAnn.valueOptions();
                     if (options != null && options.length > 0) {
                        valueOptions = new ArrayList<>();
                        for (String option : options) {
                           Map<String, Object> valueMap = new HashMap<>();
                           valueMap.put("value", option);
                           valueOptions.add(valueMap);
                        }
                     }
                  }
               }

               // set class values...

               RESTMetadata mdata = new RESTMetadata();
               mdata.setFieldName(field.getName());
               mdata.setIdField(idField);
               mdata.setDescription(description);
               mdata.setAddDataAttribute(addDataAttrCode);
               mdata.setModifyDataAttribute(modDataAttrCode);
               mdata.setFilterable(filterable);
               mdata.setSortable(sortable);
               mdata.setFieldType(fieldClass);
               mdata.setFieldTypeMetadata(subDataMetadataList);
               mdata.setValueOptions(valueOptions);
               mdata.setDefaultValue(defaultValue);

               metadataList.add(mdata);
            }
            // else skip it...
         }
         // else skip it already in the list from a superClass....
      }

      return metadataList;
   }

   public static List<RESTMetadata> getModifiableFields(List<RESTMetadata> metadataList) throws Exception {
      List<RESTMetadata> modifableFieldList = new ArrayList<>();
      for (RESTMetadata mdata : metadataList) {
         Attribute modifyAttribute = mdata.getModifyDataAttribute();
         if (modifyAttribute.equals(Attribute.FIELD_OPTIONAL_MODIFIABLE) || modifyAttribute.equals(Attribute.FIELD_REQUIRED_MODIFIABLE)) {
            modifableFieldList.add(mdata);
         }
      }
      return modifableFieldList;
   }

   public static Map<String, List<RESTMetadata>> getModifiableFields(Map<String, List<RESTMetadata>> inputMetadataMap) throws Exception {
      Map<String, List<RESTMetadata>> metadataMap = new HashMap<>();
      for (Map.Entry<String, List<RESTMetadata>> entry : inputMetadataMap.entrySet()) {
         String fieldType = entry.getKey();
         List<RESTMetadata> metadataList = entry.getValue();
         List<RESTMetadata> modifableFieldList = getModifiableFields(metadataList);
         metadataMap.put(fieldType, modifableFieldList);
      }
      return metadataMap;
   }

   // ---------------------------------------------------------------------

   public String getFieldName() {
      return fieldName;
   }

   public void setFieldName(String fieldname) {
      this.fieldName = fieldname;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public boolean isIdField() {
      return idField;
   }

   public void setIdField(boolean idField) {
      this.idField = idField;
   }

   public Attribute getAddDataAttribute() {
      return Attribute.fromCode(addDataAttribute);
   }

   public void setAddDataAttribute(Attribute addDataAttribute) {
      this.addDataAttribute = addDataAttribute.getCode();
   }

   public Attribute getModifyDataAttribute() {
      return Attribute.fromCode(modifyDataAttribute);
   }

   public void setModifyDataAttribute(Attribute modifyDataAttribute) {
      this.modifyDataAttribute = modifyDataAttribute.getCode();
   }

   public boolean isFilterable() {
      return filterable;
   }

   public void setFilterable(boolean filterable) {
      this.filterable = filterable;
   }

   public boolean isSortable() {
      return sortable;
   }

   public void setSortable(boolean sortable) {
      this.sortable = sortable;
   }

   public Class<?> getFieldType() {
      return fieldType;
   }

   public void setFieldType(Class<?> fieldType) {
      this.fieldType = fieldType;
   }

   public void setFieldTypeMetadata(Map<String, List<RESTMetadata>> metadataList) {
      fieldTypeMetadata = metadataList;
   }

   public Map<String, List<RESTMetadata>> getFieldTypeMetadata() {
      return fieldTypeMetadata;
   }

   public List<Map<String, Object>> getValueOptions() {
      return valueOptions;
   }

   public void setValueOptions(List<Map<String, Object>> valueOptions) {
      this.valueOptions = valueOptions;
   }

   public String getDefaultValue() {
      return defaultValue;
   }

   public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
   }
}
