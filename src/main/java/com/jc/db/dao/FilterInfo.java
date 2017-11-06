package com.jc.db.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jc.db.DatabaseErrorCode;

public class FilterInfo {

   private String fieldName;
   private Object[] fieldValues;
   private int method; // the FilterMethod code...

   private List<FilterInfo> filters;

   @SuppressWarnings("unused")
   private FilterInfo() {}

   public FilterInfo(String fieldName, Object fieldValue, FilterMethod filterMethod) throws Exception {
      this(fieldName, fieldValue, null, filterMethod);
   }

   public FilterInfo(String fieldName, Object fromFieldValue, Object toFieldValue, FilterMethod filterMethod) throws Exception {

      verifyInputs(fieldName, fromFieldValue, toFieldValue, filterMethod);

      this.method = filterMethod.getCode();
      this.fieldName = fieldName;

      Object[] fieldValues = null;
      if (toFieldValue == null) {
         fieldValues = new Object[1];
         fieldValues[0] = fromFieldValue;
      }
      else {
         fieldValues = new Object[2];
         fieldValues[0] = fromFieldValue;
         fieldValues[1] = toFieldValue;
      }

      this.fieldValues = fieldValues;
   }

   public FilterInfo(String fieldName, FilterMethod filterMethod) {
      if (fieldName == null || fieldName.isEmpty()) {
         throw new IllegalArgumentException("'fieldName' cannot be null or empty.");
      }

      if (filterMethod == null) {
         throw new IllegalArgumentException("'filterMethod' cannot be null.");
      }

      if (!(filterMethod.equals(FilterMethod.IS_NULL)) && !(filterMethod.equals(FilterMethod.IS_NOT_NULL))) {
         throw new IllegalArgumentException("'filterMethod' must be IS_NULL or IS_NOT_NULL.");
      }

      this.method = filterMethod.getCode();
      this.fieldName = fieldName;
   }

   public FilterInfo(FilterMethod filterMethod, FilterInfo... filters) {
      if (filters != null && filters.length > 0) {
         List<FilterInfo> filterList = new ArrayList<>();
         for (FilterInfo filter : filters) {
            if (filter != null) {
               filterList.add(filter);
            }
         }

         if (filterList.size() == 1) {
            FilterInfo filter = filterList.get(0);
            this.method = filter.method;
            this.fieldName = filter.fieldName;
            this.fieldValues = filter.getFieldValues();
         }
         else {
            if (filterMethod.equals(FilterMethod.AND_FIELD_GROUP) || filterMethod.equals(FilterMethod.OR_FIELD_GROUP)) {
               this.filters = filterList;
               this.method = filterMethod.getCode();
            }
            else {
               throw new IllegalArgumentException(
                     "FilterMethod must be either '" + FilterMethod.AND_FIELD_GROUP + "' or '" + FilterMethod.OR_FIELD_GROUP + "' was '" + filterMethod + "'.");
            }
         }
      }
      else {
         throw new IllegalArgumentException("FilterInfo list is not allowed to be null or empty.");
      }
   }

   public boolean isANDed() {
      return getFilterMethod().isANDedMethodType();
   }

   public boolean isNull() {
      return getFilterMethod().isNull();
   }

   public boolean isNotNull() {
      return getFilterMethod().isNotNull();
   }

   public String getFieldName() {
      return fieldName;
   }

   public Object[] getFieldValues() {
      return fieldValues;
   }

   public FilterMethod getFilterMethod() {
      return FilterMethod.getFilterMethod(method);
   }

   public boolean hasFilters() {
      return filters != null && !filters.isEmpty();
   }

   public List<FilterInfo> getFilters() {
      return filters;
   }

   @Override
   public String toString() {
      String value = "";
      if (filters != null && !filters.isEmpty()) {
         String filterMethodStr = " AND ";
         if (getFilterMethod().equals(FilterMethod.OR_FIELD_GROUP)) {
            filterMethodStr = " OR ";
         }

         StringBuilder sb = new StringBuilder("(");
         int numFilters = filters.size();
         for (int i = 0; i < numFilters; i++) {
            FilterInfo filter = filters.get(i);
            sb.append(filter.toString());
            if (i + 1 < numFilters) {
               sb.append(filterMethodStr);
            }
         }
         sb.append(")");
         value = sb.toString();
      }
      else {
         String fieldValue = "";
         if (fieldValues != null) {
            if (fieldValues.length == 1) {
               fieldValue = fieldValues[0].toString();
            }
            else {
               fieldValue = fieldValues[0].toString() + " and " + fieldValues[1].toString();
            }
         }
         value = getFieldName() + " " + getFilterMethod() + " " + fieldValue;
      }
      return value;
   }

   private void verifyInputs(String fieldName, Object fromFieldValue, Object toFieldValue, FilterMethod filterMethod) throws Exception {

      if (fieldName == null || fieldName.isEmpty()) {
         throw new IllegalArgumentException("'fieldName' cannot be null or empty.");
      }

      if (filterMethod == null) {
         throw new IllegalArgumentException("'filterMethod' cannot be null.");
      }

      if (filterMethod.equals(FilterMethod.BETWEEN) || filterMethod.equals(FilterMethod.NOT_BETWEEN)) {
         if (fromFieldValue == null || toFieldValue == null) { // both field values are required.
            throw new InvalidFilterMethodException(DatabaseErrorCode.TWO_FIELD_VALUES_REQUIRED);
         }

         // both have to be the same type...
         if (!fromFieldValue.getClass().equals(toFieldValue.getClass())) {
            throw new IllegalArgumentException("Both field values are not of the same data type.");
         }

         // check that they are one of the allowable data types for this FilterMethod.
         if (!(fromFieldValue instanceof String) && !(fromFieldValue instanceof Number) && !(fromFieldValue instanceof Date)) {
            throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
         }
      }
      else if (filterMethod.equals(FilterMethod.IS_NULL) || filterMethod.equals(FilterMethod.IS_NOT_NULL)) {
         if (fromFieldValue != null || toFieldValue != null) { // no field value allowed.
            throw new InvalidFilterMethodException(DatabaseErrorCode.FIELD_VALUE_NOT_ALLOWED);
         }
      }
      else { // requires only a single value 'fromFieldValue'...
         if (fromFieldValue == null) {
            throw new InvalidFilterMethodException(DatabaseErrorCode.NULL_FIELD_VALUE);
         }
         else if (fromFieldValue != null && toFieldValue != null) {
            throw new InvalidFilterMethodException(DatabaseErrorCode.TOO_MANY_FIELD_VALUES);
         }

         // next, check that the field value is an accepted data type
         if (!(fromFieldValue instanceof String) && !(fromFieldValue instanceof Number) && !(fromFieldValue instanceof Boolean)
               && !(fromFieldValue instanceof Date) && !(fromFieldValue instanceof List)) {
            throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
         }

         if (fromFieldValue instanceof List && ((List<?>) fromFieldValue).isEmpty()) {
            throw new InvalidFilterMethodException(DatabaseErrorCode.EMPTY_LIST);
         }

         //         if (fromFieldValue instanceof String && ((String) fromFieldValue).isEmpty()) {
         //            throw new IllegalArgumentException("A String type 'fieldValue' cannot be empty.");
         //         }

         // check that the field value is one of the allowable data types for this FilterMethod.
         // DEV NOTE: commented out options below indicate that those options are valid for the data type.
         if (fromFieldValue instanceof String) {
            switch (filterMethod) {
               //     case MATCH_ANYWHERE:
               //     case MATCH_FROM_START:
               //     case MATCH_FROM_END:
               //     case MATCH_EXACT:
               //     case MATCH_OTHER_THAN:
               //     case DOES_NOT_MATCH_FROM_START:
               //     case DOES_NOT_MATCH_ANYWHERE:
               //     case DOES_NOT_MATCH_FROM_END:
               //     case LESSTHAN:
               //     case GREATERTHAN:
               //     case LESSTHAN_EQUALTO:
               //     case GREATERTHAN_EQUALTO:
               //     case IN_LIST:
               //     case NOT_IN_LIST:
               //throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
               default:
                  break;
            }
         }
         else if (fromFieldValue instanceof Number) {
            switch (filterMethod) {
               case MATCH_ANYWHERE:
               case MATCH_FROM_START:
               case MATCH_FROM_END:
                  //          case MATCH_EXACT:
                  //          case MATCH_OTHER_THAN:
               case DOES_NOT_MATCH_FROM_START:
               case DOES_NOT_MATCH_ANYWHERE:
               case DOES_NOT_MATCH_FROM_END:
                  //          case LESSTHAN:
                  //          case GREATERTHAN:
                  //          case LESSTHAN_EQUALTO:
                  //          case GREATERTHAN_EQUALTO:
                  //          case IN_LIST:
                  //          case NOT_IN_LIST:
                  throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
               default:
                  break;
            }
         }
         else if (fromFieldValue instanceof Boolean) {
            switch (filterMethod) {
               case MATCH_ANYWHERE:
               case MATCH_FROM_START:
               case MATCH_FROM_END:
                  //         case MATCH_EXACT:
                  //         case MATCH_OTHER_THAN:
               case DOES_NOT_MATCH_FROM_START:
               case DOES_NOT_MATCH_ANYWHERE:
               case DOES_NOT_MATCH_FROM_END:
               case LESSTHAN:
               case GREATERTHAN:
               case LESSTHAN_EQUALTO:
               case GREATERTHAN_EQUALTO:
               case IN_LIST:
               case NOT_IN_LIST:
                  throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
               default:
                  break;
            }
         }
         else if (fromFieldValue instanceof Date) {
            switch (filterMethod) {
               case MATCH_ANYWHERE:
               case MATCH_FROM_START:
               case MATCH_FROM_END:
                  //          case MATCH_EXACT:
                  //          case MATCH_OTHER_THAN:
               case DOES_NOT_MATCH_FROM_START:
               case DOES_NOT_MATCH_ANYWHERE:
               case DOES_NOT_MATCH_FROM_END:
                  //          case LESSTHAN:
                  //          case GREATERTHAN:
                  //          case LESSTHAN_EQUALTO:
                  //          case GREATERTHAN_EQUALTO:
                  //          case IN_LIST:
                  //          case NOT_IN_LIST:
                  throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
               default:
                  break;
            }
         }
         else if (fromFieldValue instanceof List) {
            switch (filterMethod) {
               case MATCH_ANYWHERE:
               case MATCH_FROM_START:
               case MATCH_FROM_END:
               case MATCH_EXACT:
               case MATCH_OTHER_THAN:
               case DOES_NOT_MATCH_FROM_START:
               case DOES_NOT_MATCH_ANYWHERE:
               case DOES_NOT_MATCH_FROM_END:
               case LESSTHAN:
               case GREATERTHAN:
               case LESSTHAN_EQUALTO:
               case GREATERTHAN_EQUALTO:
                  //        case IN_LIST:
                  //        case NOT_IN_LIST:
                  throw new InvalidFilterMethodException(DatabaseErrorCode.INVALID_DATATYPE);
               default:
                  break;
            }
         }
      }
   }
}
