package com.jc.log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import com.jc.exception.LoggableException;
import com.jc.util.ConfigInfo;
import com.jc.util.FileSystem;

/*
 * 
 *  This class extends the logging services provided by the Java Util Logging framework to provide logging 
 *  to a specified file or database table and is configured by the following properties:
 *  
 *  log.<logid>.logToConsole = boolean value that governs if log messages to the <logid> are written to 
 *                             the console as well. Default is true.
 *  
 *  log.<logid>.datasourceClass = string value that is the full package name of the datasource class that
 *                             will handle logging messages to a database. See DatabaseLogHandler and 
 *                             LogDatasource for more info.
 *                             
 *  log.<logid>.directory = string value that represents the directory under which the log will be written.
 *                          This string value supports the Java Util Logging FileHandler wild cards.
 *                          This property is ignored if the datasourceClass is also present for the <logid>.
 *                          See LogFileHandler and LogFileFormatter for more info.
 *                          
 *  log.<logid>.namePrefix = string value that represents the filename to which log messages for <logid> will
 *                           be written. This string value supports the Java Util Logging FileHandler wild cards.
 *                           If not specified the default value will be the <logid>.
 *
 *  log.use.java.tmpdir = boolean value; if present and no directory or datasource is specified for the <logid>,
 *                        all log messages will be written to a file under the java tmpdir. If not present, all
 *                        messages will be written to a file in the 'user.dir' as defined by System.getProperty().
 */

public class Logger {

   public final static String LOGFILE_FIELD_SEPARATOR = LogFileFormatter.LOGFILE_FIELD_SEPARATOR;
   public final static String DEFAULT_LOG_ID = "app";

   private static final Map<String, Logger> Loggers = new Hashtable<>();

   private static boolean LOGTO_CONSOLE = true;
   private static Handler CONSOLE_HANDLER;

   // If no logging location is specified, DEFAULT_LOGGING == true will log to a default location
   // and print that location to the console through stderr; DEFAULT_LOGGING == false, will cause 
   // default location logging to be turned off, and if no other location is available then logging
   // in general will be turned off.
   private static boolean DEFAULT_LOGGING = true;

   private Handler logHandler;
   private LogMetadata logMetadata;

   protected Logger() {} // So that this class can be extended.

   private Logger(String logid) {
      boolean logToConsole = LOGTO_CONSOLE;
      try {
         ConfigInfo info = ConfigInfo.getInstance().getConfigInfoFor("log." + logid);

         if (info.hasProperty("logToConsole")) {
            logToConsole = info.getPropertyAsBoolean("logToConsole");
         }

         if (info.hasProperty("datasourceClass")) { // logging to a database table...
            logHandler = createDatabaseLogHandler(info);
         }
         else { // Assume logging to a file...
            logHandler = createLogFileHandler(info, logid);
         }

         logMetadata = new LogMetadata(logHandler, logToConsole);
      }
      catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   private static Logger getInstance(String logid) {
      Logger logger = null;

      if (!Loggers.containsKey(logid)) {
         logger = new Logger(logid);

         // if DEFAULT_LOGGING is false and no log dir or datasource was specified, then the logHandler 
         // will be null and essentially logging is turned off for logid.
         if (logger.logHandler != null) {
            Loggers.put(logid, logger);
         }
         else {
            logger = null;
         }
      }
      else {
         logger = Loggers.get(logid);
         LogMetadata logMetadata = logger.getLogMetadata();
         if (logger != null && logMetadata.isFileBased()) {
            String filename = logMetadata.getLogfileName();

            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            String postfix = "_" + df.format(new Date()) + ".txt";

            if (filename.indexOf(postfix) < 0) {
               logger.close();
               logger = new Logger(logid);
               Loggers.put(logid, logger);
            }
         }
      }
      return logger;
   }

   // ----------------------------------------------------------------------

   public static void log(Class<?> loggingClass, Level level, String msg) {
      log(DEFAULT_LOG_ID, loggingClass, level, msg);
   }

   public static void log(String logid, Class<?> loggingClass, Level level, String msg) {

      if (logid == null || logid.isEmpty()) {
         logid = DEFAULT_LOG_ID;
      }

      Logger logger = Logger.getInstance(logid);
      if (logger != null) {
         logger.logInt(loggingClass, level, msg);
      }
   }

   /*
    * This method will log the supplied exception along with it's message, if available. 
    * Note that this method turns off logToConsole, while logging the exception only so 
    * that no stacktrace is sent to the console.
    */
   public static void log(Class<?> loggingClass, Level level, Throwable ex) {
      log(loggingClass, level, "", ex);
   }

   /*
    * This method will log the supplied exception along with it's message, if available,
    * and if a 'msg' was supplied that will be appended to the log record. Note that this
    * method turns off logToConsole, while logging the exception only so that no stacktrace 
    * is sent to the console.
    */
   public static void log(Class<?> loggingClass, Level level, String msg, Throwable ex) {
      log(DEFAULT_LOG_ID, loggingClass, level, msg, ex);
   }

   /*
    * This method will log the supplied exception along with it's message, if available,
    * and if a 'msg' was supplied that will be appended to the log record. Note that this
    * method turns off logToConsole, while logging the exception only so that no stacktrace 
    * is sent to the console.
    */
   public static void log(String logid, Class<?> loggingClass, Level level, String msg, Throwable ex) {

      if (logid == null || logid.isEmpty()) {
         logid = DEFAULT_LOG_ID;
      }

      Logger logger = Logger.getInstance(logid);
      if (logger != null) {

         String exceptionMsg = null;
         if (ex instanceof LoggableException) {
            exceptionMsg = ((LoggableException) ex).getMessage();
         }
         else {
            // EXCEPTION_NAME thrown. EXCEPTION_MESSAGE
            ExceptionMessageHandler exHandler = new ExceptionMessageHandler(ex, msg);
            String exceptionName = exHandler.getExceptionName();
            if (exceptionName != null && !exceptionName.isEmpty()) {
               exceptionName += " thrown. ";
            }
            exceptionMsg = exceptionName + exHandler.getExceptionMessage();
         }

         // Don't print the stacktrace to the console....
         boolean logtoConsoleSaved = LOGTO_CONSOLE;
         if (LOGTO_CONSOLE) {
            Logger.setConsoleLoggingOn(false);
         }

         logger.logInt(loggingClass, level, exceptionMsg, ex);

         Logger.setConsoleLoggingOn(logtoConsoleSaved);
      }
   }

   /*
    * Controls all logger instances as to whether logging to the console is on or off.
    */
   public static void setConsoleLoggingOn(boolean value) {
      LOGTO_CONSOLE = value;
      if (!Loggers.isEmpty()) {
         for (Logger logger : Loggers.values()) {
            LogMetadata loggerMetadata = logger.getLogMetadata();
            loggerMetadata.setLogToConsole(LOGTO_CONSOLE);
         }
      }
   }

   /*
    * Controls default logging on a global level. If value = false then no default logging will 
    * be available.
    */
   public static void setDefaultLogging(boolean value) {
      DEFAULT_LOGGING = value;
   }

   /*
    * Outputs the supplied fileContent to the logid's directory and returns the full path and
    * name of the file to which the content was saved.
    */
   public static String saveOutput(StringBuilder fileContent) {
      return saveOutput(DEFAULT_LOG_ID, fileContent);
   }

   /*
    * Outputs the supplied fileContent to the logid's directory and returns the full path and
    * name of the file to which the content was saved.
    */
   public static String saveOutput(String logid, StringBuilder fileContent) {

      if (logid == null || logid.isEmpty()) {
         logid = DEFAULT_LOG_ID;
      }

      Logger logger = getInstance(logid);

      String outfile = "Logging turned off, no outfile created.";
      if (logger != null) {
         outfile = "Outfile not created for logid '" + logid + "'.";
         String filename = logger.saveOutputInt(fileContent);
         if (filename != null && !filename.isEmpty()) {
            outfile = filename;
         }
      }
      return outfile;
   }

   public static String saveStackTrace(String logid, Throwable ex) {
      Writer stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      ex.printStackTrace(printWriter);
      String outfile = saveOutput(logid, new StringBuilder(stringWriter.toString()));
      return outfile;
   }

   public static String saveStackTrace(Throwable ex) {
      return saveStackTrace(null, ex);
   }

   public static LogMetadata getLogMetadata(String logid) {
      Logger logger = getInstance(logid);
      return logger.getLogMetadata();
   }

   public static void flushAllLogs() {
      for (String logid : Loggers.keySet()) {
         flushLog(logid);
      }
   }

   public static void flushLog(String logid) {
      Logger logger = getInstance(logid);
      if (logger != null) {
         logger.logHandler.flush();
      }
   }

   public static void setLogLevelForClass(Class<?> loggingClass, Level level) {
      java.util.logging.Logger alogger = java.util.logging.Logger.getLogger(loggingClass.getName());
      alogger.setLevel(level);
   }

   // -------------------------- Private Methods ------------------------------------------

   private Handler createDatabaseLogHandler(ConfigInfo info)
         throws SecurityException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
      // use properties to instantiate a datasource and pass to the handler...
      String datasourceClassName = info.getProperty("datasourceClass");
      Class<?> classObj = Class.forName(datasourceClassName);
      LogDatasource datasource = (LogDatasource) classObj.newInstance();
      Handler logHandler = new DatabaseLogHandler(datasource);
      return logHandler;
   }

   private Handler createLogFileHandler(ConfigInfo info, String logid) throws SecurityException, IOException {
      // Get log file location, use the user directory as the default.
      String logdir = "";
      String propkey = "directory";

      if (info.hasProperty(propkey)) {
         logdir = info.getProperty(propkey).replace("\\", File.separator);
      }
      else {
         // The property file doesn't have the prop key associated with this log id, create a default in the user.dir location.
         // possibly use java.io.tmpdir
         String prop = "log.use.java.tmpdir";
         if (info.hasProperty(prop) && info.getPropertyAsBoolean(prop)) {
            logdir = System.getProperty("java.io.tmpdir");
            if (logdir.endsWith(File.separator)) {
               logdir = logdir.substring(0, logdir.lastIndexOf(File.separator));
            }
         }
         else {
            if (DEFAULT_LOGGING) {
               logdir = System.getProperty("user.dir");
               logdir += File.separator + "logs" + File.separator + logid + File.separator;
               System.err.println("WARNING: No logging directory defined; a default log directory will be used at: " + logdir + ".");
            }
            else {
               logdir = null;
            }
         }
      }

      LogFileHandler logHandler = null;
      if (logdir != null) {
         // If the log directory doesn't exist, create it.
         File dir = new File(logdir);
         if (!dir.exists()) {
            dir.mkdirs();
         }

         if (!logdir.endsWith(File.separator)) {
            logdir = logdir.concat(File.separator);
         }

         // Assign file name prefix, use log id as the default.
         String namePrefix = logid;
         propkey = "namePrefix";
         if (info.hasProperty(propkey)) {
            namePrefix = info.getProperty(propkey);
         }

         Date now = new Date();
         String formattedDate = LogFileHandler.DATEFORMAT.format(now);

         String fileName = namePrefix + "_" + formattedDate;

         logHandler = new LogFileHandler(logdir + fileName + ".txt");
         logHandler.setNamePrefix(namePrefix);
         logHandler.setLogDir(logdir);
      }

      return logHandler;
   }

   private LogMetadata getLogMetadata() {
      return logMetadata;
   }

   private void logInt(Class<?> loggingClass, Level level, String msg) {
      logInt(loggingClass, level, msg, null);
   }

   private void close() {
      if (logMetadata.isFileBased()) {
         java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
         rootLogger.removeHandler(logHandler);

         logHandler.flush();
         logHandler.close();
         logHandler = null;
      }
   }

   private void logInt(Class<?> loggingClass, Level level, String msg, Throwable ex) {
      java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
      Handler[] handlers = rootLogger.getHandlers();
      for (Handler handler : handlers) {
         if (handler instanceof ConsoleHandler && CONSOLE_HANDLER == null) {
            CONSOLE_HANDLER = handler; // save it for later use.
         }

         // remove all handlers...
         rootLogger.removeHandler(handler);
      }

      // add the logid's handler...
      rootLogger.addHandler(logHandler);

      if (logMetadata.isLogToConsole()) {
         rootLogger.addHandler(CONSOLE_HANDLER);
      }

      java.util.logging.Logger alogger = java.util.logging.Logger.getLogger(loggingClass.getName());

      if (ex == null) {
         alogger.log(level, msg);
      }
      else {
         alogger.log(level, msg, ex);
      }

      rootLogger.addHandler(CONSOLE_HANDLER); // add it back...
   }

   private String saveOutputInt(StringBuilder fileContent) {
      String outfile = null;
      if (logHandler instanceof LogFileHandler) {
         outfile = ((LogFileHandler) logHandler).getLogfileName();
         outfile = outfile.substring(0, outfile.lastIndexOf("."));

         long random_num = System.currentTimeMillis();
         outfile = outfile + "_outfile_" + random_num + ".txt";
         try {
            FileSystem.writeContentOutToFile(outfile, new StringBuilder(fileContent));
         }
         catch (Exception ex) {
            Logger.log(Logger.class, Level.WARNING, ex.getMessage(), ex);
         }
      }
      return outfile;
   }
}
