package com.jc.db.dao.jdbc;

import com.jc.log.ExceptionMessageHandler;

public class JDBCDaoException extends Exception {

   private static final long serialVersionUID = -3140951278067168374L;

   private final Throwable[] errorExceptions;
   private final String queryString;

   public JDBCDaoException(Throwable[] errorExceptions) {
      this(errorExceptions, "");
   }

   public JDBCDaoException(Throwable[] errorExceptions, String queryString) {
      super();

      this.errorExceptions = errorExceptions;
      this.queryString = queryString;
   }

   public Throwable[] getErrorExceptions() {
      return errorExceptions;
   }

   public String getQueryString() {
      return queryString;
   }

   @Override
   public String getMessage() {
      StringBuilder sb = new StringBuilder();

      if (errorExceptions != null && errorExceptions.length > 0) {
         //    EXCEPTION_NAME was thrown at LOCATION: EXCEPTION_MSG.
         for (int i = 0; i < errorExceptions.length; i++) {
            ExceptionMessageHandler exceptionMsgHandler = new ExceptionMessageHandler(errorExceptions[i]);
            sb.append(exceptionMsgHandler.getExceptionName() + " was thrown at " + exceptionMsgHandler.getExceptionLocation() + ": "
                  + exceptionMsgHandler.getExceptionMessage());
            if (i + 1 < errorExceptions.length) {
               sb.append("; ");
            }
            else {
               sb.append(".");
            }
         }
      }
      else {
         sb.append("No exception message is available.");
      }

      if (queryString != null && !queryString.isEmpty()) {
         sb.append(" QUERY: " + queryString);
      }

      return sb.toString();
   }
}
