package com.jc.db.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import com.jc.app.data.JPAUtils;
import com.jc.db.dao.DAOTransaction;
import com.jc.log.ExceptionMessageHandler;

public class JPATransaction implements DAOTransaction {

   private final String datasource;
   private final String passwordSecurityKey;

   private EntityManager em;
   private EntityTransaction tx;

   public JPATransaction(String datasource, String passwordSecurityKey) {
      this.datasource = datasource;
      this.passwordSecurityKey = passwordSecurityKey;
   }

   public JPATransaction(String datasource) {
      this(datasource, null);
   }

   public JPATransaction() {
      this(null, null);
   }

   public static EntityManager createEntityManager() throws Exception {
      return createEntityManager(DAOTransaction.DEFAULT_DATASOURCE, null);
   }

   public static EntityManager createEntityManager(String datasource) throws Exception {
      return createEntityManager(datasource, null);
   }

   public static EntityManager createEntityManager(String datasource, String passwordSecurityKey) throws Exception {

      if (datasource == null || datasource.isEmpty()) {
         throw new IllegalStateException("Supplied datasource was 'null' or and empty string.");
      }

      EntityManager manager = null;
      try {
         EntityManagerFactory factory = JPAUtils.getEntityFactory(datasource);
         if (factory != null) {
            manager = factory.createEntityManager();
            if (manager != null) {
               EntityTransaction tx = manager.getTransaction();
               tx.begin();
               manager.flush();
               tx.commit();
               tx = null;
            }
            else {
               throw new EntityManagerCreationException(datasource);
            }
         }
         else {
            throw new JPATransactionException(datasource);
         }
      }
      catch (Exception ex) {
         if (ex instanceof EntityManagerCreationException) {
            throw (EntityManagerCreationException) ex;
         }
         else if (ex instanceof JPATransactionException) {
            throw (JPATransactionException) ex;
         }
         else if (ex instanceof RollbackException) {
            // try again...
            JPAUtils.removeDBFactoryInfo(datasource); // for the JPAUtils to create a new factory...
            manager = createEntityManager(datasource);
         }
         else {
            throw ex;
         }
      }
      return manager;
   }

   @Override
   public EntityManager getEntityManager() {
      return this.em;
   }

   @Override
   public boolean hasEntityManager() {
      return this.em != null;
   }

   @Override
   public void startTransaction() throws Exception {
      if (em == null) {
         if ((datasource != null && !datasource.isEmpty()) && (passwordSecurityKey != null && !passwordSecurityKey.isEmpty())) {
            em = createEntityManager(datasource, passwordSecurityKey);
         }
         else if (datasource != null && !datasource.isEmpty()) {
            em = createEntityManager(datasource);
         }
         else {
            em = createEntityManager();
         }
      }

      tx = em.getTransaction();
      tx.begin();
   }

   @Override
   public void endTransaction() throws Exception {
      tx.commit();
      tx = null;

      em.clear();
      em.close();
      em = null;
   }

   @Override
   public void rollBackTransaction() throws Exception {
      tx.rollback();
      tx = null;

      em.clear();
      em.close();
      em = null;
   }

   @Override
   public boolean isExternalTransactionMonitoring() throws Exception {
      return (tx != null) && (em != null);
   }

   @Override
   public Exception handleGlobalSessionException(Class<?> refClass, String message, Exception ex) {

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
      if (em != null && em.isOpen()) {
         try {
            em.close();
         }
         catch (Exception exs) {
            emCloseErrMsg = "Unable to close the session during exception processing: " + exs.getMessage();
         }
      }

      // Create the error message added to the LoggableException that will be thrown as a result of handling this exception....

      String origExMsg = ex.getMessage();

      ExceptionMessageHandler exHandler = new ExceptionMessageHandler(ex);
      String exceptionLocation = exHandler.getExceptionLocation();
      String exceptionName = exHandler.getExceptionName();
      //boolean suspectedDBIsue = exHandler.isSuspectedDatabaseIssue();

      String rollbackOkMsg = "All data was rolled back and no data was saved to the database.";

      StringBuilder sb = new StringBuilder(message + " " + exceptionName + " thrown at " + exceptionLocation + ". " + origExMsg);
      if (rollBackErrMsg != null) {
         sb.append(" " + rollBackErrMsg);
      }

      if (emCloseErrMsg != null) {
         sb.append(" " + emCloseErrMsg);
      }

      if (rollBackErrMsg == null && emCloseErrMsg == null) {
         sb.append(" " + rollbackOkMsg);
      }

      return new Exception(sb.toString());
   }

   @Override
   public String getDataSource() {
      return datasource;
   }

   @Override
   public String getPasswordSecurityKey() {
      return passwordSecurityKey;
   }
}
