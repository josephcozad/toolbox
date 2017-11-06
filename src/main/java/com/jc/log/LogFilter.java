package com.jc.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class LogFilter implements Filter {

   @Override
   public boolean isLoggable(LogRecord logRec) {
      String loggerName = logRec.getLoggerName();
      boolean loggable = loggerName.contains("com.jc");
      return loggable;
   }
}
