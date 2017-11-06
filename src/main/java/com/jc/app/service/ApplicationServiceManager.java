package com.jc.app.service;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.jc.app.User;
import com.jc.app.data.domain.AppConfig;
import com.jc.app.data.domain.AppDatabase;
import com.jc.app.data.domain.AppLogRec;
import com.jc.app.data.domain.AppUser;
import com.jc.app.data.domain.GenericDatabase;
import com.jc.db.dao.DAOFactory;
import com.jc.db.dao.DAOTransaction;
import com.jc.exception.MethodUnimplementedException;
import com.jc.exception.SystemInfoException;
import com.jc.log.LogFieldsEnum;
import com.jc.log.LogFileHandler;
import com.jc.log.LogMetadata;
import com.jc.log.Logger;
import com.jc.shiro.AccessControlService;
import com.jc.util.ConfigInfo;
import com.jc.util.FileSystem;

/*
 * This class provides application generic services related to users, their roles, permissions and 
 * accessible databases; as well as getting info from the application logs.
 */

public abstract class ApplicationServiceManager extends ServiceManager {

   public final static String SERVICE_ACCOUNT_DATASOURCE = DAOTransaction.DEFAULT_DATASOURCE;

   private DAOFactory daoFactory;
   private String datasource;

   public ApplicationServiceManager() throws Exception {
      this(SERVICE_ACCOUNT_DATASOURCE); // default datasource
   }

   public ApplicationServiceManager(String datasource) throws Exception {
      super();
      this.datasource = datasource;
   }

   protected ApplicationServiceManager(DAOFactory daoFactory) throws Exception {
      this();
      if (daoFactory != null) {
         this.datasource = null;
         this.daoFactory = daoFactory;
      }
      else {
         throw (new Exception("Supplied DAOFactory object was null."));
      }
   }

   public static ApplicationServiceManager getServiceAccountServiceManager() throws Exception {
      ApplicationServiceManager srvcmgr = null;
      ConfigInfo configInfo = ConfigInfo.getInstance();

      // Create the application service manager....
      String appServiceMgrClassName = null;
      try {
         appServiceMgrClassName = configInfo.getProperty(SERVICE_MANAGER_PROPKEY);
         Class<?> appServiceMgrClass = Class.forName(appServiceMgrClassName);
         Constructor<?> constructor = appServiceMgrClass.getConstructor();
         Object object = constructor.newInstance();
         srvcmgr = (ApplicationServiceManager) object;
      }
      catch (Exception ex) {
         String outfile = Logger.saveStackTrace(ex);
         String message = "Error trying to construct the Service Account Service Manager '" + appServiceMgrClassName + "'. See full stacktrace at: " + outfile;
         Logger.log(ApplicationServiceManager.class, Level.SEVERE, message, ex);
      }
      return srvcmgr;
   }

   public static List<AppLogRec> readAppLog(Date fromDate, Date toDate) throws Exception {
      List<AppLogRec> appLogRecs = new ArrayList<>();

      LogMetadata appLogMetadata = Logger.getLogMetadata(Logger.DEFAULT_LOG_ID);

      DateFormat df = LogFileHandler.DATEFORMAT;
      List<String> fileDates = new ArrayList<>();
      if (fromDate.before(toDate)) {
         while (!fromDate.after(toDate)) { // include toDate in the stuff to return...
            String fileDate = df.format(fromDate);
            fileDates.add(fileDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(fromDate);
            cal.add(Calendar.HOUR, 24);
            fromDate = cal.getTime();
         }
      }
      else if (fromDate.after(toDate)) {
         DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
         String message = "The from date, " + df2.format(fromDate) + ", cannot be after the to date " + df2.format(toDate) + ".";
         throw new SystemInfoException(Level.INFO, AppServiceErrorCode.INVALID_FROM_DATE, message);
      }
      else { // they are equal...
         fileDates.add(df.format(fromDate));
      }

      String logdir = appLogMetadata.getLogDir();
      String namePrefix = appLogMetadata.getFilenamePrefix();

      for (String fileDate : fileDates) {
         String applogFilename = logdir + namePrefix + "_" + fileDate + ".txt";
         File logfile = new File(applogFilename);
         if (logfile.exists()) {
            String[] lines = FileSystem.readInFile(applogFilename);
            for (int i = 0; i < lines.length; i++) {
               String[] data = lines[i].split("\\|", LogFieldsEnum.getNumberOfFields());

               AppLogRec record = new AppLogRec();
               record.setNanoSecs(data[0]);
               record.setDatestamp(data[1]);
               record.setLoglevel(data[2]);
               record.setClassFieldInfo(data[3]);
               record.setMessage(data[4]);
               appLogRecs.add(record);
            }
         }
         // else ignore it...
      }

      return appLogRecs;
   }

   // Database Related Stuff ------------------------------------------------------------

   /*
    * Override this method to return the application database info for the supplied datasource.
    * The default implementation loads data based on info found in the ConfigInfo using the 
    * following keys: serviceAccount.dbIp, serviceAccount.schema, serviceAccount.pwd, and 
    * serviceAccount.uid.
    */
   public AppDatabase getDatabase(String datasource) throws Exception {
      if (datasource.equals(SERVICE_ACCOUNT_DATASOURCE)) {
         ConfigInfo configInfo = ConfigInfo.getInstance();
         GenericDatabase serviceAccountDBInfo = new GenericDatabase(configInfo);
         return serviceAccountDBInfo;
      }
      else {
         throw new Exception("Uknown datasource '" + datasource + "'; unable to create AppDatabase object.");
      }
   }

   protected String getDatasource() {
      return datasource;
   }

   protected DAOFactory getDAOFactory() throws Exception {
      return daoFactory;
   }

   protected void setDAOFactory(DAOFactory daoFactory) {
      this.daoFactory = daoFactory;
   }

   // Application Configuration Stuff ---------------------------------------------------

   /*
    * Override this method to return application config info objects. These can be 
    * retrieved from a database, or read in from a property file on disk. This method 
    * is called by the WebAppContextListener to load the ConfigInfo for the 
    * application to use.
    */
   public List<AppConfig> getAppConfigs() throws Exception {
      List<AppConfig> appConfigList = new ArrayList<>();
      return appConfigList;
   }

   // User Stuff ------------------------------------------------------------------------

   public String encryptPassword(String password) {
      return AccessControlService.encryptPassword(password);
   }

   public User createUser(String principal) throws Exception {
      throw new MethodUnimplementedException();
   }

   public AppUser getAppUser(String username) throws Exception {
      throw new MethodUnimplementedException();
   }

   public AppUser getAppUserByAppKey(String applicationKey) throws Exception {
      throw new MethodUnimplementedException();
   }
}
