package com.jc.db.dao.jdbc;

import com.jc.app.data.domain.AppDatabase;
import com.jc.app.service.ApplicationServiceManager;
import com.jc.db.JDBCDriverInfo;
import com.jc.db.dao.DAOTransaction;
import com.jc.log.ExceptionMessageHandler;

public class JDBCTransaction implements DAOTransaction {

   private final String datasource;
   private final String passwordSecurityKey;

   private JdbcEntityManager em;

   public JDBCTransaction(String datasource, String passwordSecurityKey) {
      this.datasource = datasource;
      this.passwordSecurityKey = passwordSecurityKey;
   }

   public JDBCTransaction(String datasource) {
      this(datasource, null);
   }

   public JDBCTransaction() {
      this(null, null);
   }

   public static JdbcEntityManager createEntityManager() throws Exception {
      return createEntityManager(DAOTransaction.DEFAULT_DATASOURCE);
   }

   public static JdbcEntityManager createEntityManager(String datasource) throws Exception {

      if (datasource == null || datasource.isEmpty()) {
         throw new IllegalStateException("Supplied datasource was 'null' or and empty string.");
      }

      JdbcEntityManager manager = null;
      ApplicationServiceManager srvcmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvcmgr != null) {
         AppDatabase databaseInfo = srvcmgr.getDatabase(datasource);
         if (databaseInfo != null) {
            if (databaseInfo.isEnabled()) { // if database is marked enabled, create a factory...

               JDBCDriverInfo driverInfo = databaseInfo.getJDBCDriverInfo();
               manager = new JdbcEntityManager(datasource, databaseInfo.getUsername(), databaseInfo.getPassword(), driverInfo.getJDBCDriver(),
                     driverInfo.getJDBCUrl());

               // ask manager to test connection if enabled...???
               //         boolean enabled = manager.validateDBConnection();
               //         database.setEnabled(enabled);
            }
            // otherwise don't bother... it's marked disabled in the database table for some reason.
         }
         else {
            manager = new JdbcEntityManager(datasource);
         }
      }
      else {
         throw new IllegalStateException("No ApplicationServiceManager is available.");
      }

      return manager;
   }

   public static JdbcEntityManager createEntityManager(String datasource, String passwordSecurityKey) throws Exception {
      JdbcEntityManager manager = new JdbcEntityManager(datasource, passwordSecurityKey);
      return manager;

      // ask manager to test connection if enabled...???
      //         boolean enabled = manager.validateDBConnection();
      //         database.setEnabled(enabled);
   }

   @Override
   public JdbcEntityManager getEntityManager() {
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

      em.startTransaction();
   }

   @Override
   public void endTransaction() throws Exception {
      em.endTransaction();
      em = null;
   }

   @Override
   public void rollBackTransaction() throws Exception {
      em.rollBackTransaction();
      em = null;
   }

   @Override
   public boolean isExternalTransactionMonitoring() throws Exception {
      return em != null;
   }

   @Override
   public Exception handleGlobalSessionException(Class<?> refClass, String message, Exception ex) {

      // try to close the transaction....
      String rollBackErrMsg = null;
      if (em != null && em.isOpen()) {
         try {
            em.rollBackTransaction();
         }
         catch (Exception exs) {
            rollBackErrMsg = "Rollback during exception processing was unsuccessful: " + exs.getMessage();
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
      else {
         sb.append(" " + rollbackOkMsg);
      }

      return new Exception(sb.toString());
   }

   //   @Override
   //   public boolean isUsingServiceAccount() throws Exception {
   //      return true;
   //      //     return dbId == DBApplicationServiceManager.SERVICE_ACCOUNT_DBID;
   //   }

   @Override
   public String getDataSource() {
      return datasource;
   }

   @Override
   public String getPasswordSecurityKey() {
      return passwordSecurityKey;
   }
}
