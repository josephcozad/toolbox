package com.jc.app.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import com.jc.app.data.domain.AppDatabase;
import com.jc.app.service.ApplicationServiceManager;
import com.jc.db.JDBCDriverInfo;
import com.jc.exception.LoggableException;
import com.jc.log.ExceptionMessageHandler;
import com.jc.log.Logger;

public class JPAUtils {

   private static HashMap<String, DBFactoryInfo> dbFactories;

   public static EntityManagerFactory getEntityFactory(String datasource) throws Exception {
      EntityManagerFactory factory = null;
      boolean stale = false;
      if (dbFactories != null && !dbFactories.isEmpty() && dbFactories.containsKey(datasource)) {
         DBFactoryInfo factoryInfo = dbFactories.get(datasource);
         factory = factoryInfo.entityManagerFactory;
         stale = factoryInfo.isStale();
      }

      if (stale || (factory == null) || (factory != null && !factory.isOpen())) {
         refreshDBFactories(datasource);
         // Because the dbId may not have been loaded (disabled because of an issue accessing it)
         // check the dbFactories to see if the dbId is even in it before getting the factoryInfo.
         if (dbFactories != null && !dbFactories.isEmpty() && dbFactories.containsKey(datasource)) {
            DBFactoryInfo factoryInfo = dbFactories.get(datasource);
            factory = factoryInfo.entityManagerFactory;
         }
      }

      return factory;
   }

   public static void refreshDBConnections() throws Exception {
      Set<String> datasources = dbFactories.keySet();
      List<String> datasourceList = new ArrayList<>(datasources);
      for (String datasource : datasourceList) {
         refreshDBFactories(datasource);
      }
   }

   public static List<AppDatabase> loadDBFactoryInfo(List<AppDatabase> databaseList) throws Exception {
      if (dbFactories == null) {
         dbFactories = new HashMap<>();
      }

      for (AppDatabase database : databaseList) {
         boolean enabled = loadDBFactoryInfo(database);
         database.setEnabled(enabled);
      }
      return databaseList;
   }

   public static boolean loadDBFactoryInfo(AppDatabase databaseInfo) throws Exception {
      boolean enabled = false;
      JDBCDriverInfo driverInfo = databaseInfo.getJDBCDriverInfo();

      try {
         if (!dbFactories.containsKey(databaseInfo.getDatasource())) { // if no factory exists for the datasource...
            if (databaseInfo.isEnabled()) { // if database is marked enabled, create a factory...
               Map<String, String> properties = new HashMap<>();
               properties.put("javax.persistence.jdbc.url", driverInfo.getJDBCUrl());
               properties.put("javax.persistence.jdbc.driver", driverInfo.getJDBCDriver());

               String username = databaseInfo.getUsername();
               if (username != null && !username.isEmpty()) {
                  properties.put("javax.persistence.jdbc.user", username);
               }

               String password = databaseInfo.getPassword();
               if (password != null && !password.isEmpty()) {
                  properties.put("javax.persistence.jdbc.password", password);
               }

               EntityManagerFactory factory = Persistence.createEntityManagerFactory("OnBoardPU", properties);
               enabled = true;

			   String message = "Num dbFactories[" + dbFactories.size() + "] before adding '" + databaseInfo.getDatasource() + "'.";
               Logger.log(JPAUtils.class, Level.INFO, message);

               DBFactoryInfo factoryInfo = new DBFactoryInfo(factory);
               dbFactories.put(databaseInfo.getDatasource(), factoryInfo);

               message = "Created EntityManagerFactory for " + databaseInfo.getDatasource() + ", user " + databaseInfo.getUsername() + ", at "
                     + driverInfo.getJDBCUrl() + ". Num DBFactories [" + dbFactories.size() + "]";
               Logger.log(JPAUtils.class, Level.INFO, message);
            }
            // otherwise don't bother... it's marked disabled in the database table for some reason.
         }
         else { // factory info exists, assume available...
            enabled = databaseInfo.isEnabled();
         }
      }
      catch (PersistenceException pexc) {
         String message = pexc.getMessage();
         if (message.contains("Unable to build entity manager factory")) {
            //skp it no entity manager factory for this.
            LoggableException lex = LoggableException.createLoggableException(JPAUtils.class, Level.SEVERE, pexc);
            String message2 = "Unable to create an EntityManagerFactory for " + databaseInfo.getDatasource() + ", user " + databaseInfo.getUsername() + ", at "
                  + driverInfo.getJDBCUrl() + ": " + lex.getMessage();
            Logger.log(JPAUtils.class, Level.SEVERE, message2); // Could be down, log and ignore.
         }
         else {
            throw new Exception(ExceptionMessageHandler.formatExceptionMessage(pexc) + " DBId '" + databaseInfo.getDatasource() + "'.");
         }
      }
      catch (Exception ex) {
         throw new Exception(ExceptionMessageHandler.formatExceptionMessage(ex) + " DBId '" + databaseInfo.getDatasource() + "'.");
      }

      return enabled;
   }

   public static void removeDBFactoryInfo(String datasource) {
      if (dbFactories != null && !dbFactories.isEmpty() && dbFactories.containsKey(datasource)) {
         dbFactories.remove(datasource);
         String message = "Removed EntityManagerFactory for " + datasource + ". Num DBFactories [" + dbFactories.size() + "]";
         Logger.log(JPAUtils.class, Level.INFO, message);
      }
   }

   private static synchronized void refreshDBFactories(String datasource) throws Exception {
      if (dbFactories != null && dbFactories.containsKey(datasource)) {
         DBFactoryInfo factoryInfo = dbFactories.get(datasource);
         factoryInfo.close();
         String message = "Num dbFactories[" + dbFactories.size() + "] before removing '" + datasource + "'.";
         Logger.log(JPAUtils.class, Level.INFO, message);
         dbFactories.remove(datasource);
         message = "Removed entity manager factory for '" + datasource + "' from dbFactories store; num dbFactories[" + dbFactories.size() + "].";
         Logger.log(JPAUtils.class, Level.INFO, message);
      }

      ApplicationServiceManager srvcmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvcmgr != null) {
         AppDatabase databaseInfo = srvcmgr.getDatabase(datasource);
         if (databaseInfo != null) {
            List<AppDatabase> databases = new ArrayList<>();
            databases.add(databaseInfo);
            loadDBFactoryInfo(databases);
         }
         else {
            throw new IllegalStateException("Unable to load database factory for datasource '" + datasource + "'.");
         }
      }
      else {
         throw new IllegalStateException("No ApplicationServiceManager is available.");
      }
   }

   public static class DBFactoryInfo {

      private EntityManagerFactory entityManagerFactory;
      private final long timeStamp;
      private final long timeToExpire; // number of millisecs before it's stale...

      public DBFactoryInfo(EntityManagerFactory factory) {
         entityManagerFactory = factory;

         timeStamp = System.currentTimeMillis();

         Calendar c = Calendar.getInstance();
         c.set(Calendar.HOUR_OF_DAY, 23);
         c.set(Calendar.MINUTE, 59);
         c.set(Calendar.SECOND, 59);
         c.set(Calendar.MILLISECOND, 0);
         c.add(Calendar.DAY_OF_MONTH, 0);
         long midnight = c.getTimeInMillis();

         timeToExpire = midnight - timeStamp;
      }

      public boolean isStale() {
         long now = System.currentTimeMillis();
         return (now - timeStamp) > timeToExpire;
      }

      public void close() {
         if (entityManagerFactory.isOpen()) {
            try {
               entityManagerFactory.close();
            }
            catch (Exception ex) {
               Logger.log(JPAUtils.class, Level.SEVERE, ex);
            }
         }
         entityManagerFactory = null;
      }
   }
}
