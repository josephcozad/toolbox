package com.jc.app.rest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jc.app.User;
import com.jc.app.rest.annotations.RDMetadata.Attribute;
import com.jc.app.rest.exceptions.InvalidFieldNameException;
import com.jc.app.rest.exceptions.InvalidJSONDatatypeException;
import com.jc.app.rest.exceptions.InvalidJSONException;
import com.jc.app.rest.exceptions.InvalidParameterException;
import com.jc.app.rest.exceptions.RESTErrorCode;
import com.jc.app.rest.exceptions.RESTServiceException;
import com.jc.app.rest.exceptions.RESTServiceLoggableException;
import com.jc.app.rest.exceptions.RESTServiceSystemInfoException;
import com.jc.app.rest.exceptions.UnauthorizedException;
import com.jc.db.DatabaseErrorCode;
import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;
import com.jc.db.dao.InvalidFilterMethodException;
import com.jc.exception.ErrorCode;
import com.jc.exception.SystemInfoException;
import com.jc.log.Logger;
import com.jc.shiro.AccessControlService;
import com.jc.util.StringUtils;

public abstract class RESTResource {

   public final static String QUERY_PARAM_COLLAPSIBLE = "_collapse";
   public final static String QUERY_PARAM_FIELDSLIST = "_fields";
   public final static String QUERY_PARAM_COUNTONLY = "_count";
   public final static String QUERY_PARAM_ITEMSONLY = "_items";

   public List<RESTMetadata> getMetadata(Class<?> referringClass, Class<?> aclass) {
      try {
         List<RESTMetadata> metadataList = RESTMetadata.getFieldMetadata(aclass);
         return metadataList;
      }
      catch (Exception ex) {
         Response response = createExceptionResponse(referringClass, ex);
         throw new WebApplicationException(response);
      }
   }

   protected List<?> extractParamRequiredDataAsList(String queryParamKey, UriInfo info, Class<? extends Object> valueClassObj) throws Exception {
      return extractParamDataAsList(queryParamKey, info, valueClassObj, true);
   }

   protected List<?> extractParamOptionalDataAsList(String queryParamKey, UriInfo info, Class<? extends Object> valueClassObj) throws Exception {
      return extractParamDataAsList(queryParamKey, info, valueClassObj, false);
   }

   protected Object extractRequiredValueFromJSONObject(String queryParamKey, JSONObject jsonObj, Class<? extends Object> valueClassObj) throws Exception {
      return extractValueFromJSONObject(queryParamKey, jsonObj, valueClassObj, true);
   }

   protected Object extractOptionalValueFromJSONObject(String queryParamKey, JSONObject jsonObj, Class<? extends Object> valueClassObj) throws Exception {
      return extractValueFromJSONObject(queryParamKey, jsonObj, valueClassObj, false);
   }

   /*
    * "{'message':'" + ex.getMessage() + "'}"
    * "{'errorCode':'" + ErrorCode.getCode + "', 'message':'" + ErrorCode.toString + "'}"
    * "{'errorCode':'" + ErrorCode.getCode + "', 'message':'" + ErrorCode.toString + "', 'parameterName':'" + parameterName + "'}"
    */

   public Response createExceptionResponse(RESTServiceException ex) {

      if (ex instanceof InvalidFilterMethodException) {
         DatabaseErrorCode errorCode = ((InvalidFilterMethodException) ex).getErrorCode();
         if (DatabaseErrorCode.INVALID_DATATYPE.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method does not support the data type of the specified field.");
         }
         else if (DatabaseErrorCode.EMPTY_LIST.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method requires a list of values.");
         }
         else if (DatabaseErrorCode.TOO_MANY_FIELD_VALUES.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method only accepts one field value.");
         }
         else if (DatabaseErrorCode.NULL_FIELD_VALUE.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method requires a field value.");
         }
         else if (DatabaseErrorCode.FIELD_VALUE_NOT_ALLOWED.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method does not accept a field value.");
         }
         else if (DatabaseErrorCode.TWO_FIELD_VALUES_REQUIRED.equals(errorCode)) {
            ex = new InvalidParameterException(errorCode, "Filter method requires a 'from' and a 'to' field value.");
         }
      }

      String type = MediaType.TEXT_PLAIN;
      String message = "";
      Status status = Status.INTERNAL_SERVER_ERROR;

      if (ex instanceof InvalidParameterException) {
         InvalidParameterException ipex = ((InvalidParameterException) ex);
         ErrorCode errorCode = ipex.getErrorCode();

         status = ipex.getStatus();

         JSONObject jsonErrorObj = new JSONObject();
         try {
            jsonErrorObj.put("errorCode", errorCode.getCode());
            jsonErrorObj.put("message", errorCode.toString());

            JSONArray paramsArray = new JSONArray();
            List<String> parameterList = ipex.getParameterList();
            for (String parameter : parameterList) {
               paramsArray.put(parameter);
            }

            jsonErrorObj.put("parameterName", paramsArray);
         }
         catch (Exception jex) {
            Logger.log(getClass(), Level.SEVERE, jex);
         }

         message = jsonErrorObj.toString();
      }
      else {
         ErrorCode errorCode = ex.getErrorCode();
         String errorMessage = ex.getMessage();
         if (errorMessage == null || errorMessage.isEmpty()) { //errorCode.getCode() == RESTErrorCode.INTERNAL_SERVICE_ERROR.getCode() || 
            errorMessage = errorCode.toString();
         }

         status = ex.getStatus();

         JSONObject jsonErrorObj = new JSONObject();
         try {
            jsonErrorObj.put("errorCode", errorCode.getCode());
            jsonErrorObj.put("message", errorMessage);
         }
         catch (Exception jex) {
            Logger.log(getClass(), Level.SEVERE, jex);
         }

         message = jsonErrorObj.toString();
      }

      ResponseBuilder builder = Response.status(status);
      builder.type(type);
      builder.entity(message);
      return builder.build();
   }

   public Response createExceptionResponse(Class<?> referringClass, Exception ex) {
      RESTServiceException rsex = null;

      if (ex instanceof SystemInfoException) {
         rsex = new RESTServiceSystemInfoException((SystemInfoException) ex);
      }
      else if (ex instanceof JSONException) {
         rsex = new InvalidJSONException((JSONException) ex);
      }
      else if (ex instanceof InvocationTargetException) {
         // This is for cases where reflection throws an error, some of which may be caused
         // by the RESTUtils class when converting datatypes.
         Throwable targetEx = ((InvocationTargetException) ex).getTargetException();
         if (targetEx instanceof RESTServiceException) {
            rsex = (RESTServiceException) targetEx;
         }
         else {
            rsex = new RESTServiceLoggableException(referringClass, Level.SEVERE, targetEx, RESTErrorCode.INTERNAL_SERVICE_ERROR);
         }
      }
      else if (!(ex instanceof RESTServiceException)) {
         rsex = new RESTServiceLoggableException(referringClass, Level.SEVERE, ex, RESTErrorCode.INTERNAL_SERVICE_ERROR);
      }
      else {
         rsex = (RESTServiceException) ex;
      }
      return createExceptionResponse(rsex);
   }

   public void validateRequiredField(String fieldname, Long value) throws Exception {
      if (value == null || value > -1) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }
   }

   public void validateRequiredField(String fieldname, Integer value) throws Exception {
      if (value == null || value > -1) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }
   }

   public void validateRequiredField(String fieldname, String value) throws Exception {
      if (value == null || value.isEmpty()) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }
   }

   public void validateRequiredField(String fieldname, List<?> alist) throws Exception {
      if (alist == null || alist.isEmpty()) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }
   }

   public void validateFieldValueType(String fieldname, String value, Class<?> valueClass) throws Exception {
      if (valueClass.equals(Long.TYPE) && !StringUtils.isLong(value)) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }

      if (valueClass.equals(Double.TYPE) && !StringUtils.isDouble(value)) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }

      if (valueClass.equals(Integer.TYPE) && !StringUtils.isInteger(value)) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_PARAMETER);
         inpex.addParameter(fieldname);
         throw inpex;
      }
   }

   public JSONObject validateWritableParameters(List<RESTMetadata> metadataList, JSONObject parameters, boolean modifyRequest) throws Exception {
      boolean isSubDatatype = false;
      return validateWritableParameters(metadataList, parameters, modifyRequest, isSubDatatype);
   }

   public Map<String, String> createSortOn(Class<?> referringClass, JSONObject sortInfo, Class<? extends RESTData> restDataClass) throws Exception {
      Map<String, String> newSortOn = new LinkedHashMap<>();

      List<RESTMetadata> metadataList = getMetadata(referringClass, restDataClass);

      String sortParamStr = sortInfo.toString();
      Map<String, String> sortOn = RESTUtils.createObjectMapFromJSONObject(new JSONObject(sortParamStr), String.class, String.class);

      // cycle through and fix fieldnames...
      if (!sortOn.isEmpty()) {
         for (Map.Entry<String, String> entry : sortOn.entrySet()) {
            String fieldName = entry.getKey();
            RESTMetadata fieldMetadata = getFilterFieldMetadata(fieldName, metadataList);
            if (fieldMetadata != null && fieldMetadata.isSortable()) {
               String value = entry.getValue();
               fieldName = convertFieldName(fieldName, metadataList, restDataClass);
               newSortOn.put(fieldName, value);
            }
            else {
               InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_SORT_FIELD);
               inpex.addParameter(fieldName);
               throw inpex;
            }
         }
      }

      return newSortOn;
   }

   public FilterInfo createFilterOn(Class<?> referringClass, JSONObject filter, Class<? extends RESTData> restDataClass) throws Exception {
      FilterInfo filterOn = null;

      List<RESTMetadata> metadataList = getMetadata(referringClass, restDataClass);

      int method = filter.getInt("method");
      FilterMethod filterMethod = FilterMethod.getFilterMethod(method);
      if (filter.has("filters")) {

         JSONArray subFilters = filter.getJSONArray("filters");
         FilterInfo[] filterInfoArray = new FilterInfo[subFilters.length()];
         for (int i = 0; i < subFilters.length(); i++) {
            JSONObject subFilter = subFilters.getJSONObject(i);
            FilterInfo subFilterInfo = createFilterOn(referringClass, subFilter, restDataClass);
            filterInfoArray[i] = subFilterInfo;
         }
         filterOn = new FilterInfo(filterMethod, filterInfoArray);
      }
      else if (filter.has("fieldName")) {

         String fieldName = filter.getString("fieldName");
         RESTMetadata fieldMetadata = getFilterFieldMetadata(fieldName, metadataList);
         if (fieldMetadata != null) {

            if (fieldMetadata.isFilterable()) {
               if (!fieldMetadata.getFieldTypeMetadata().isEmpty()) { // filter field cannot itself be a datatype.
                  throw new InvalidFieldNameException(RESTErrorCode.INVALID_FILTER_FIELD, "Filter fieldName '" + fieldName
                        + "' cannot point to a sub datatype, please specify a valid fieldName such as '" + fieldName + ".someField'.");
               }

               // fieldName validation for only single value
               Pattern fieldNamePattern = Pattern.compile("^\\w+(?:\\.?\\w+)+$");
               Matcher wordMatcher = fieldNamePattern.matcher(fieldName);
               if (!wordMatcher.matches()) {
                  throw new InvalidFieldNameException(RESTErrorCode.INVALID_FILTER_FIELD, "Filter field name must be a single value.");
               }

               fieldName = convertFieldName(fieldName, metadataList, restDataClass);

               if (filter.has("fieldValues")) {

                  Object fieldValuesObj = filter.get("fieldValues");
                  JSONArray valuesArray = extractFilterFieldValuesJSONArray(fieldValuesObj, method);
                  Class<?> filterFieldType = fieldMetadata.getFieldType();
                  if (filterFieldType != null) {
                     boolean isList = false;
                     if (valuesArray.get(0) instanceof JSONArray) {
                        isList = true;
                     }

                     Object[] values = new Object[2];
                     if (filterFieldType.equals(String.class)) {
                        if (!isList) {
                           values[0] = valuesArray.getString(0);
                           if (valuesArray.length() == 2) {
                              values[1] = valuesArray.getString(1);
                           }
                        }
                        else {
                           List<String> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              String valueStr = valueArray.getString(i);
                              valueList.add(valueStr);
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(Long.class)) {
                        if (!isList) {
                           values[0] = valuesArray.getLong(0);
                           if (valuesArray.length() == 2) {
                              values[1] = valuesArray.getLong(1);
                           }
                        }
                        else {
                           List<Long> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              Long valueStr = valueArray.getLong(i);
                              valueList.add(valueStr);
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(Date.class)) {
                        if (!isList) {
                           try {
                              String dateStr = valuesArray.getString(0);
                              values[0] = RESTUtils.DATE_FORMAT_ISO8601.parse(dateStr);
                           }
                           catch (ParseException pex) {
                              InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_DATE_FORMAT);
                              inpex.addParameter(fieldName);
                              throw inpex;
                           }

                           if (valuesArray.length() == 2) {
                              try {
                                 String dateStr = valuesArray.getString(1);
                                 values[1] = RESTUtils.DATE_FORMAT_ISO8601.parse(dateStr);
                              }
                              catch (ParseException pex) {
                                 InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.INVALID_DATE_FORMAT);
                                 inpex.addParameter(fieldName);
                                 throw inpex;
                              }
                           }
                        }
                        else {
                           List<Date> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              String valueStr = valueArray.getString(i);
                              valueList.add(RESTUtils.DATE_FORMAT_ISO8601.parse(valueStr));
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(BigInteger.class)) {
                        if (!isList) {
                           values[0] = new BigInteger(valuesArray.getString(0));
                           if (valuesArray.length() == 2) {
                              values[1] = new BigInteger(valuesArray.getString(1));
                           }
                        }
                        else {
                           List<BigInteger> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              String valueStr = valueArray.getString(i);
                              valueList.add(new BigInteger(valueStr));
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(Boolean.class)) {
                        values[0] = valuesArray.getBoolean(0);
                     }
                     else if (filterFieldType.equals(Integer.class)) {
                        if (!isList) {
                           values[0] = valuesArray.getInt(0);
                           if (valuesArray.length() == 2) {
                              values[1] = valuesArray.getInt(1);
                           }
                        }
                        else {
                           List<Integer> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              Integer valueStr = valueArray.getInt(i);
                              valueList.add(valueStr);
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(Double.class)) {
                        if (!isList) {
                           values[0] = valuesArray.getDouble(0);
                           if (valuesArray.length() == 2) {
                              values[1] = valuesArray.getDouble(1);
                           }
                        }
                        else {
                           List<Double> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              Double valueStr = valueArray.getDouble(i);
                              valueList.add(valueStr);
                           }
                           values[0] = valueList;
                        }
                     }
                     else if (filterFieldType.equals(Float.class)) {
                        if (!isList) {
                           values[0] = new Float(valuesArray.getDouble(0));
                           if (valuesArray.length() == 2) {
                              values[1] = new Float(valuesArray.getDouble(1));
                           }
                        }
                        else {
                           List<Float> valueList = new ArrayList<>();
                           JSONArray valueArray = (JSONArray) valuesArray.get(0);
                           for (int i = 0; i < valueArray.length(); i++) {
                              Double valueStr = valueArray.getDouble(i);
                              valueList.add(new Float(valueStr));
                           }
                           values[0] = valueList;
                        }
                     }

                     if (values[1] != null) {
                        filterOn = new FilterInfo(fieldName, values[0], values[1], filterMethod);
                     }
                     else if (values[0] != null) {
                        filterOn = new FilterInfo(fieldName, values[0], filterMethod);
                     }
                     else {
                        throw new Exception("Unable to convert supplied FilterInfo value of type '" + filterFieldType + "' for field '" + fieldName + "'.");
                     }
                  }
                  else {
                     throw new InvalidFieldNameException(RESTErrorCode.INVALID_FILTER_FIELD, fieldName);
                  }
               }
               else {
                  // no field values... should be a valid filter method that doesn't take fields...
                  filterOn = new FilterInfo(fieldName, filterMethod);
               }
            }
            else {
               throw new InvalidFieldNameException(RESTErrorCode.INVALID_FILTER_FIELD, "Filter fieldName '" + fieldName + "' is not filterable.");
            }
         }
         else {
            throw new InvalidFieldNameException(RESTErrorCode.INVALID_FILTER_FIELD, fieldName);
         }
      }
      return filterOn;
   }

   protected int getIntValueFromParameters(String jsonKey, JSONObject parameters) throws Exception {
      int intValue = 0; // default
      if (parameters.has(jsonKey)) {
         Object value = parameters.get(jsonKey);
         if (value instanceof Number) {
            intValue = parameters.getInt(jsonKey);
            if (intValue < 0) {
               intValue = 0;
            }
         }
         else {
            throw new InvalidJSONDatatypeException("'" + jsonKey + "' value must be an integer.");
         }
      }
      return intValue;
   }

   protected List<String> getFilterableFields(Class<?> referringClass, Class<?> aclass) throws Exception {
      try {
         List<String> filterableFields = new ArrayList<>();

         List<RESTMetadata> metadataList = RESTMetadata.getFieldMetadata(aclass);
         for (RESTMetadata mdata : metadataList) {
            if (mdata.isFilterable()) {
               filterableFields.add(mdata.getFieldName());
            }
         }

         return (filterableFields);
      }
      catch (Exception ex) {
         Response response = createExceptionResponse(referringClass, ex);
         throw new WebApplicationException(response);
      }
   }

   protected void requestAllowed(String permission) throws Exception {
      User user = (User) AccessControlService.getUser();
      boolean ok = user.isAllowed(permission);
      if (!ok) {
         Class<?> parentClass = getClass().getSuperclass();
         throw new UnauthorizedException(parentClass, permission, user.getUsername());
      }
   }

   protected QueryParamInfo getQueryParamInfoFor(String queryParamKey, UriInfo info) throws Exception {
      QueryParamInfo queryParamInfo = new QueryParamInfo();

      Map<String, List<String>> queryParams = info.getQueryParameters();
      if (queryParams.containsKey(queryParamKey)) {

         List<String> paramValue = queryParams.get(queryParamKey);
         if (paramValue != null && !paramValue.isEmpty()) {

            String paramsData = paramValue.get(0);
            String[] paramsList = paramsData.split("\\,", -1);
            for (String parameter : paramsList) {
               queryParamInfo.addParameter(parameter);
            }
         }
      }
      return queryParamInfo;
   }

   protected boolean hasQueryParamFor(String queryParamKey, UriInfo info) throws Exception {
      boolean hasParam = false;
      Map<String, List<String>> queryParams = info.getQueryParameters();
      if (queryParams.containsKey(queryParamKey)) {
         hasParam = true;
      }
      return hasParam;
   }

   /*
    * This method is designed to allow inheriting classes to "convert" the supplied fieldName 
    * into one that can be used against the supplied metadataList to access the correct metadata 
    * for the supplied fieldName.
    */
   protected String convertFieldName(String fieldName, List<RESTMetadata> metadataList, Class<? extends RESTData> restDataClass) throws Exception {
      return fieldName;
   }

   // PRIVATE METHODS -------------------------------------------------------------------------------

   /*
    * Takes a list of metadata objects and compares them to the parameters for the request;
    * removing all passed in parameters that are read only and then verifying that the
    * required non-read only parameters have values present.
    */
   private JSONObject validateWritableParameters(List<RESTMetadata> metadataList, JSONObject parameters, boolean modifyRequest, boolean isSubDatatype)
         throws Exception {

      List<String> unknownParamsList = new LinkedList<>(Arrays.asList(JSONObject.getNames(parameters))); // remove fieldnames as they are found; any left after processing are unknown.
      for (RESTMetadata mdata : metadataList) {
         String fieldname = mdata.getFieldName();
         Class<?> fieldType = mdata.getFieldType();

         Map<String, List<RESTMetadata>> subMetadataMap = mdata.getFieldTypeMetadata(); // field's sub-entity metadata
         if (subMetadataMap != null && !subMetadataMap.isEmpty()) {
            Map<String, List<RESTMetadata>> subMetadataMapCopy = new HashMap<>(subMetadataMap);
            if (parameters.has(fieldname)) {
               unknownParamsList.remove(fieldname); // remove known param from unknown params list...

               Object jsonValue = parameters.get(fieldname); // get the JSON data for the fieldname...
               if (jsonValue instanceof JSONArray) {
                  JSONArray subParams = (JSONArray) jsonValue;
                  JSONArray validatedParams = new JSONArray();
                  for (Map.Entry<String, List<RESTMetadata>> entry : subMetadataMapCopy.entrySet()) {
                     //          String key = entry.getKey();
                     List<RESTMetadata> subFieldInfo = entry.getValue();
                     for (int i = 0; i < subParams.length(); i++) {
                        JSONObject param = subParams.getJSONObject(i);
                        param = validateWritableParameters(subFieldInfo, param, modifyRequest, true);
                        String[] names = JSONObject.getNames(param);
                        if (names != null && names.length > 0) {
                           validatedParams.put(param);
                        }
                     }

                  }
                  parameters.remove(fieldname);
                  parameters.put(fieldname, validatedParams);
               }
               else {
                  JSONObject subParam = (JSONObject) jsonValue;
                  List<String> jsonNamesList = new ArrayList<>();
                  for (Map.Entry<String, List<RESTMetadata>> entry : subMetadataMapCopy.entrySet()) {
                     String key = entry.getKey();
                     List<RESTMetadata> subFieldInfo = entry.getValue();
                     subParam = validateWritableParameters(subFieldInfo, subParam, modifyRequest, true);
                     String[] names = JSONObject.getNames(subParam);
                     if (names != null && names.length > 0) {
                        jsonNamesList.addAll(Arrays.asList(names));
                     }

                  }
                  parameters.remove(fieldname);
                  if (!jsonNamesList.isEmpty()) { // checking to see if it's empty...
                     parameters.put(fieldname, subParam);
                  }
               }
            }
         }
         else {

            if (!isSubDatatype && mdata.isIdField()) {
               if (!modifyRequest && parameters.has(fieldname)) { // adding...
                  throw new InvalidFieldNameException(RESTErrorCode.INVALID_USE_OF_ID_FIELD, "Id field not allowed when adding data."); // id is not allowed to be included on add...
               }
               else if (modifyRequest && !parameters.has(fieldname)) { // modifying...
                  InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER); // id is required and missing...
                  inpex.addParameter(mdata.getFieldName());
                  throw inpex;
               }
            }

            unknownParamsList.remove(fieldname); // remove known param from unknown params list...

            Attribute attribute = mdata.getAddDataAttribute();
            if (modifyRequest) {
               attribute = mdata.getModifyDataAttribute();
            }

            switch (attribute) {
               case FIELD_REQUIRED_READ_ONLY:
               case FIELD_REQUIRED_MODIFIABLE:
                  // validate value since it's required...
                  if (parameters.has(fieldname)) { // do the parameters have the required field..
                     String value = parameters.getString(fieldname);
                     validateRequiredField(fieldname, value); // throws exception on failure...
                     value = value.replace(";", "");
                     parameters.remove(fieldname);

                     if (!fieldType.equals(String.class)) {
                        if (fieldType.equals(Integer.class)) {
                           parameters.put(fieldname, new Integer(value));
                        }
                        else if (fieldType.equals(Long.class)) {
                           parameters.put(fieldname, new Long(value));
                        }
                        else if (fieldType.equals(Boolean.class)) {
                           parameters.put(fieldname, new Boolean(value));
                        }
                        else if (fieldType.equals(Double.class)) {
                           parameters.put(fieldname, new Double(value));
                        }
                        else if (fieldType.equals(Float.class)) {
                           parameters.put(fieldname, new Float(value));
                        }
                        else {
                           throw new Exception("Unsupported fieldType of '" + fieldType + "' for '" + fieldname + "'.");
                        }
                     }
                     else {
                        parameters.put(fieldname, value);
                     }
                  }
                  else {
                     InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
                     inpex.addParameter(fieldname);
                     throw inpex;
                  }
                  break;
               case FIELD_READ_ONLY:
                  // remove value, field not modifiable or invalid for this operation....
                  if (parameters.has(fieldname)) {
                     parameters.remove(fieldname);
                  }
                  break;
               case FIELD_OPTIONAL_MODIFIABLE:
                  // ignore, good as is...
                  break;
               default:
                  throw new Exception("Unable to determine RESTMetadata conditional settings for field '" + mdata.getFieldName());
            }
         }
      }

      if (!unknownParamsList.isEmpty()) {
         // remove parameters passed in that we don't recognize...
         for (String unknownParam : unknownParamsList) {
            if (parameters.has(unknownParam)) {
               parameters.remove(unknownParam);
            }
         }
      }

      return parameters;
   }

   /*
    * The FilterInfo class contains an array of Objects (Object[]) that holds a single field value,
    * two field values if the method is a BETWEEN or NOT_BETWEEN filter method, or it contains a 
    * single array of field values to be used IN_LIST or NOT_IN_LIST filter method. This method 
    * attempts to correctly extract from the supplied fieldValuesObj one of these three scenarios, 
    * and return a JSONArray that can be used to correctly assign the field values for the FilterInfo
    * object being inflated.
    */
   private static JSONArray extractFilterFieldValuesJSONArray(Object fieldValuesObj, int method) throws Exception {
      JSONArray valuesArray = null;

      if (!(fieldValuesObj instanceof JSONArray)) { // a single value... 
         valuesArray = new JSONArray();
         valuesArray.put(fieldValuesObj);
      }
      else { // is JSONArray...
         JSONArray fieldValuesArray = (JSONArray) fieldValuesObj;

         int numElements = fieldValuesArray.length();
         if (numElements == 1) { // only holds one item..
            Object item = fieldValuesArray.get(0);
            if (item instanceof JSONArray) { // item is itself a JSONArray, everything is good...
               valuesArray = fieldValuesArray;
            }
            else { // item is not a JSONArray, figure out what it is...
               if (FilterMethod.getFilterMethod(method).equals(FilterMethod.IN_LIST) || FilterMethod.getFilterMethod(method).equals(FilterMethod.NOT_IN_LIST)) {
                  // item is a single item that is intended to be in a list.
                  JSONArray jArray = new JSONArray();
                  jArray.put(item);
                  valuesArray = new JSONArray();
                  valuesArray.put(jArray);
               }
               else {
                  // item is a single item that is not intended to be in a list.
                  valuesArray = new JSONArray();
                  valuesArray.put(item);
               }
            }
         }
         else { // holds more than one item...
            if (FilterMethod.getFilterMethod(method).equals(FilterMethod.IN_LIST) || FilterMethod.getFilterMethod(method).equals(FilterMethod.NOT_IN_LIST)) {
               // items are intended to be in a list...
               JSONArray jArray = new JSONArray();
               for (int i = 0; i < fieldValuesArray.length(); i++) {
                  Object item = fieldValuesArray.get(i);
                  jArray.put(item);
               }
               valuesArray = new JSONArray();
               valuesArray.put(jArray);
            }
            else {
               // items were not intended to be in a list ... don't worry about it... error will follow.
               valuesArray = fieldValuesArray;
            }
         }
      }

      return valuesArray;
   }

   // Takes the fieldName and supplied metadataList and looks for a related RESTMetadata object.
   private RESTMetadata getFilterFieldMetadata(String fieldName, List<RESTMetadata> metadataList) {

      RESTMetadata filterFieldMetadata = null;
      if (fieldName.contains(".")) {
         String currentFieldName = fieldName.substring(0, fieldName.indexOf("."));
         fieldName = fieldName.substring(fieldName.indexOf(".") + 1, fieldName.length());

         for (RESTMetadata mdata : metadataList) {
            String testFieldName = mdata.getFieldName();
            if (currentFieldName.equals(testFieldName)) {
               Map<String, List<RESTMetadata>> subMetadataMap = mdata.getFieldTypeMetadata(); // field's sub-entity metadata
               if (subMetadataMap.size() > 1) {
                  // there's more than one set of metadata, see if the fieldName has a pointer to which set to use..
                  String metadataKey = null;
                  if (fieldName.contains(".")) { // fieldname could have embedded in it a pointer to the needed metadata list...
                     metadataKey = fieldName.substring(0, fieldName.indexOf("."));
                     metadataKey = metadataKey.substring(0, 1).toUpperCase() + metadataKey.substring(1); // Capitalize the first letter...
                  }

                  if (metadataKey != null && subMetadataMap.containsKey(metadataKey)) { // look to see if the subMetadataMap has a list by the metadataKey parsed..
                     fieldName = fieldName.substring(fieldName.indexOf(".") + 1, fieldName.length());
                     List<RESTMetadata> subMetadataList = subMetadataMap.get(metadataKey);
                     filterFieldMetadata = getFilterFieldMetadata(fieldName, subMetadataList);
                  }
                  else { // fieldName doesn't have a pointer to which one to use, just search through all of them for a field/metadata match...
                     for (Map.Entry<String, List<RESTMetadata>> entry : subMetadataMap.entrySet()) {
                        List<RESTMetadata> subMetadataList = entry.getValue();
                        filterFieldMetadata = getFilterFieldMetadata(fieldName, subMetadataList);
                        if (filterFieldMetadata != null) {
                           break;
                        }
                     }
                  }
               }
               else { // there's only one just use that one to find the match....
                  for (List<RESTMetadata> subMetadataList : subMetadataMap.values()) {
                     filterFieldMetadata = getFilterFieldMetadata(fieldName, subMetadataList);
                     break;
                  }
               }
               break;
            }
         }
      }
      else {
         for (RESTMetadata mdata : metadataList) {
            String testFieldName = mdata.getFieldName();
            if (fieldName.equals(testFieldName)) {
               filterFieldMetadata = mdata;
               break;
            }
         }
      }
      return filterFieldMetadata;
   }

   /*
    * This method takes the supplied parameter to extract a comma separated list of values
    * from the supplied UriInfo object and return a List object of those values where the 
    * supplied valueClassObj is the Class type of the value. Note that the Class type must 
    * have a constructor that takes a single String parameter.
    */
   private List<?> extractParamDataAsList(String queryParamKey, UriInfo info, Class<? extends Object> valueClassObj, boolean requiredValue) throws Exception {
      List<Object> parameterValueList = new ArrayList<>();
      String valueListStr = info.getQueryParameters().getFirst(queryParamKey);

      if (requiredValue) {
         validateRequiredField(queryParamKey, valueListStr);
      }

      if (valueListStr != null && !valueListStr.isEmpty()) {
         valueListStr = valueListStr.replace(" ", ""); // remove any spaces...

         String[] valuesArray = null;
         if (valueListStr.contains(",")) {
            valuesArray = valueListStr.split("\\,");
         }
         else {
            valuesArray = new String[1];
            valuesArray[0] = valueListStr;
         }

         for (int i = 0; i < valuesArray.length; i++) {
            validateFieldValueType(queryParamKey, valuesArray[i], valueClassObj);
            Constructor<? extends Object> constructor = valueClassObj.getConstructor(String.class);
            Object value = constructor.newInstance(valuesArray[i]);
            parameterValueList.add(value);
         }
      }

      return parameterValueList;
   }

   /*
    * This method takes the supplied parameter to extract a single value from the supplied 
    * JSONObject object the value where the supplied valueClassObj is the Class type of the 
    * value. Note that the Class type must have a constructor that takes a single 
    * String parameter.
    */
   private Object extractValueFromJSONObject(String queryParamKey, JSONObject jsonObj, Class<? extends Object> valueClassObj, boolean requiredValue)
         throws Exception {
      Object value = null;

      if (jsonObj.has(queryParamKey)) {
         String valueStr = jsonObj.getString(queryParamKey);

         validateFieldValueType(queryParamKey, valueStr, valueClassObj);
         Constructor<? extends Object> constructor = valueClassObj.getConstructor(String.class);
         value = constructor.newInstance(valueStr);
      }
      else if (requiredValue) {
         InvalidParameterException inpex = new InvalidParameterException(RESTErrorCode.MISSING_REQUIRED_PARAMETER);
         inpex.addParameter(queryParamKey);
         throw inpex;
      }
      return value;
   }
}
