package com.jc.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jc.log.ExceptionMessageHandler;
import com.jc.util.StopWatch;
import com.jc.util.StringUtils;

public abstract class ResultSetProcessor {

   private final List<Throwable> Errors;
   private final List<String> Warnings;
   private int NumRecordsRetrieved;
   private final StopWatch stopWatch;

   public ResultSetProcessor() {
      stopWatch = new StopWatch();
      Errors = new ArrayList<Throwable>();
      Warnings = new ArrayList<String>();
      NumRecordsRetrieved = 0;
   }

   public abstract String getQuery();

   public abstract void processResultSet(ResultSet result) throws SQLException;

   public int getNumRowsRetrieved() {
      return (NumRecordsRetrieved);
   }

   public boolean hasErrors() {
      return !Errors.isEmpty();
   }

   public boolean hasWarnings() {
      return !Warnings.isEmpty();
   }

   /**
    * Returns the total time, in ms, it took to run the query.
    */
   public long getTotalRunTime() {
      return stopWatch.getTime("Request");
   }

   /**
    * Returns the amount to time taken, in ms, to process the ResultSet object.
    */
   public long getTotalResultSetProcessingTime() {
      return stopWatch.getTime("ResultSet");
   }

   /**
    * Override this method to take advantage of setting parameters in the connection's PreparedStatement object.
    */
   protected List<Object> getParameterList() {
      return new ArrayList<Object>();
   }

   protected void incrementRecordCount() {
      NumRecordsRetrieved++;
   }

   protected void addWarning(String message) {
      Warnings.add(message);
   }

   protected void addError(Throwable errorException) {
      Errors.add(errorException);
   }

   public Throwable[] getErrors() {
      return Errors.toArray(new Throwable[Errors.size()]);
   }

   protected String[] getWarnings() {
      return Warnings.toArray(new String[Warnings.size()]);
   }

   void startPerfTracking() {
      try {
         stopWatch.start("Request");
      }
      catch (Exception ex) {
         addError(ex);
      }
   }

   void stopPerfTracking() {
      stopWatch.stop("Request");
   }

   void startResultSetPerfTracking() {
      try {
         stopWatch.start("ResultSet");
      }
      catch (Exception ex) {
         this.addError(ex);
      }
   }

   void stopResultSetPerfTracking() {
      stopWatch.stop("ResultSet");
   }

   String getLogMessage() {
      StringBuilder logmsg = new StringBuilder();

      logmsg.append(getErrorMessage());
      logmsg.append(getWarningMessage());

      int num_rows = getNumRowsRetrieved();
      if (num_rows > -1) {
         logmsg.append(" NUM_ROWS: ").append(num_rows).append('\n');
      }
      logmsg.append(getQueryInfo()).append('\n');

      return logmsg.toString();
   }

   String getErrorMessage() {
      if (!hasErrors()) {
         return StringUtils.EMPTY;
      }

      Throwable[] errors = getErrors();
      StringBuilder msg = new StringBuilder();

      if (errors.length > 1) {
         msg.append("There were ");
         msg.append(errors.length);
         msg.append(" errors reported. ");
      }
      else {
         msg.append("There was 1 error reported. ");
      }

      for (int i = 0; i < errors.length; i++) {
         if (errors.length > 1) {
            msg.append("ERROR[" + i + "]: ");
         }
         else {
            msg.append("ERROR: ");
         }

         msg.append(ExceptionMessageHandler.formatExceptionMessage(errors[i]));
         if (i + 1 < errors.length) {
            msg.append("; ");
         }
      }

      return msg.append(getQueryInfo()).toString();
   }

   private String getWarningMessage() {
      if (!hasWarnings()) {
         return StringUtils.EMPTY;
      }

      String[] warnings = getWarnings();
      StringBuilder msg = new StringBuilder();

      if (warnings.length > 1) {
         msg.append("There were ");
         msg.append(warnings.length);
         msg.append(" warnings reported. ");
      }
      else {
         msg.append("There was 1 warning reported. ");
      }

      for (int i = 0; i < warnings.length; i++) {
         if (warnings.length > 1) {
            msg.append("WARNING[" + i + "]: ");
         }
         else {
            msg.append("WARNING: ");
         }
         msg.append(warnings[i].replace("" + '\n', "; "));
         if (i + 1 < warnings.length) {
            msg.append("; ");
         }
      }

      return msg.append(getQueryInfo()).toString();
   }

   private String getQueryInfo() {
      StringBuilder info = new StringBuilder("TOTAL_QUERY_TIME: ");
      info.append(getTotalRunTime() + "; ");
      info.append("QUERY: ").append(getQuery());

      return info.toString();
   }
}
