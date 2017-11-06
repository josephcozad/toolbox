package com.jc.log.audit;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;

import com.jc.log.Logger;
import com.jc.shiro.AccessControlService;
import com.jc.util.ConfigInfo;

public abstract class AuditLogger {

   private static AuditLogger AuditLogger;

   public AuditLogger() throws Exception {}

   static AuditLogger getInstance() {
      if (AuditLogger == null) {
         String auditLoggerClassName = null;
         try {
            auditLoggerClassName = ConfigInfo.getInstance().getProperty("log.audit.loggerClass");
            Class<?> loggerClass = Class.forName(auditLoggerClassName);
            Constructor<?> constructor = loggerClass.getConstructor();
            Object object = constructor.newInstance();
            AuditLogger = (AuditLogger) object;
         }
         catch (Exception ex) {
            String outfile = Logger.saveStackTrace(ex);
            String message = "Error trying to construct the AuditLoggerClassName '" + auditLoggerClassName + "'. See full stacktrace at: " + outfile;
            Logger.log(AuditLogDatasource.class, Level.SEVERE, message, ex);
         }
      }
      return AuditLogger;
   }

   /*
    * Called by referringClass to log an audit 'action'. Typically an 'action' is a 
    * standard simple string describing an action that took place by an application user.
    */
   public static void log(Class<?> referringClass, String action) throws Exception {
      log(referringClass, action, null);
   }

   /*
    * Called by referringClass to log an audit 'action' and related 'comment'.
    */
   public static void log(Class<?> referringClass, String action, String comment) {
      try {
         AuditLogger serviceManager = getInstance();
         serviceManager.logAuditInfo(referringClass, action, comment);
      }
      catch (Exception ex) {
         Logger.log(AuditLogger.class, Level.SEVERE, ex);
      }
   }

   /*
    * Called by referringClass to log an audit 'action'. Typically an 'action' is a 
    * standard simple string describing an action that took place by an application user.
    */
   public static void addToLog(Class<?> referringClass, String action) throws Exception {
      addToLog(referringClass, action, null);
   }

   /*
    * Called by referringClass to log an audit 'action' and related 'comment'.
    */
   public static void addToLog(Class<?> referringClass, String action, String comment) {
      try {
         AuditLogger serviceManager = getInstance();
         serviceManager.addAuditInfo(referringClass, action, comment);
      }
      catch (Exception ex) {
         Logger.log(AuditLogger.class, Level.SEVERE, ex);
      }
   }

   /*
    * Outputs all audit log info.
    */
   public static void publish() {
      try {
         AuditLogger serviceManager = getInstance();
         serviceManager.publishAuditInfo();
      }
      catch (Exception ex) {
         Logger.log(AuditLogger.class, Level.SEVERE, ex);
      }
   }

   /*
    * Clears all saved audit log info without outputting.
    */
   public static void clear() {
      try {
         AuditLogger serviceManager = getInstance();
         serviceManager.clearAuditInfo();
      }
      catch (Exception ex) {
         Logger.log(AuditLogger.class, Level.SEVERE, ex);
      }
   }

   /*
    * Override this method to use the supplied AuditInfo to create the
    * message that will be logged.
    */
   protected abstract String createMessage(AuditInfo auditInfo) throws Exception;

   /*
    * Override this method to save the audit message to some data source.
    */
   protected abstract void persistAuditLogMessage(String message) throws Exception;

   private void logAuditInfo(Class<?> referringClass, String action, String comment) {
      try {
         AuditInfo auditInfo = new AuditInfo();
         auditInfo.setReferringClass(referringClass);
         auditInfo.setAction(action);
         auditInfo.setComment(comment);

         String message = createMessage(auditInfo);
         if (message != null && !message.isEmpty()) {
            Logger.log(AuditInfo.AUDITLOG_LOGID, auditInfo.getReferringClass(), Level.INFO, message);
         }
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   private void addAuditInfo(Class<?> referringClass, String action, String comment) {
      try {
         AuditInfo auditInfo = new AuditInfo();
         auditInfo.setReferringClass(referringClass);
         auditInfo.setAction(action);
         auditInfo.setComment(comment);

         AuditableUser user = (AuditableUser) AccessControlService.getUser();
         user.addAuditInfo(auditInfo);
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   private void publishAuditInfo() {
      try {
         AuditableUser user = (AuditableUser) AccessControlService.getUser();
         List<AuditInfo> auditInfoList = user.getAuditInfo();

         if (auditInfoList != null && !auditInfoList.isEmpty()) {
            for (AuditInfo auditInfo : auditInfoList) {
               String message = createMessage(auditInfo);
               if (message != null && !message.isEmpty()) {
                  Logger.log(AuditInfo.AUDITLOG_LOGID, auditInfo.getReferringClass(), Level.INFO, message);
               }
            }
            auditInfoList.clear();
         }
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   private void clearAuditInfo() throws Exception {
      AuditableUser user = (AuditableUser) AccessControlService.getUser();
      user.clearAuditInfo();
   }
}
