package com.jc.db.dao.jdbc;

import com.jc.log.ExceptionMessageHandler;

public final class JDBCDaoDeleteException extends IllegalStateException {

   private static final long serialVersionUID = 8693188498488888113L;

   private final Throwable[] errorExceptions;

   public JDBCDaoDeleteException(Throwable[] errorExceptions) {
      super();
      this.errorExceptions = errorExceptions;
   }

   @Override
   public String getMessage() {
      StringBuilder sb = new StringBuilder("Error while removing data.");

      if (errorExceptions != null && errorExceptions.length > 0) {
         sb.append(" ");

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

      sb.append(" All data was rolled back and no data was saved to the database.");

      return sb.toString();
   }
}
