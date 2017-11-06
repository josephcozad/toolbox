package com.jc.db.dao.jdbc;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import com.jc.db.DBResult;
import com.jc.db.DBResult.ColumnMetaData;
import com.jc.db.ResultSetProcessor;
import com.jc.util.ObjectFactory;

public class JDBCQuery extends ResultSetProcessor implements Query {

   private final JdbcEntityManager entityManager;
   private List<Object> entityObjs;
   private int startResultNum;
   private int maxResults;
   private boolean recordCountQuery;

   private List<Object> parameterList;
   private String queryStr;
   private Class<?> entityClass;

   JDBCQuery(JdbcEntityManager entityManager) {
      this.entityManager = entityManager;
   }

   // ----------------- Implements ResultSetProcessor -----------------

   @Override
   protected List<Object> getParameterList() {
      return parameterList;
   }

   @Override
   public String getQuery() {
      return queryStr;
   }

   @Override
   public void processResultSet(ResultSet result) throws SQLException {
      this.entityObjs = new ArrayList<Object>();

      if (!recordCountQuery) {
         if (startResultNum > 0) {
            // fast forward to startResultNum row...
            for (int i = 0; i < startResultNum; i++) {
               result.next();
            }
         }

         Map<String, ColumnMetaData> metadata = DBResult.getColumnInfo(result, true);
         EntityInfo entityInfo = new EntityInfo(entityClass);

         int numRecsProcessed = 0;
         while (result.next()) {
            try {
               Object entity = createEntityObject(result, metadata, entityInfo);
               entityObjs.add(entity);

               numRecsProcessed++;
               if (maxResults > 0 && numRecsProcessed >= maxResults) {
                  break;
               }
            }
            catch (Exception ex) {
               if (!(ex instanceof JDBCQueryException)) {
                  throw new JDBCQueryException(ex);
               }
               else {
                  throw (JDBCQueryException) ex;
               }
            }
         }
      }
      else { // is recordCountQuery
         long count = 0;
         while (result.next()) {
            count = result.getLong(1);
         }

         entityObjs.add(new Long(count));
      }
   }

   // ------------------- Implements Query Interface -------------------

   @Override
   @SuppressWarnings("rawtypes")
   public List getResultList() {
      boolean previouslyStarted = entityManager.transactionStarted();

      try {
         if (!previouslyStarted) {
            entityManager.startTransaction();
         }

         entityManager.executeJDBCQuery(this);

         if (!previouslyStarted) {
            entityManager.endTransaction();
         }
      }
      catch (Exception ex) {
         throw new JDBCQueryException(ex);
      }
      return entityObjs;
   }

   @Override
   public Object getSingleResult() {
      Object value = null;
      List resultList = getResultList();
      if (resultList.size() > 0) {
         value = resultList.get(0);
      }
      return value;
   }

   @Override
   public int executeUpdate() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setMaxResults(int maxResults) {
      this.maxResults = maxResults;
      return this;
   }

   @Override
   public int getMaxResults() {
      return maxResults;
   }

   @Override
   public Query setFirstResult(int startResultNum) {
      this.startResultNum = startResultNum;
      return this;
   }

   @Override
   public int getFirstResult() {
      return startResultNum;
   }

   @Override
   public Query setHint(String hintName, Object value) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Map<String, Object> getHints() {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> Query setParameter(Parameter<T> param, T value) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(String name, Object value) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(String name, Calendar value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(String name, Date value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(int position, Object value) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(int position, Calendar value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setParameter(int position, Date value, TemporalType temporalType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Set<Parameter<?>> getParameters() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Parameter<?> getParameter(String name) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> Parameter<T> getParameter(String name, Class<T> type) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Parameter<?> getParameter(int position) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> Parameter<T> getParameter(int position, Class<T> type) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isBound(Parameter<?> param) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> T getParameterValue(Parameter<T> param) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object getParameterValue(String name) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object getParameterValue(int position) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setFlushMode(FlushModeType flushMode) {
      throw new UnsupportedOperationException();
   }

   @Override
   public FlushModeType getFlushMode() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Query setLockMode(LockModeType lockMode) {
      throw new UnsupportedOperationException();
   }

   @Override
   public LockModeType getLockMode() {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> T unwrap(Class<T> cls) {
      throw new UnsupportedOperationException();
   }

   // ------------------------------------------------------------------------

   void setParameterList(List<Object> parameterList) {
      this.parameterList = parameterList;
   }

   Long getRecordCount() throws Exception {
      recordCountQuery = true;
      return (Long) getSingleResult();
   }

   void setQueryString(Class<?> entityClass, String queryStr) {
      this.queryStr = queryStr;
      this.entityClass = entityClass;
   }

   // ---------------------------- Private Methods ----------------------------

   private Object createEntityObject(ResultSet result, Map<String, ColumnMetaData> metadata, EntityInfo entInfo) throws Exception {
      Object entity = null;

      List<String> colPropKeys = entInfo.getColumPropKeys();
      for (String columnProp : colPropKeys) {

         FieldInfo info = entInfo.getFieldInfoByColumnProperty(columnProp);

         if (!info.hasEntityInfo()) {
            Object value = null;
            ColumnMetaData column = metadata.get(columnProp.toLowerCase());
            if (column != null) {
               value = DBResult.getValueFromResultSet(column, result);
               if (value != null) {
                  if (entity == null) {
                     entity = ObjectFactory.getInstance(entInfo.getEntityClassName());
                  }
                  Method setMethod = info.getSetterMethod();

                  Class<? extends Object> valueClass = value.getClass();
                  Class<?>[] methodParamTypes = setMethod.getParameterTypes();

                  boolean skipReTest = false; // methodParamType != valueClass, but no conversion needed; so don't retest if set to true.
                  if (methodParamTypes[0].equals(Integer.TYPE) && valueClass.equals(Integer.class)) {
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(Long.TYPE) && valueClass.equals(Long.class)) {
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(Boolean.TYPE) && valueClass.equals(Boolean.class)) {
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(Double.TYPE) && valueClass.equals(Double.class)) {
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(Float.TYPE) && valueClass.equals(Float.class)) {
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(Long.class) && valueClass.equals(BigInteger.class)) {
                     // convert value from BigInteger to Long...
                     long longValue = ((BigInteger) value).longValue();
                     value = new Long(longValue);
                     valueClass = value.getClass();
                  }
                  else if (methodParamTypes[0].equals(Long.class) && valueClass.equals(Integer.class)) {
                     // convert value from Integer to Long...
                     long longValue = ((Integer) value).longValue();
                     value = new Long(longValue);
                     valueClass = value.getClass();
                  }
                  else if (methodParamTypes[0].equals(Boolean.class) && valueClass.equals(Integer.class)) {
                     // convert value from Integer to Boolean...
                     boolean boolValue = true;
                     int intValue = ((Integer) value).intValue();
                     if (intValue == 0) {
                        boolValue = false;
                     }
                     value = new Boolean(boolValue);
                     valueClass = value.getClass();
                  }
                  else if (methodParamTypes[0].equals(Boolean.TYPE) && valueClass.equals(Integer.class)) {
                     // convert value from Integer to Boolean...
                     boolean boolValue = true;
                     int intValue = ((Integer) value).intValue();
                     if (intValue == 0) {
                        boolValue = false;
                     }
                     value = new Boolean(boolValue);
                     skipReTest = true;
                  }
                  else if (methodParamTypes[0].equals(BigInteger.class) && valueClass.equals(Long.class)) {
                     // convert value from Long to BigInteger...
                     value = new BigInteger(((Long) value).toString());
                     valueClass = value.getClass();
                  }

                  // compare the value type with the object's field type...
                  if (!skipReTest && !methodParamTypes[0].equals(valueClass)) {
                     throw new JDBCQueryException(valueClass.getName(), setMethod.getName(), methodParamTypes[0].getName(), entInfo.getEntityClassName());
                  }

                  setMethod.invoke(entity, value);
               }
               // else no data exists for this field...
            }
            // else no data was queried for this field...
         }
         else {
            Object subEntity = createEntityObject(result, metadata, info.getEntityInfo());
            if (subEntity != null) {
               if (entity == null) {
                  entity = ObjectFactory.getInstance(entInfo.getEntityClassName());
               }
               Method setMethod = info.getSetterMethod();

               // compare the value type with the object's field type...
               Class<? extends Object> valueClass = subEntity.getClass();
               Class<?>[] paramTypes = setMethod.getParameterTypes();
               if (!paramTypes[0].equals(valueClass)) {
                  throw new JDBCQueryException(valueClass.getName(), setMethod.getName(), paramTypes[0].getName(), entInfo.getEntityClassName());
               }

               setMethod.invoke(entity, subEntity);
            }
         }
      }

      return entity;
   }
}
