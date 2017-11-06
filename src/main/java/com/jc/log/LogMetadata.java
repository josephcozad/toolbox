package com.jc.log;

import java.util.logging.FileHandler;
import java.util.logging.Handler;

public class LogMetadata {

   private final boolean fileBased;
   private String currentLogFileName;
   private boolean logToConsole;

   private String filenamePrefix;
   private String logdir;

   LogMetadata(Handler logHandler, boolean logToConsole) {

      fileBased = (logHandler != null) && (logHandler instanceof FileHandler);

      this.logToConsole = logToConsole;

      if (fileBased && logHandler instanceof LogFileHandler) {
         currentLogFileName = ((LogFileHandler) logHandler).getLogfileName();
         filenamePrefix = ((LogFileHandler) logHandler).getNamePrefix();
         logdir = ((LogFileHandler) logHandler).getLogDir();
      }
   }

   public String getLogfileName() {
      return currentLogFileName;
   }

   public boolean isFileBased() {
      return fileBased;
   }

   public boolean isLogToConsole() {
      return logToConsole;
   }

   void setLogToConsole(boolean logToConsole) {
      this.logToConsole = logToConsole;
   }

   public String getFilenamePrefix() {
      return filenamePrefix;
   }

   public String getLogDir() {
      return logdir;
   }
}
