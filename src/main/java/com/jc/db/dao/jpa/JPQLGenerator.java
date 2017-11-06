package com.jc.db.dao.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;

public class JPQLGenerator {

   public static String createJPQL(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      Map<String, String> sortOn = new HashMap<String, String>();
      return createJPQL(entityClass, filterOn, sortOn);
   }

   public static String createJPQL(Class<?> entityClass, FilterInfo filterOn, Map<String, String> sortOn) throws Exception {
      JPAEntityInfo entityInfo = new JPAEntityInfo(entityClass);

      List<String> joinOn = getJoinOnInfo(entityInfo, filterOn);
      String queryString = createFindAllQuery(entityInfo, joinOn, false);

      String where_clause = "";
      if (filterOn != null) {
         StringBuilder clause = new StringBuilder(" where ");
         String filter = processFilterInfo(entityInfo, filterOn);
         clause.append(filter);
         where_clause = clause.toString();
      }

      String sortby_clause = getOrderByClause(entityInfo, sortOn);
      queryString += where_clause + sortby_clause;

      return queryString;
   }

   public static String createCountJPQL(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      JPAEntityInfo entityInfo = new JPAEntityInfo(entityClass);

      List<String> joinOn = getJoinOnInfo(entityInfo, filterOn);
      String queryString = createFindAllQuery(entityInfo, joinOn, true); // include count code in JPQL...

      String where_clause = "";
      if (filterOn != null) {
         StringBuilder clause = new StringBuilder(" where ");
         String filter = processFilterInfo(entityInfo, filterOn);
         clause.append(filter);
         where_clause = clause.toString();
      }

      queryString += where_clause;

      return queryString;
   }

   public static List<Object> createParameterList(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      List<Object> parameterList = new ArrayList<Object>();
      if (filterOn != null) {
         JPAEntityInfo entityInfo = new JPAEntityInfo(entityClass);
         parameterList = createParameterListForWhereClause(entityInfo, filterOn);
      }
      return parameterList;
   }

   private static List<Object> createParameterListForWhereClause(JPAEntityInfo entityInfo, FilterInfo filterInfo) throws Exception {
      List<Object> parameterList = new ArrayList<Object>();
      if (filterInfo.hasFilters()) {
         List<FilterInfo> filters = filterInfo.getFilters();
         for (int i = 0; i < filters.size(); i++) {
            FilterInfo filter = filters.get(i);
            List<Object> addedParamList = createParameterListForWhereClause(entityInfo, filter);
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
                  if (!(values[0] instanceof List)) {
                     parameterList.add(values[0]);
                  }
                  else {
                     List<?> valueList = (List<?>) values[0];
                     for (int i = 0; i < valueList.size(); i++) {
                        parameterList.add(valueList.get(i));
                     }
                  }
               }
            }
         }
         // else don't add any parameter, this value is hard coded into the where clause ....
      }
      return parameterList;
   }

   private static String processFilterInfo(JPAEntityInfo entityInfo, FilterInfo filterInfo) throws Exception {
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
         String fieldName = filterInfo.getFieldName();
         if (entityInfo != null) {
            fieldName = entityInfo.getAliasedFieldName(fieldName);
         }

         String filterValue = getFilterString(filterInfo);
         clause = fieldName + filterValue;
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
         FilterMethod methodType = filterInfo.getFilterMethod();
         String term = "?";
         switch (methodType) {
            case MATCH_ANYWHERE:
               Object[] values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
               filterValue = " like '%" + term + "%'";
               break;
            case DOES_NOT_MATCH_ANYWHERE:
               values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
               filterValue = " not like '%" + term + "%'";
               break;
            case MATCH_FROM_START:
               values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
               filterValue = " like '" + term + "%'";
               break;
            case DOES_NOT_MATCH_FROM_START:
               values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
               filterValue = " not like '" + term + "%'";
               break;
            case MATCH_FROM_END:
               values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
               filterValue = " like '%" + term + "'";
               break;
            case DOES_NOT_MATCH_FROM_END:
               values = filterInfo.getFieldValues();
               term = (String) values[0];
               term = term.replace("'", "''");
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
               List<?> valueList = (List<?>) filterInfo.getFieldValues()[0];
               StringBuilder sb = new StringBuilder();
               for (int i = 0; i < valueList.size(); i++) {
                  sb.append("?");
                  if (i + 1 < valueList.size()) {
                     sb.append(", ");
                  }
               }
               term = sb.toString();

               filterValue = " in (" + term + ")";
               break;
            case NOT_IN_LIST:
               valueList = (List<?>) filterInfo.getFieldValues()[0];
               sb = new StringBuilder();
               for (int i = 0; i < valueList.size(); i++) {
                  sb.append("?");
                  if (i + 1 < valueList.size()) {
                     sb.append(", ");
                  }
               }
               term = sb.toString();

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

   private static String getOrderByClause(JPAEntityInfo entityInfo, Map<String, String> sortOn) throws Exception {
      String clause = "";

      int num_sortables = sortOn.size();
      if (num_sortables > 0 && entityInfo != null) {
         String selectAlias = entityInfo.getSelectAlias();
         StringBuilder sort_clause = new StringBuilder(" order by ");
         int index = 0;
         for (Map.Entry<String, String> entry : sortOn.entrySet()) {
            String fieldName = entry.getKey();
            //            if (entityInfo != null) {
            //               fieldName = entityInfo.getAliasedFieldName(fieldName);
            //            }

            String direction = entry.getValue();

            sort_clause.append(selectAlias + "." + fieldName + " " + direction);
            if (index + 1 < num_sortables) {
               sort_clause.append(", ");
            }
            index++;
         }
         clause = sort_clause.toString();
      }

      return (clause);
   }

   private static String createFindAllQuery(JPAEntityInfo entityInfo, List<String> joinOn, boolean includeCount) throws Exception {
      String queryStr = entityInfo.getSelectStatement(joinOn, includeCount);

      if (!joinOn.isEmpty()) {
         // Only add join statements for those fields listed in the JoinOn list.
         StringBuilder joinQuery = new StringBuilder();
         for (String fieldName : joinOn) {
            JPAJoinInfo joinInfo = entityInfo.getJoinInfoFor(fieldName);
            String statement = joinInfo.toString();
            joinQuery.append(statement);
         }

         queryStr += joinQuery.toString();
      }

      return queryStr;
   }

   private static List<String> getJoinOnInfo(JPAEntityInfo entityInfo, FilterInfo filterInfo) throws Exception {
      List<String> joinOnInfo = new ArrayList<String>();
      if (filterInfo != null) {
         if (filterInfo.hasFilters()) {
            List<FilterInfo> filters = filterInfo.getFilters();
            for (int i = 0; i < filters.size(); i++) {
               FilterInfo filter = filters.get(i);
               List<String> alist = getJoinOnInfo(entityInfo, filter);
               joinOnInfo.addAll(alist);
            }
         }
         else {
            String fieldName = filterInfo.getFieldName();
            JPAJoinInfo joinInfo = entityInfo.getJoinInfoFor(fieldName);
            if (joinInfo != null) {
               joinOnInfo.add(fieldName);
            }
         }
      }
      return joinOnInfo;
   }
}
