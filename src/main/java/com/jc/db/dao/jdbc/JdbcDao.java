package com.jc.db.dao.jdbc;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jc.db.dao.Dao;
import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;

public abstract class JdbcDao<K, E> implements Dao<K, E> {

   private final Class<E> entityClass;
   private EntityInfo entityInfo;

   private JdbcEntityManager MyEntityManager;

   private String datasource;
   private String passwordSecurityKey;

   @SuppressWarnings("unchecked")
   public JdbcDao() {
      ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
      entityClass = (Class<E>) genericSuperclass.getActualTypeArguments()[1];
      entityInfo = new EntityInfo(entityClass);
   }

   public JdbcDao(String datasource) throws Exception {
      this();
      this.datasource = datasource;
   }

   public JdbcDao(String datasource, String passwordSecurityKey) throws Exception {
      this();
      this.datasource = datasource;
      this.passwordSecurityKey = passwordSecurityKey;
   }

   public JdbcDao(JdbcEntityManager entityManager) throws Exception {
      this();
      if (entityManager != null) {
         if (entityManager.isOpen()) {
            MyEntityManager = entityManager;
         }
         else {
            throw (new Exception("Supplied entity manager was not open."));
         }
      }
      else {
         throw (new Exception("Supplied entity manager was null."));
      }
   }

   protected JdbcEntityManager getEntityManager() throws Exception {
      JdbcEntityManager entityManager = MyEntityManager;
      if (entityManager == null) {
         if ((datasource != null && !datasource.isEmpty()) && (passwordSecurityKey != null && !passwordSecurityKey.isEmpty())) {
            entityManager = JDBCTransaction.createEntityManager(datasource, passwordSecurityKey);
         }
         else if (datasource != null && !datasource.isEmpty()) {
            entityManager = JDBCTransaction.createEntityManager(datasource);
         }
         else {
            entityManager = JDBCTransaction.createEntityManager();
         }
      }
      return (entityManager);
   }

   @Override
   public E addData(E data) throws Exception {
      return persistData(data, SAVE);
   }

   @Override
   public List<E> addData(List<E> dataList) throws Exception {
      ArrayList<E> updatedList = new ArrayList<E>();
      for (E data : dataList) {
         data = addData(data);
         updatedList.add(data);
      }
      return updatedList;
   }

   @Override
   public E updateData(E data) throws Exception {
      return persistData(data, UPDATE);
   }

   @Override
   public List<E> updateData(List<E> dataList) throws Exception {
      ArrayList<E> updatedList = new ArrayList<E>();
      for (E data : dataList) {
         data = updateData(data);
         updatedList.add(data);
      }
      return updatedList;
   }

   @Override
   public void removeData(List<E> dataList) throws Exception {
      for (E data : dataList) {
         removeData(data);
      }
   }

   @Override
   public E persistData(E data, int presistType) throws Exception {
      JdbcEntityManager entityManager = getEntityManager();
      boolean previouslyStarted = entityManager.transactionStarted();

      boolean transactionCompleted = false;
      try {
         if (!previouslyStarted) {
            entityManager.startTransaction();
         }

         if (presistType == SAVE) {
            entityManager.persist(data); // do an Insert
         }
         else {
            entityManager.merge(data); // do an Update
         }

         if (!previouslyStarted) {
            entityManager.endTransaction();
            transactionCompleted = true;
         }

         // this has to be done once the transaction is ended and the insert is commited.
         if (presistType == SAVE) {
            data = verifyDataInsert(data);
         }

         return data;
      }
      catch (Exception ex) {
         if (previouslyStarted) {
            // Only package the exception and send it back, rollback should be handled there
            // in a "global" scenario.
            throw ex;
         }
         else {
            // local scenario... handle rollback.
            String message = "";
            if (!transactionCompleted) {
               entityManager.rollBackTransaction();
               message = "Error while persisting data. " + ex.getMessage() + " All data was rolled back and no data was saved to the database.";
            }
            else {
               message = "Error while persisting data. " + ex.getMessage()
                     + " The data may have been added to the database but failure occured when trying to retrieve the data.";
            }
            throw new Exception(message);
         }
      }
   }

   @Override
   public void removeData(E data) throws Exception {
      JdbcEntityManager entityManager = getEntityManager();
      boolean previouslyStarted = entityManager.transactionStarted();

      try {
         if (!previouslyStarted) {
            entityManager.startTransaction();
         }

         entityManager.remove(data); // do a Delete

         if (!previouslyStarted) {
            entityManager.endTransaction();
         }
      }
      catch (Exception ex) {
         if (previouslyStarted) {
            // Only package the exception and send it back, rollback should be handled there
            // in a "global" scenario.
            throw ex;
         }
         else {
            // local scenario... handle rollback.
            entityManager.rollBackTransaction();

            String message = "Error while removing data. " + ex.getMessage() + " All data was rolled back and no data was saved to the database.";
            throw new Exception(message);
         }
      }
   }

   @Override
   public E findById(K id) throws Exception {

      FieldInfo idFieldInfo = entityInfo.getIdFieldInfo();
      String fieldName = idFieldInfo.getFieldName();

      int start = 0;
      int length = 0;
      Map<String, String> sortOn = new HashMap<String, String>();

      FilterInfo filterInfo = new FilterInfo(fieldName, id, FilterMethod.MATCH_EXACT);

      List<E> dataList = findAll(start, length, sortOn, filterInfo);
      if (!dataList.isEmpty() && dataList.size() == 1) {
         return dataList.get(0);
      }
      else if (dataList.isEmpty()) {
         return null;
      }
      else {
         throw new Exception("More than one record found for id " + id + ".");
      }
   }

   @Override
   public List<E> findAll(int start, int length, Map<String, String> sortOn, FilterInfo filterOn) throws Exception {
      return (List<E>) findAll(entityClass, start, length, sortOn, filterOn);
   }

   @Override
   public long countAll(FilterInfo filterOn) throws Exception {
      return countAll(entityClass, filterOn);
   }

   @Override
   public List<E> getDataListByField(String fieldname, Object fieldvalue, FilterMethod methodType) throws Exception {
      int start = 0;
      int length = 0;
      Map<String, String> sortOn = new HashMap<String, String>();

      if (fieldvalue instanceof List<?>) {
         if ((!methodType.equals(FilterMethod.IN_LIST)) || (!methodType.equals(FilterMethod.NOT_IN_LIST))) {
            methodType = FilterMethod.IN_LIST; // if fieldvalue is a List, force to list and assume positive
         }
      }

      FilterInfo filterInfo = new FilterInfo(fieldname, fieldvalue, methodType);
      List<E> data = findAll(start, length, sortOn, filterInfo);
      return data;
   }

   @Override
   public E getDataByField(String fieldname, Object fieldvalue, FilterMethod methodType) throws Exception {
      List<E> results = getDataListByField(fieldname, fieldvalue, methodType);
      if (results.isEmpty()) {
         return null;
      }
      else if (results.size() > 1) {
         throw (new Exception("More than one record was found for field name " + fieldname + " and value of " + fieldvalue + "."));
      }
      else {
         return results.get(0);
      }
   }

   protected List<?> findAll(Class<?> entityClass, int start, int length, Map<String, String> sortOn, FilterInfo filterOn) throws Exception {
      JdbcEntityManager entityManager = null;
      List<Object> parameterList = null;
      String queryString = "";

      try {
         queryString = SQLGenerator.createSQL(entityClass, filterOn, sortOn);
         parameterList = SQLGenerator.createParameterListForWhereClause(filterOn);

         entityManager = getEntityManager();
         JDBCQuery query = entityManager.createJDBCQuery(entityClass, queryString);
         if (parameterList != null && !parameterList.isEmpty()) {
            query.setParameterList(parameterList);
         }

         if (start < 0 || length < 0) {
            throw new Exception("Start and length parameters must both be 0 or greater. Start = " + start + " and length = " + length + ".");
         }
         else if (start > 0 || length > 0) {
            query.setFirstResult(start);
            query.setMaxResults(length);
         }

         List<?> resultList = query.getResultList();
         if (query.hasErrors()) {
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  queryString = queryString.replaceFirst("\\?", value.toString());
               }
            }

            throw new JDBCDaoException(query.getErrors(), queryString);
         }
         return resultList;
      }
      catch (Exception ex) {
         if (!(ex instanceof JDBCDaoException)) {
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  queryString = queryString.replaceFirst("\\?", value.toString());
               }
            }
            Throwable[] errorExceptions = new Throwable[1];
            errorExceptions[0] = ex;
            ex = new JDBCDaoException(errorExceptions, queryString);
         }
         throw ex;
      }
   }

   protected long countAll(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      JdbcEntityManager entityManager = null;
      List<Object> parameterList = null;
      String queryString = "";

      try {
         queryString = SQLGenerator.createCountSQL(entityClass, filterOn);
         parameterList = SQLGenerator.createParameterListForWhereClause(filterOn);

         entityManager = getEntityManager();
         JDBCQuery query = entityManager.createJDBCQuery(entityClass, queryString);
         if (parameterList != null && !parameterList.isEmpty()) {
            query.setParameterList(parameterList);
         }

         Number count = query.getRecordCount();
         if (query.hasErrors()) {
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  queryString = queryString.replaceFirst("\\?", value.toString());
               }
            }

            throw new JDBCDaoException(query.getErrors(), queryString);
         }

         long numRecords = count.longValue();
         return numRecords;
      }
      catch (Exception ex) {
         if (!(ex instanceof JDBCDaoException)) {
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  queryString = queryString.replaceFirst("\\?", value.toString());
               }
            }
            Throwable[] errorExceptions = new Throwable[1];
            errorExceptions[0] = ex;
            ex = new JDBCDaoException(errorExceptions, queryString);
         }
         throw ex;
      }
   }

   public static List<?> findAll(JdbcEntityManager entityManager, Class<?> entityClass, int start, int length, String queryString, List<Object> parameterList)
         throws Exception {
      JDBCQuery query = entityManager.createJDBCQuery(entityClass, queryString);
      if (parameterList != null && !parameterList.isEmpty()) {
         query.setParameterList(parameterList);
      }

      if (start < 0 || length < 0) {
         throw (new Exception("Start and length parameters must both be 0 or greater. Start = " + start + " and length = " + length + "."));
      }
      else if (start == 0 && length == 0) {
         // skip it and return everything....
      }
      else {
         query.setFirstResult(start);
         query.setMaxResults(length);
      }

      List<?> resultList = query.getResultList();
      if (query.hasErrors()) {
         if (parameterList != null && !parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size(); i++) {
               Object value = parameterList.get(i);
               queryString = queryString.replaceFirst("\\?", value.toString());
            }
         }

         throw new JDBCDaoException(query.getErrors(), queryString);
      }

      return resultList;
   }

   public static long countAll(JdbcEntityManager entityManager, Class<?> entityClass, String queryString, List<Object> parameterList) throws Exception {
      if (queryString != null && !queryString.isEmpty()) {
         JDBCQuery query = entityManager.createJDBCQuery(entityClass, queryString);
         if (parameterList != null && !parameterList.isEmpty()) {
            query.setParameterList(parameterList);
         }

         Number count = query.getRecordCount();
         long numRecords = count.longValue();
         return numRecords;
      }
      else {
         throw new Exception("No query string provided.");
      }
   }

   private E verifyDataInsert(E data) throws Exception {
      JdbcEntityManager entityManager = getEntityManager();

      FilterInfo filterOn = getFilterInfoFor(data);

      String queryString = SQLGenerator.createVerifyDataInsertSQL(data.getClass(), filterOn);

      List<Object> parameterList = SQLGenerator.createParameterListForWhereClause(filterOn);

      List<E> results = (List<E>) findAll(entityManager, data.getClass(), 0, 0, queryString, parameterList);
      if (results.isEmpty()) {
         FieldInfo idFieldInfo = entityInfo.getIdFieldInfo();
         Object dataId = idFieldInfo.getEntityValueForField(data);

         throw new Exception("Unable to find record for newly added entity " + dataId + ".");
      }
      else if (results.size() > 1) {
         throw new Exception("More than one record was found for newly added entity; query: " + queryString);
      }
      else {
         return results.get(0);
      }
   }

   private <T> FilterInfo getFilterInfoFor(T entity) throws Exception {
      List<FilterInfo> filterList = new ArrayList<FilterInfo>();

      FieldInfo idFieldInfo = entityInfo.getIdFieldInfo();

      List<String> propKeys = entityInfo.getColumPropKeys();
      for (int i = 0; i < propKeys.size(); i++) {
         String propKey = propKeys.get(i);
         FieldInfo fieldInfo = entityInfo.getFieldInfoByColumnProperty(propKey);

         if (!fieldInfo.equals(idFieldInfo)) {
            Object value = null;
            if (!fieldInfo.hasEntityInfo()) { // Not a sub-entity...
               value = fieldInfo.getEntityValueForField(entity);
            }
            else {
               Object subEntity = fieldInfo.getEntityValueForField(entity);
               if (subEntity != null) {
                  String refColumnName = fieldInfo.getRefColumnName();
                  if (refColumnName != null && !refColumnName.isEmpty()) {
                     EntityInfo subEntityInfo = fieldInfo.getEntityInfo();
                     String columnProp = subEntityInfo.getTableName() + "." + refColumnName;
                     FieldInfo subEntityFieldInfo = subEntityInfo.getFieldInfoByColumnProperty(columnProp);
                     value = subEntityFieldInfo.getEntityValueForField(subEntity);
                  }
                  else {
                     // get the value of the id field by default...
                     EntityInfo subEntityInfo = fieldInfo.getEntityInfo();
                     FieldInfo subEntityIdFieldInfo = subEntityInfo.getIdFieldInfo();
                     value = subEntityIdFieldInfo.getEntityValueForField(subEntity);
                  }
               }
               // else possible that the value for this field is null.
            }

            if (value != null && !value.getClass().equals(Date.class)) {
               String fieldName = fieldInfo.getFieldName();
               FilterMethod filterMethod = FilterMethod.MATCH_EXACT;
               FilterInfo filterInfo = new FilterInfo(fieldName, value, filterMethod);
               filterList.add(filterInfo);
            }
            // else skip it...
         }
         // else skip the id field...
      }

      FilterInfo[] filters = filterList.toArray(new FilterInfo[filterList.size()]);
      FilterInfo filterOn = new FilterInfo(FilterMethod.AND_FIELD_GROUP, filters);
      return filterOn;
   }
}
