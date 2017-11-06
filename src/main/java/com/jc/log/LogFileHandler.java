package com.jc.log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.FileHandler;

public class LogFileHandler extends FileHandler {

   public final static DateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMdd");

   private final String logfilename;
   private String namePrefix;
   private String logDir;

   public LogFileHandler(String logfilename) throws SecurityException, IOException {
      super(logfilename, true); // appends to the file if the file already exists.
      init();
      this.logfilename = logfilename;
   }

   public String getLogfileName() {
      return logfilename;
   }

   void setNamePrefix(String namePrefix) {
      this.namePrefix = namePrefix;
   }

   public String getNamePrefix() {
      return namePrefix;
   }

   void setLogDir(String logDir) {
      this.logDir = logDir;
   }

   public String getLogDir() {
      return logDir;
   }

   private void init() {
      LogFileFormatter formatterTxt = new LogFileFormatter();
      setFormatter(formatterTxt);

      LogFilter filter = new LogFilter();
      setFilter(filter);
   }
}
