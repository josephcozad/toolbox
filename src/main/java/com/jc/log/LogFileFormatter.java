package com.jc.log;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.jc.util.ConfigInfo;
import com.jc.util.FileSystem;

public class LogFileFormatter extends Formatter {

   final static String LOGFILE_FIELD_SEPARATOR = "|";

   private DateFormat MyDateFormat = null;

   public LogFileFormatter(String logid) {
      try {
         ConfigInfo info = ConfigInfo.getInstance();

         // Get the date format, use the following as the default.
         String date_format = "yyyy-MM-dd HH:mm:ss:SS";
         String propkey = "log." + logid + ".dateFormat";
         if (info.hasProperty(propkey)) {
            date_format = info.getProperty(propkey);
         }
         MyDateFormat = new SimpleDateFormat(date_format);
      }
      catch (FileNotFoundException fnfex) {
         fnfex.printStackTrace();
      }
   }

   public LogFileFormatter() {
      this(Logger.DEFAULT_LOG_ID);
   }

   @Override
   public String format(LogRecord logRec) {
      String message = getMessage(logRec);

      Throwable cause = logRec.getThrown();
      ExceptionMessageHandler expHandler = new ExceptionMessageHandler(cause);
      String classfield_info = expHandler.getExceptionLocation();
      String timestamp = MyDateFormat.format(new Date());

      String logLevel = logRec.getLevel().getName();

      // NANO_SECS|DATE_STAMP|LOG_LEVEL|CLASS_FIELD_INFO|MESSAGE
      String logLine = System.nanoTime() + LOGFILE_FIELD_SEPARATOR + timestamp + LOGFILE_FIELD_SEPARATOR + logLevel + LOGFILE_FIELD_SEPARATOR + classfield_info
            + LOGFILE_FIELD_SEPARATOR + message + FileSystem.NEWLINE;

      return logLine;
   }

   @Override
   public String getHead(Handler handler) {
      return LogFieldsEnum.NANO_SECS + "|" + LogFieldsEnum.DATE_STAMP + "|" + LogFieldsEnum.LOG_LEVEL + "|" + LogFieldsEnum.CLASS_FIELD_INFO + "|"
            + LogFieldsEnum.MESSAGE + FileSystem.NEWLINE;
   }

   private String getMessage(LogRecord logRec) {
      String message = logRec.getMessage();
      message = message.replace('\n', ' ');
      return (message);
   }
}
