package com.jc.app.rest;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.jc.app.rest.annotations.RDMetadata.BOOLEAN;
import com.jc.app.rest.annotations.Transient;

public abstract class RESTData implements Serializable {

   private static final long serialVersionUID = -8493878746832598360L;

   @Transient(includeInJson = BOOLEAN.YES)
   private String href;

   @Transient
   private boolean collapsed;

   private static boolean emptyValue(Object value) {
      boolean empty = value == null;
      if (!empty) {
         if (value instanceof String) {
            empty = ((String) value).isEmpty();
         }
      }
      return empty;
   }

   /*
    * Copies the data contained in the supplied srcData to the destData where the values 
    * in destData are null, based on the supplied fields contained in the fieldsList.
    */
   public static Object copy(Object srcData, Object destData, List<RESTMetadata> fieldsList) throws Exception {

      Class<? extends Object> srcClassObj = srcData.getClass();
      Class<? extends Object> destClassObj = destData.getClass();
      if (srcClassObj.equals(destClassObj)) {
         for (RESTMetadata mdata : fieldsList) {

            String fieldName = mdata.getFieldName();
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            String prefix = "get";
            Class<?> fieldType = mdata.getFieldType();
            if (Boolean.class.isAssignableFrom(fieldType)) {
               prefix = "is";
            }

            Method srcGetMethod = srcClassObj.getMethod(prefix + fieldName, new Class[0]);
            srcGetMethod.setAccessible(true);
            Object srcValue = srcGetMethod.invoke(srcData);

            Method destGetMethod = destClassObj.getMethod(prefix + fieldName, new Class[0]);
            destGetMethod.setAccessible(true);
            Object destValue = destGetMethod.invoke(destData);

            boolean changeIt = false;
            if (!emptyValue(srcValue)) {
               if (!emptyValue(destValue)) {
                  if (!srcValue.equals(destValue)) { // set dest to src...
                     changeIt = true;
                  }
                  // else no change, they are the same...
               }
               else { // add src to dest...
                  changeIt = true;
               }
            }
            // else ignore it...

            if (changeIt) {
               Map<String, List<RESTMetadata>> subMetadataMap = mdata.getFieldTypeMetadata();
               if (!subMetadataMap.isEmpty()) {
                  String key = srcValue.getClass().getSimpleName();
                  List<RESTMetadata> subMetadataList = subMetadataMap.get(key);
                  List<RESTMetadata> modifiableSubFieldsList = RESTMetadata.getModifiableFields(subMetadataList); // get metadata fields that are not readOnly...
                  srcValue = RESTData.copy(srcValue, destValue, modifiableSubFieldsList);
               }

               Method destSetMethod = destClassObj.getMethod("set" + fieldName, fieldType);
               destSetMethod.setAccessible(true);
               destSetMethod.invoke(destData, srcValue);
            }
         }
      }
      else {
         throw new Exception(
               "The source and dest RESTData objects supplied are not of the same class type; src[" + srcClassObj + "] dest[" + destClassObj + "].");
      }

      return destData;
   }

   public void setCollapsed(String href) {
      collapsed = true;
      this.href = href;
   }

   public boolean isCollapsed() {
      return collapsed;
   }

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((href == null) ? 0 : href.hashCode());
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
      if (!(obj instanceof RESTData)) {
         return false;
      }
      RESTData other = (RESTData) obj;
      if (href == null) {
         if (other.href != null) {
            return false;
         }
      }
      else if (!href.equals(other.href)) {
         return false;
      }
      return true;
   }

   /*
    * Override this method to return specialized collapsed data objects.
    */
   public Object getCollapsedData() {
      return new CollapsedObject(getHref());
   }

   public static class CollapsedObject {

      private final String href;

      public CollapsedObject(String href) {
         this.href = href;
      }
   }
}
