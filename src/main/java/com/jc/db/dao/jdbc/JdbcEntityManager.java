package com.jc.db.dao.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import com.jc.db.DBConnection;
import com.jc.db.DBResult;
import com.jc.db.command.JDBCConnection;
import com.jc.db.dao.EntityAssociation;

public class JdbcEntityManager implements EntityManager {

   private String datasource;
   private String passwordSecurityKey;

   private JDBCConnection jdbcConnection;
   private DBConnection dbConn;

   public JdbcEntityManager(String datasource, String username, String password, String driver, String url) {
      jdbcConnection = new JDBCConnection(username, password, driver, url);
      jdbcConnection.setDatasource(datasource);
   }

   public JdbcEntityManager(String datasource, String username, String password, String driver) {
      jdbcConnection = new JDBCConnection(username, password, driver);
      jdbcConnection.setDatasource(datasource);
   }

   public JdbcEntityManager(String datasource, String passwordSecurityKey) {
      // Assumes connection info is contained in the ConfigInfo...
      this.datasource = datasource;
      this.passwordSecurityKey = passwordSecurityKey;
   }

   public JdbcEntityManager(String datasource) {
      // Assumes connection info is contained in the ConfigInfo...
      this(datasource, "");
   }

   public JdbcEntityManager() {
      // Assumes connection info is contained in the ConfigInfo...
      this("", "");
   }

   public boolean transactionStarted() {
      return dbConn != null;
   }

   public void startTransaction() throws Exception {
      if (dbConn == null) {
         dbConn = getDBConnection();
      }
      dbConn.startTransaction();

      if (!dbConn.transactionStarted()) {
         throw new Exception("Unable to start JDBC connection. See database log for more details.");
      }
   }

   public void endTransaction() {
      dbConn.endTransaction();
      dbConn = null;
   }

   public void rollBackTransaction() {
      dbConn.rollBackTransaction();
      dbConn = null;
   }

   JDBCQuery createJDBCQuery(Class<?> entityClass, String queryStr) throws Exception {
      JDBCQuery query = new JDBCQuery(this);
      query.setQueryString(entityClass, queryStr);
      return query;
   }

   @Override
   public Query createQuery(String queryStr) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void persist(Object entity) {
      boolean previouslyStarted = false;

      try {
         // create insert query based on entity info.
         Class<?> entityClass = entity.getClass();
         EntityInfo entityInfo = new EntityInfo(entityClass);

         String query = createInsertQuery(entity, entityInfo);
         List<Object> parameterList = createParameterList(entity, entityInfo);

         previouslyStarted = transactionStarted();

         if (!previouslyStarted) {
            startTransaction();
         }

         DBResult result = executeTransaction(query, parameterList);
         if (result.hasErrors()) {
            throw new JDBCDaoInsertException(result.getErrors());
         }

         if (!previouslyStarted) {
            endTransaction();
         }
      }
      catch (Exception ex) {
         if (!previouslyStarted) {
            rollBackTransaction();
         }

         Throwable[] errorExceptions = new Throwable[1];
         errorExceptions[0] = ex;
         throw new JDBCDaoInsertException(errorExceptions);
      }
   }

   @Override
   public <T> T merge(T entity) {
      boolean previouslyStarted = false;

      try {
         Class<?> entityClass = entity.getClass();
         EntityInfo entityInfo = new EntityInfo(entityClass);

         String query = createUpdateQuery(entity, entityInfo);
         List<Object> parameterList = createParameterList(entity, entityInfo);

         previouslyStarted = transactionStarted();

         // create insert query based on entity info.
         if (!previouslyStarted) {
            startTransaction();
         }

         DBResult result = executeTransaction(query, parameterList);
         if (result.hasErrors()) {
            throw new JDBCDaoUpdateException(result.getErrors());
         }

         if (!previouslyStarted) {
            endTransaction();
         }
      }
      catch (Exception ex) {
         if (!previouslyStarted) {
            rollBackTransaction();
         }

         Throwable[] errorExceptions = new Throwable[1];
         errorExceptions[0] = ex;
         throw new JDBCDaoUpdateException(errorExceptions);
      }
      return entity;
   }

   @Override
   public void remove(Object entity) {
      boolean previouslyStarted = false;

      try {
         Class<?> entityClass = entity.getClass();
         EntityInfo entityInfo = new EntityInfo(entityClass);

         String query = createDeleteQuery(entity, entityInfo);

         previouslyStarted = transactionStarted();

         // create insert query based on entity info.
         if (!previouslyStarted) {
            startTransaction();
         }

         DBResult result = executeTransaction(query);
         if (result.hasErrors()) {
            throw new JDBCDaoDeleteException(result.getErrors());
         }

         if (!previouslyStarted) {
            endTransaction();
         }
      }
      catch (Exception ex) {
         if (!previouslyStarted) {
            rollBackTransaction();
         }

         Throwable[] errorExceptions = new Throwable[1];
         errorExceptions[0] = ex;
         throw new JDBCDaoDeleteException(errorExceptions);
      }
   }

   @Override
   public <T> T find(Class<T> entityClass, Object primaryKey) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> T getReference(Class<T> entityClass, Object primaryKey) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void flush() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void setFlushMode(FlushModeType flushMode) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public FlushModeType getFlushMode() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void lock(Object entity, LockModeType lockMode) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void refresh(Object entity) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void refresh(Object entity, Map<String, Object> properties) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void refresh(Object entity, LockModeType lockMode) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void clear() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void detach(Object entity) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public boolean contains(Object entity) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public LockModeType getLockMode(Object entity) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void setProperty(String propertyName, Object value) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Map<String, Object> getProperties() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
      throw (new UnsupportedOperationException());
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Query createQuery(CriteriaUpdate updateQuery) {
      throw (new UnsupportedOperationException());
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Query createQuery(CriteriaDelete deleteQuery) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Query createNamedQuery(String name) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Query createNativeQuery(String sqlString) {
      throw (new UnsupportedOperationException());
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Query createNativeQuery(String sqlString, Class resultClass) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Query createNativeQuery(String sqlString, String resultSetMapping) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
      throw (new UnsupportedOperationException());
   }

   @Override
   @SuppressWarnings("rawtypes")
   public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void joinTransaction() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public boolean isJoinedToTransaction() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> T unwrap(Class<T> cls) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Object getDelegate() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public void close() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public boolean isOpen() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public EntityTransaction getTransaction() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public EntityManagerFactory getEntityManagerFactory() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public CriteriaBuilder getCriteriaBuilder() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public Metamodel getMetamodel() {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public EntityGraph<?> createEntityGraph(String graphName) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public EntityGraph<?> getEntityGraph(String graphName) {
      throw (new UnsupportedOperationException());
   }

   @Override
   public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
      throw (new UnsupportedOperationException());
   }

   // -----------------------------------------------------------

   void executeJDBCQuery(JDBCQuery query) throws Exception {
      dbConn.executeSQLQuery(query);
   }

   // -------------------- PRIVATE METHODS ----------------------

   private DBConnection getDBConnection() {

      DBConnection dbConn = null;
      try {
         if (jdbcConnection != null) {
            dbConn = DBConnection.getInstance(jdbcConnection);
         }
         else {
            if (datasource.isEmpty()) {
               dbConn = DBConnection.getInstance();
            }
            else {
               if (passwordSecurityKey.isEmpty()) {
                  dbConn = DBConnection.getInstance(datasource);
               }
               else {
                  dbConn = DBConnection.getInstance(datasource, passwordSecurityKey);
               }
            }
         }

         if (dbConn == null) {
            throw new IllegalStateException("No database connection could be found for datasource '" + datasource + "'.");
         }
      }
      catch (Exception ex) {
         throw new IllegalStateException(ex);
      }
      return dbConn;
   }

   private DBResult executeTransaction(String query, List<Object> parameterList) throws Exception {
      return dbConn.executeSQLUpdate(query, parameterList);
   }

   private DBResult executeTransaction(String query) throws Exception {
      return dbConn.executeSQLUpdate(query);
   }

   private String createInsertQuery(Object entity, EntityInfo entityInfo) throws Exception {

      StringBuilder columnList = new StringBuilder();
      StringBuilder valuesList = new StringBuilder();
      String table = (entityInfo.getSchemaName() + "." + entityInfo.getTableName()).toLowerCase();

      List<String> colPropKeys = entityInfo.getColumPropKeys();
      for (String columnProp : colPropKeys) {

         FieldInfo info = entityInfo.getFieldInfoByColumnProperty(columnProp);
         Object value = getValueFromEntity(entity, info);
         if (value != null) {
            String column = columnProp.toLowerCase();
            column = column.substring(column.lastIndexOf(".") + 1, column.length());
            columnList.append(column + ",");
            //            columnList.append(columnProp.toLowerCase() + ",");
            valuesList.append("?,");
         }
      }

      columnList.deleteCharAt(columnList.length() - 1); // remove last character...
      valuesList.deleteCharAt(valuesList.length() - 1); // remove last character...

      StringBuilder query = new StringBuilder("INSERT INTO ");
      query.append(table + " ");
      query.append("(" + columnList + ") ");
      query.append("VALUES ");
      query.append("(" + valuesList + ") ");

      return query.toString();
   }

   private String createUpdateQuery(Object entity, EntityInfo entityInfo) throws Exception {

      StringBuilder setList = new StringBuilder();
      String table = (entityInfo.getSchemaName() + "." + entityInfo.getTableName()).toLowerCase();

      FieldInfo idFieldInfo = entityInfo.getIdFieldInfo();
      Object idValueObj = getValueFromEntity(entity, idFieldInfo);
      String idValue = idValueObj.toString(); // Assume id is a number value.
      if (idValueObj instanceof String) {
         idValue = "'" + idValueObj + "',";
      }

      List<String> colPropKeys = entityInfo.getColumPropKeys();
      for (String columnProp : colPropKeys) {
         FieldInfo fieldInfo = entityInfo.getFieldInfoByColumnProperty(columnProp);
         Object value = getValueFromEntity(entity, fieldInfo);
         if (value != null) {
            String column = columnProp.toLowerCase();
            column = column.substring(column.lastIndexOf(".") + 1, column.length());
            //            String column = columnProp.toLowerCase();
            setList.append(column + "=?,");
         }
      }

      setList.deleteCharAt(setList.length() - 1); // remove last character...

      String idfield = idFieldInfo.getColumnProperty();
      StringBuilder query = new StringBuilder("UPDATE ");
      query.append(table + " ");
      query.append("SET " + setList + " ");
      query.append("WHERE " + idfield + "=" + idValue);

      return query.toString();
   }

   private List<Object> createParameterList(Object entity, EntityInfo entityInfo) throws Exception {

      List<Object> parameterList = new ArrayList<Object>();
      List<String> colPropKeys = entityInfo.getColumPropKeys();
      for (String columnProp : colPropKeys) {
         FieldInfo info = entityInfo.getFieldInfoByColumnProperty(columnProp);
         Object value = getValueFromEntity(entity, info);
         if (value != null) {
            parameterList.add(value);
         }
      }
      return parameterList;
   }

   private String createDeleteQuery(Object entity, EntityInfo entityInfo) throws Exception {

      String table = (entityInfo.getSchemaName() + "." + entityInfo.getTableName()).toLowerCase();

      FieldInfo idFieldInfo = entityInfo.getIdFieldInfo();
      Object idValueObj = getValueFromEntity(entity, idFieldInfo);
      String idValue = idValueObj.toString(); // Assume id is a number value.
      if (idValueObj instanceof String) {
         idValue = "'" + idValueObj + "',";
      }

      String idfield = idFieldInfo.getColumnProperty();
      StringBuilder query = new StringBuilder("DELETE FROM ");
      query.append(table + " ");
      query.append("WHERE " + idfield + "=" + idValue);

      return query.toString();
   }

   private Object getValueFromEntity(Object entity, FieldInfo fieldInfo) throws Exception {
      Object value = null;
      if (fieldInfo.hasEntityInfo()) {
         if (fieldInfo.isAssociationOwner()) {
            EntityAssociation association = fieldInfo.getAssociation();
            if (association.equals(EntityAssociation.ONE_TO_MANY)) {
               // Not supported... because entity will have a collection of sub-entities.
            }
            else if (association.equals(EntityAssociation.MANY_TO_MANY)) {
               // Not supported... because requires an xref table to join the two entities.
            }
            else {
               // ManyToOne and OneToOne... the sub-entity's idColProp is the value to return.

               // get the sub-entity...
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
               // possible the value of the sub entity is null.
            }
         }
         // skip, only get values for association owners.
      }
      else {
         value = fieldInfo.getEntityValueForField(entity);
      }
      return value;
   }
}
