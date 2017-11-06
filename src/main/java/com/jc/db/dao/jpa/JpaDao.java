package com.jc.db.dao.jpa;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.jc.db.DatabaseErrorCode;
import com.jc.db.dao.Dao;
import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;
import com.jc.exception.LoggableException;
import com.jc.exception.SystemInfoException;

public abstract class JpaDao<K, E> implements Dao<K, E> {

   private final Class<E> entityClass;
   private EntityManager MyEntityManager;

   private String datasource;
   private String passwordSecurityKey;

   @SuppressWarnings("unchecked")
   private JpaDao() {
      datasource = "";
      ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
      entityClass = (Class<E>) genericSuperclass.getActualTypeArguments()[1];
   }

   public JpaDao(String datasource) throws Exception {
      this();
      this.datasource = datasource;
   }

   public JpaDao(String datasource, String passwordSecurityKey) throws Exception {
      this();
      this.datasource = datasource;
      this.passwordSecurityKey = passwordSecurityKey;
   }

   public JpaDao(EntityManager entityManager) throws Exception {
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

   protected EntityManager getEntityManager() throws Exception {
      if (MyEntityManager == null || !MyEntityManager.isOpen()) {
         if ((datasource != null && !datasource.isEmpty()) && (passwordSecurityKey != null && !passwordSecurityKey.isEmpty())) {
            MyEntityManager = JPATransaction.createEntityManager(datasource, passwordSecurityKey);
         }
         else if (datasource != null && !datasource.isEmpty()) {
            MyEntityManager = JPATransaction.createEntityManager(datasource);
         }
         else {
            MyEntityManager = JPATransaction.createEntityManager();
         }
      }
      return MyEntityManager;
   }

   @Override
   public E addData(E data) throws Exception {
      return persistData(data, SAVE);
   }

   @Override
   public List<E> addData(List<E> dataList) throws Exception {
      ArrayList<E> updatedList = new ArrayList<>();
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
      ArrayList<E> updatedList = new ArrayList<>();
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
      EntityManager entityManager = getEntityManager();
      EntityTransaction tx = null;

      try {
         if (!isExternalTransactionMonitoring()) {
            tx = entityManager.getTransaction();
            tx.begin();
         }

         if (presistType == SAVE) { // 
            entityManager.persist(data); // do an Insert
         }
         else {
            entityManager.merge(data); // do an Update
         }

         if (!isExternalTransactionMonitoring()) {
            tx.commit();
            entityManager.clear();
            entityManager.close();
         }

         return data;
      }
      catch (Exception ex) {
         if (isExternalTransactionMonitoring()) {
            // Only package the exception and sent it back, roll back should be handled there
            // in a "global" scenario.
            throw ex;
         }
         else {
            // local scenario... handle rollback.
            DatabaseErrorCode errorCode = DatabaseErrorCode.UPDATE_DATA;
            if (presistType == SAVE) {
               errorCode = DatabaseErrorCode.ADD_DATA;
            }
            throw createGlobalSessionException(entityClass, "Error while persisting data.", ex, tx, entityManager, errorCode);
         }
      }
   }

   @Override
   public void removeData(E data) throws Exception {
      EntityManager entityManager = getEntityManager();
      EntityTransaction tx = null;

      try {
         if (!isExternalTransactionMonitoring()) {
            tx = entityManager.getTransaction();
            tx.begin();
         }

         if (!entityManager.contains(data)) {
            data = entityManager.merge(data);
         }
         entityManager.remove(data); // do a Delete

         if (!isExternalTransactionMonitoring()) {
            tx.commit();
            entityManager.clear();
            entityManager.close();
         }
      }
      catch (Exception ex) {
         if (isExternalTransactionMonitoring()) {
            // Only package the exception and sent it back, roll back should be handled there
            // in a "global" scenario.
            throw ex;
         }
         else {
            // local scenario... handle rollback.
            throw createGlobalSessionException(entityClass, "Error while removing data.", ex, tx, entityManager, DatabaseErrorCode.REMOVE_DATA);
         }
      }
   }

   /*
    * This method assumes that the entity returned has an id field of 'id'. 
    */
   @Override
   public E findById(K id) throws Exception {
      int start = 0;
      int length = 0;
      Map<String, String> sortOn = new HashMap<>();
      FilterInfo filterOn = new FilterInfo("id", id, FilterMethod.MATCH_EXACT);
      List<E> entityList = findAll(start, length, sortOn, filterOn);
      if (entityList.isEmpty()) {
         return null;
      }
      else if (entityList.size() == 1) {
         return entityList.get(0);
      }
      else {
         throw new Exception("More than one entity object was found for id '" + id + "'.");
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
      Map<String, String> sortOn = new HashMap<>();

      if (fieldvalue instanceof List<?>) {
         if ((!methodType.equals(FilterMethod.IN_LIST)) && (!methodType.equals(FilterMethod.NOT_IN_LIST))) {
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
      EntityManager entityManager = null;
      List<Object> parameterList = null;
      String queryString = "";

      try {
         queryString = JPQLGenerator.createJPQL(entityClass, filterOn, sortOn);
         if (queryString != null && !queryString.isEmpty()) {
            entityManager = getEntityManager();
            Query query = entityManager.createQuery(queryString);

            if (start < 0 || length < 0) {
               throw new Exception("Start and length parameters must both be 0 or greater. Start = " + start + " and length = " + length + ".");
            }
            else if (start > 0 || length > 0) {
               query.setFirstResult(start);
               query.setMaxResults(length);
            }
            // else skip it and return everything....

            parameterList = JPQLGenerator.createParameterList(entityClass, filterOn);
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  query.setParameter(i + 1, value);
               }
            }

            return query.getResultList();
         }
         else {
            throw (new Exception("No query string could be constructed for " + entityClass + "."));
         }
      }
      catch (Exception ex) {
         String message = "";
         if (parameterList != null && !parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size(); i++) {
               Object value = parameterList.get(i);
               queryString = queryString.replaceFirst("\\?", value.toString());
            }
            message = "Query: " + queryString;
         }
         else if (queryString != null && !queryString.isEmpty()) {
            message = "Query: " + queryString;
         }

         if (!message.isEmpty()) {
            ex = LoggableException.createLoggableException(entityClass, Level.SEVERE, message, ex);
         }

         throw ex;
      }
      finally {
         if (!isExternalTransactionMonitoring() && entityManager != null) {
            entityManager.close();
         }
      }
   }

   protected long countAll(Class<?> entityClass, FilterInfo filterOn) throws Exception {
      EntityManager entityManager = null;
      List<Object> parameterList = null;
      String queryString = "";

      try {
         queryString = JPQLGenerator.createCountJPQL(entityClass, filterOn);
         if (queryString != null && !queryString.isEmpty()) {
            entityManager = getEntityManager();
            Query query = entityManager.createQuery(queryString);

            parameterList = JPQLGenerator.createParameterList(entityClass, filterOn);
            if (parameterList != null && !parameterList.isEmpty()) {
               for (int i = 0; i < parameterList.size(); i++) {
                  Object value = parameterList.get(i);
                  query.setParameter(i + 1, value);
               }
            }

            Number count = (Number) query.getSingleResult();
            long numRecords = count.longValue();

            return numRecords;
         }
         else {
            throw (new Exception("No query string could be constructed for " + entityClass + "."));
         }
      }
      catch (Exception ex) {
         String message = "";
         if (parameterList != null && !parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size(); i++) {
               Object value = parameterList.get(i);
               queryString = queryString.replaceFirst("\\?", value.toString());
            }
            message = "Query: " + queryString;
         }
         else if (queryString != null && !queryString.isEmpty()) {
            message = "Query: " + queryString;
         }

         if (!message.isEmpty()) {
            ex = LoggableException.createLoggableException(entityClass, Level.SEVERE, message, ex);
         }

         throw ex;
      }
      finally {
         if (!isExternalTransactionMonitoring() && entityManager != null) {
            entityManager.close();
         }
      }
   }

   /*
    * "External Transaction Monitoring Mode" means that the JPADao will not manage the transaction 
    * during a write to the database. This mode can only happen if the datasource is an empty string
    * and the entity manager object is not null.
    */
   protected boolean isExternalTransactionMonitoring() {
      boolean globalSessionMode = datasource.isEmpty() && (MyEntityManager != null);
      return globalSessionMode;
   }

   protected SystemInfoException createGlobalSessionException(Class<?> refClass, String message, Exception ex, EntityTransaction tx,
         EntityManager entityManager, DatabaseErrorCode errorCode) {

      // try to close the transaction....
      String rollBackErrMsg = null;
      if (tx != null && tx.isActive()) {
         try {
            tx.rollback();
         }
         catch (Exception txex) {
            rollBackErrMsg = "Rollback during exception processing was unsuccessful: " + txex.getMessage();
         }
      }

      // try to close the entityManager....
      String emCloseErrMsg = null;
      if (entityManager != null && entityManager.isOpen()) {
         try {
            entityManager.close();
         }
         catch (Exception exs) {
            emCloseErrMsg = "Unable to close the session during exception processing: " + exs.getMessage();
         }
      }

      LoggableException obex = null;
      if (ex instanceof LoggableException) {
         obex = (LoggableException) ex;
      }
      else {
         obex = LoggableException.createLoggableException(refClass, Level.SEVERE, ex);
      }

      // Create the error message added to the LoggableException that will be thrown as a result of handling this exception....

      String origExMsg = message + " " + obex.getMessage();

      String rollbackOkMsg = "All data was rolled back and no data was saved to the database.";

      StringBuilder sb = new StringBuilder(origExMsg);
      if (rollBackErrMsg != null) {
         sb.append(" " + rollBackErrMsg);
      }

      if (emCloseErrMsg != null) {
         sb.append(" " + emCloseErrMsg);
      }

      if (rollBackErrMsg == null && emCloseErrMsg == null) {
         sb.append(" " + rollbackOkMsg);
      }

      return new SystemInfoException(Level.SEVERE, errorCode, sb.toString());
   }

   //
   //
   //
   //
   //
   //   protected List<E> findAll(int start, int length, String queryString, List<Object> parameterList, boolean nativeCode) throws Exception {
   //      EntityManager entityManager = getEntityManager();
   //      try {
   //         if (queryString != null && !queryString.isEmpty()) {
   //
   //            //      System.out.println("QRY[" + queryString + "]");
   //
   //            javax.persistence.Query query = null;
   //            if (!nativeCode) {
   //               query = entityManager.createQuery(queryString);
   //            }
   //            else {
   //               query = entityManager.createNativeQuery(queryString, entityClass);
   //            }
   //
   //            if (start < 0 || length < 0) {
   //               //               throw (new SystemInfoException("Start and length parameters must both be 0 or greater. Start = " + start + " and length = " + length + "."));
   //               throw (new Exception("Start and length parameters must both be 0 or greater. Start = " + start + " and length = " + length + "."));
   //            }
   //            else if (start == 0 && length == 0) {
   //               // skip it and return everything....
   //            }
   //            else {
   //               query.setFirstResult(start);
   //               query.setMaxResults(length);
   //            }
   //
   //            if (parameterList != null && !parameterList.isEmpty()) {
   //               for (int i = 0; i < parameterList.size(); i++) {
   //                  Object value = parameterList.get(i);
   //                  query.setParameter(i + 1, value);
   //               }
   //            }
   //
   //            return query.getResultList();
   //         }
   //         else {
   //            throw new IllegalArgumentException("Null or empty query string supplied.");
   //         }
   //      }
   //      catch (Exception ex) {
   //         String message = "";
   //         if (parameterList != null && !parameterList.isEmpty()) {
   //            for (int i = 0; i < parameterList.size(); i++) {
   //               Object value = parameterList.get(i);
   //               queryString = queryString.replaceFirst("\\?", value.toString());
   //            }
   //            message = "Query: " + queryString;
   //         }
   //
   //         if (!message.isEmpty()) {
   //            ex = LoggableException.createLoggableException(entityClass, Level.SEVERE, message, ex);
   //         }
   //
   //         throw ex;
   //      }
   //      finally {
   //         if (!isExternalTransactionMonitoring() && entityManager != null) {
   //            entityManager.close();
   //         }
   //      }
   //   }
   //
   //
   //   private long getTotalNumberOfRecords(String queryString, List<Object> parameterList, boolean nativeCode) throws Exception {
   //      EntityManager entityManager = getEntityManager();
   //      try {
   //         if (queryString != null && !queryString.isEmpty()) {
   //            javax.persistence.Query query = null;
   //            if (!nativeCode) {
   //               query = entityManager.createQuery(queryString);
   //            }
   //            else {
   //               // Native Query Support...
   //               query = entityManager.createNativeQuery(queryString);
   //            }
   //
   //            if (parameterList != null && !parameterList.isEmpty()) {
   //               for (int i = 0; i < parameterList.size(); i++) {
   //                  Object value = parameterList.get(i);
   //                  query.setParameter(i + 1, value);
   //               }
   //            }
   //
   //            Number count = (Number) query.getSingleResult();
   //            long numRecords = count.longValue();
   //
   //            return numRecords;
   //         }
   //         else {
   //            throw new IllegalArgumentException("Null or empty query string supplied.");
   //         }
   //      }
   //      catch (Exception ex) {
   //         throw ex;
   //      }
   //      finally {
   //         if (!isExternalTransactionMonitoring() && entityManager != null) {
   //            entityManager.close();
   //         }
   //      }
   //   }

}
