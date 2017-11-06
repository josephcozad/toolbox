package com.jc.db.dao.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;

public class SQLGenerator {

   private final static String ALIAS_LIST = "ALIAS_LIST";

   public static String createCountSQL(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      EntityInfo entityInfo = new EntityInfo(entityClass);

      String queryString = createFindCountQuery(entityInfo, filterOn);

      String where_clause = "";
      if (filterOn != null) {
         StringBuilder clause = new StringBuilder(" where (");
         String filter = processFilterInfo(entityInfo, filterOn);
         clause.append(filter + ")");
         where_clause = clause.toString();
      }

      queryString += where_clause;

      return queryString;
   }

   public static String createSQL(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      Map<String, String> sortOn = new HashMap<String, String>();
      return createSQL(entityClass, filterOn, sortOn);
   }

   public static String createSQL(Class<?> entityClass, FilterInfo filterOn, Map<String, String> sortOn) throws Exception {
      EntityInfo entityInfo = new EntityInfo(entityClass);

      String queryString = createFindAllQuery(entityInfo, filterOn);

      String where_clause = "";
      if (filterOn != null) {
         StringBuilder clause = new StringBuilder(" where (");
         String filter = processFilterInfo(entityInfo, filterOn);
         clause.append(filter + ")");
         where_clause = clause.toString();
      }

      String sortby_clause = getOrderByClause(entityInfo, sortOn);
      queryString += where_clause + sortby_clause;

      return queryString;
   }

   public static List<Object> createParameterListForWhereClause(FilterInfo filterInfo) throws Exception {
      List<Object> parameterList = new ArrayList<Object>();
      if (filterInfo != null) {
         if (filterInfo.hasFilters()) {
            List<FilterInfo> filters = filterInfo.getFilters();
            for (int i = 0; i < filters.size(); i++) {
               FilterInfo filter = filters.get(i);
               List<Object> addedParamList = createParameterListForWhereClause(filter);
               if (!addedParamList.isEmpty()) {
                  parameterList.addAll(addedParamList);
               }
            }
         }
         else {
            FilterMethod methodType = filterInfo.getFilterMethod();
            if (!methodType.equals(FilterMethod.MATCH_FROM_START) && !methodType.equals(FilterMethod.MATCH_ANYWHERE)
                  && !methodType.equals(FilterMethod.MATCH_FROM_END) && !methodType.equals(FilterMethod.DOES_NOT_MATCH_FROM_START)
                  && !methodType.equals(FilterMethod.DOES_NOT_MATCH_ANYWHERE) && !methodType.equals(FilterMethod.DOES_NOT_MATCH_FROM_END)) {
               Object[] values = filterInfo.getFieldValues();
               if (values != null) {
                  if (values.length == 2) {
                     parameterList.add(values[0]);
                     parameterList.add(values[1]);
                  }
                  else {
                     if (isList(values[0])) {
                        List<?> aList = (List<?>) values[0];
                        int size = aList.size();
                        for (int i = 0; i < size; i++) {
                           parameterList.add(aList.get(i));
                        }
                     }
                     else {
                        parameterList.add(values[0]);
                     }
                  }
               }
            }
            // else don't add any parameter, this value is hard coded into the where clause ....
         }
      }
      return parameterList;
   }

   static String createVerifyDataInsertSQL(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      EntityInfo entityInfo = new EntityInfo(entityClass);

      String queryString = createFindAllQuery(entityInfo, filterOn);

      String whereClause = "";
      if (filterOn != null) {
         StringBuilder clause = new StringBuilder(" where (");
         String filter = processFilterInfo(entityInfo, filterOn);
         clause.append(filter + ")");
         whereClause = clause.toString();
      }

      String aliasedIdField = getIDColumnName(entityInfo);
      String lastRecordQuery = createFindLastRecordQuery(entityInfo);
      String additionalClause = " AND " + aliasedIdField + " = (" + lastRecordQuery + ")";

      queryString += whereClause + additionalClause;

      return queryString;
   }

   private static String createFindAllQuery(EntityInfo entityInfo, FilterInfo filterOn) throws Exception {
      Map<String, JoinInfo> joinInfoMap = entityInfo.getJoinInfo();
      JoinInfo selectInfo = entityInfo.getSelectInfo();

      StringBuilder aliases = new StringBuilder(selectInfo.pTableAlias + ".*,");
      StringBuilder query = new StringBuilder(selectInfo.toString());
      for (Map.Entry<String, JoinInfo> entry : joinInfoMap.entrySet()) {
         String alias = entry.getKey();
         aliases.append(alias + ".*,");

         String statement = entry.getValue().toString();
         query.append(statement);
      }

      String aliasList = aliases.substring(0, aliases.length() - 1);
      String queryStr = query.toString();
      queryStr = queryStr.replace(ALIAS_LIST, aliasList);
      return queryStr;
   }

   private static String createFindCountQuery(EntityInfo entityInfo, FilterInfo filterOn) throws Exception {
      Map<String, JoinInfo> joinInfoMap = entityInfo.getJoinInfo();
      JoinInfo selectInfo = entityInfo.getSelectInfo();

      StringBuilder query = new StringBuilder(selectInfo.toString());
      for (Map.Entry<String, JoinInfo> entry : joinInfoMap.entrySet()) {
         String statement = entry.getValue().toString();
         query.append(statement);
      }

      String aliasList = "count(*)";
      String queryStr = query.toString();
      queryStr = queryStr.replace(ALIAS_LIST, aliasList);
      return queryStr;
   }

   //
   //
   //

   private static String processFilterInfo(EntityInfo entityInfo, FilterInfo filterInfo) throws Exception {
      String clause = "";

      if (filterInfo.hasFilters()) {
         String logicOperator = " or ";
         if (filterInfo.isANDed()) {
            logicOperator = " and ";
         }

         List<FilterInfo> filters = filterInfo.getFilters();
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < filters.size(); i++) {
            FilterInfo filter = filters.get(i);
            String filterClause = processFilterInfo(entityInfo, filter);
            sb.append(filterClause);
            if (i + 1 < filters.size()) {
               sb.append(logicOperator);
            }
         }

         clause = "(" + sb.toString() + ")";
      }
      else {
         String filterKey = filterInfo.getFieldName();
         filterKey = entityInfo.getColumnNameByFieldName(filterKey);

         String filterString = getFilterString(filterInfo);
         clause = filterKey + filterString;
      }

      return clause;
   }

   private static String getFilterString(FilterInfo filterInfo) throws Exception {
      String filterValue = null;

      if (filterInfo.isNull()) {
         filterValue = " IS NULL";
      }
      else if (filterInfo.isNotNull()) {
         filterValue = " IS NOT NULL";
      }
      else {
         Object[] values = filterInfo.getFieldValues();
         FilterMethod methodType = filterInfo.getFilterMethod();
         String term = "?";

         if (values != null && values.length == 1) {
            if (isList(values[0])) {
               List<?> aList = (List<?>) values[0];
               int size = aList.size();
               StringBuilder sb = new StringBuilder();
               for (int i = 0; i < size; i++) {
                  sb.append("?");
                  if (i + 1 < size) {
                     sb.append(", ");
                  }
               }
               term = sb.toString();
            }
         }

         switch (methodType) {
            case MATCH_ANYWHERE:
               term = (String) values[0];
               filterValue = " like '%" + term + "%'";
               break;
            case DOES_NOT_MATCH_ANYWHERE:
               term = (String) values[0];
               filterValue = " not like '%" + term + "%'";
               break;
            case MATCH_FROM_START:
               term = (String) values[0];
               filterValue = " like '" + term + "%'";
               break;
            case DOES_NOT_MATCH_FROM_START:
               term = (String) values[0];
               filterValue = " not like '" + term + "%'";
               break;
            case MATCH_FROM_END:
               term = (String) values[0];
               filterValue = " like '%" + term + "'";
               break;
            case DOES_NOT_MATCH_FROM_END:
               term = (String) values[0];
               filterValue = " not like '%" + term + "'";
               break;
            case MATCH_EXACT:
               filterValue = " = " + term;
               break;
            case MATCH_OTHER_THAN:
               filterValue = " <> " + term;
               break;
            case LESSTHAN:
               filterValue = " < " + term;
               break;
            case GREATERTHAN:
               filterValue = " > " + term;
               break;
            case LESSTHAN_EQUALTO:
               filterValue = " <= " + term;
               break;
            case GREATERTHAN_EQUALTO:
               filterValue = " >= " + term;
               break;
            case IN_LIST:
               filterValue = " in (" + term + ")";
               break;
            case NOT_IN_LIST:
               filterValue = " not in (" + term + ")";
               break;
            case BETWEEN:
               filterValue = " between " + term + " and " + term;
               break;
            case NOT_BETWEEN:
               filterValue = " not between " + term + " and " + term;
               break;
            default:
               throw (new Exception("Unknown FilterMethod of '" + methodType + "'."));
         }
      }
      return filterValue;
   }

   private static String getOrderByClause(EntityInfo entityInfo, Map<String, String> sortOn) throws Exception {
      String clause = "";

      int num_sortables = sortOn.size();
      if (num_sortables > 0) {
         StringBuilder sort_clause = new StringBuilder(" order by ");
         int index = 0;
         for (Map.Entry<String, String> entry : sortOn.entrySet()) {
            String fieldName = entry.getKey();
            String columnName = entityInfo.getColumnNameByFieldName(fieldName);
            String direction = entry.getValue();

            sort_clause.append(columnName + " " + direction);
            if (index + 1 < num_sortables) {
               sort_clause.append(", ");
            }
            index++;
         }
         clause = sort_clause.toString();
      }

      return (clause);
   }

   private static boolean isList(Object value) {
      boolean isList = false;
      Class<?> valueClass = value.getClass();
      Class<?>[] interfaces = valueClass.getInterfaces();
      for (int i = 0; i < interfaces.length && !isList; i++) {
         isList = interfaces[i].equals(List.class);
      }
      return isList;
   }

   private static String createFindLastRecordQuery(EntityInfo entityInfo) throws Exception {
      FieldInfo fieldInfo = entityInfo.getIdFieldInfo();
      String tableName = entityInfo.getSchemaName() + "." + fieldInfo.getTableName();
      String tableAlias = "xxxx";
      String idColumnName = fieldInfo.getColumnName();
      return "SELECT max(" + tableAlias + "." + idColumnName + ") FROM " + tableName + " " + tableAlias;
   }

   private static String getIDColumnName(EntityInfo entityInfo) {
      FieldInfo fieldInfo = entityInfo.getIdFieldInfo();
      String columnName = fieldInfo.getColumnName();
      String tableName = fieldInfo.getTableName();
      String alias = entityInfo.getAliasForTableName(tableName);
      return alias + "." + columnName;
   }
}
