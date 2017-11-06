package com.jc.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import com.jc.app.data.domain.AppConfig;
import com.jc.app.service.ApplicationServiceManager;
import com.jc.log.Logger;
import com.jc.util.ConfigInfo;

public abstract class WebAppContextListener implements javax.servlet.ServletContextListener {

   protected final static String SERVICE_ACCOUNT_DP_IP_PROPKEY = "serviceAccount.dbIp";
   protected final static String DEFAULT_LOGGING_ON_PROPKEY = "defaultLoggingOn";
   protected final static String CONSOLE_LOGGING_ON_PROPKEY = "consoleLoggingOn";

   protected final static String APP_VERSION_PROPKEY = "app.version";
   protected final static String APP_PROPERTY_FILE_PROPKEY = "app.properties";
   protected final static String APP_SERVICE_MANAGER_PROPKEY = "app.serviceManagerClass";
   protected final static String APP_SERVICE_ACCOUNT_SCHEMA_PROPKEY = "serviceAccount.schema";
   protected final static String APP_SERVICE_ACCOUNT_PWD_PROPKEY = "serviceAccount.pwd";
   protected final static String APP_SERVICE_ACCOUNT_UID_PROPKEY = "serviceAccount.uid";

   public final static String INITIALIZATION_ERROR_PROPKEY = "initializationErrorMsg";

   @Override
   public void contextInitialized(ServletContextEvent arg0) {
      try {

         ConfigInfo info = ConfigInfo.getInstance();

         // Log only to stderr...
         Logger.setConsoleLoggingOn(true);
         Logger.setDefaultLogging(false);

         // Init application configInfo from the servletContext...
         ServletContext sc = arg0.getServletContext();

         // Add the service account database ip value if one is available to the configInfo...
         String serviceAccountDBIpInfo = getServiceAccountDBIpInfo();
         if (serviceAccountDBIpInfo != null && !serviceAccountDBIpInfo.isEmpty()) {
            info.addProperty(SERVICE_ACCOUNT_DP_IP_PROPKEY, serviceAccountDBIpInfo);
         }

         // user defined required application context params to load...
         List<String> requiredContextParamKeys = getRequiredApplicationContextParamKeys();
         for (String parameter : requiredContextParamKeys) {
            String paramterValue = sc.getInitParameter(parameter);
            if (paramterValue != null && !paramterValue.isEmpty()) {
               info.addProperty(parameter, paramterValue);
            }
            else {
               throw new ServletException("'" + parameter + "' undefined in the web.xml file.");
            }
         }

         // user defined optional application context params to load...
         List<String> appContextParamKeys = getApplicationContextParamKeys();
         for (String parameter : appContextParamKeys) {
            String paramterValue = sc.getInitParameter(parameter);
            if (paramterValue != null && !paramterValue.isEmpty()) {
               info.addProperty(parameter, paramterValue);
            }
            else {
               String message = "'" + parameter + "' undefined in the web.xml file.";
               System.out.println(message);
               Logger.log(getClass(), Level.WARNING, message);
            }
         }

         // These are default optional values that are not required to be in the web.xml
         String[] optionalContextParamKeys = {
               DEFAULT_LOGGING_ON_PROPKEY, CONSOLE_LOGGING_ON_PROPKEY
         };

         for (String parameter : optionalContextParamKeys) {
            String paramterValue = sc.getInitParameter(parameter);
            if (paramterValue != null && !paramterValue.isEmpty()) {
               info.addProperty(parameter, paramterValue);
            }
         }

         // load any properties from a system file...
         if (info.hasProperty(APP_PROPERTY_FILE_PROPKEY)) {
            String propertyFile = info.getProperty(APP_PROPERTY_FILE_PROPKEY);
            File file = new File(propertyFile);
            if (file.exists()) {
               info.addPropertiesFromFile(propertyFile);
            }
            // else file doesn't exist, skip it...
         }

         // Loads application configInfo from the ApplicationServiceManager...
         ApplicationServiceManager srvcmgr = createApplicationServiceManager();
         if (srvcmgr != null) {
            List<AppConfig> configList = srvcmgr.getAppConfigs();
            for (AppConfig configInfo : configList) {
               if (configInfo.isActive()) {
                  String path = configInfo.getKey();
                  String value = configInfo.getValue();
                  info.addProperty(path, value);
               }
            }
         }

         loadLocalConfigInfo(info);

         // Turn on logging to tmp dir if no log location specified for 'app'...
         boolean defaultLoggingOn = false;
         if (info.hasProperty(DEFAULT_LOGGING_ON_PROPKEY)) {
            defaultLoggingOn = info.getPropertyAsBoolean(DEFAULT_LOGGING_ON_PROPKEY);
         }
         Logger.setDefaultLogging(defaultLoggingOn);

         // Turn on console logging for the entire application...
         boolean consoleLoggingOn = false;
         if (info.hasProperty(CONSOLE_LOGGING_ON_PROPKEY)) {
            consoleLoggingOn = info.getPropertyAsBoolean(CONSOLE_LOGGING_ON_PROPKEY);
         }
         Logger.setConsoleLoggingOn(consoleLoggingOn);
      }
      catch (Exception ex) {
         logExceptionDuringInitialization(ex);
      }
   }

   @Override
   public void contextDestroyed(ServletContextEvent arg0) {}

   protected abstract String getServiceAccountDBIpInfo() throws Exception;

   protected abstract ApplicationServiceManager createApplicationServiceManager() throws Exception;

   protected List<String> getRequiredApplicationContextParamKeys() {
      return new ArrayList<>();
   }

   protected List<String> getApplicationContextParamKeys() throws Exception {
      return new ArrayList<>();
   }

   protected void logExceptionDuringInitialization(Throwable ex) {
      Logger.setDefaultLogging(true);

      Date date = new Date(); // now
      SimpleDateFormat dt = new SimpleDateFormat("yyyyMMddhhmmss");
      String appErrorCode = dt.format(date);
      String outfile = Logger.saveStackTrace(ex);
      String message = "See the " + outfile + " file for more details.";
      //        System.err.println(message);
      Logger.log(getClass(), Level.SEVERE, "APPLICATION INITIALIZATION ERROR: (" + appErrorCode + ") " + message);

      try {
         message = "Initialization Error: " + appErrorCode;
         ConfigInfo info = ConfigInfo.getInstance();
         info.addProperty(INITIALIZATION_ERROR_PROPKEY, message);
      }
      catch (FileNotFoundException fnfex) {
         fnfex.printStackTrace();
      }
   }

   protected boolean isLocal() {
      boolean local = false;
      String filename = localFileName();
      if (filename != null && !filename.isEmpty()) {
         File localFile = new File(filename);
         local = localFile.exists();
      }
      return local;
   }

   protected void loadLocalConfigInfo(ConfigInfo info) throws Exception {}

   protected String localFileName() {
      return "";
   }
}
