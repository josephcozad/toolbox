package com.jc.exception;

import java.util.logging.Level;

import com.jc.log.ExceptionMessageHandler;
import com.jc.log.Logger;

/*
 * This class is intended to be used to either throw an application generated exception 
 * that does need to be logged, or to "wrap" an java.lang.Excetpion which can be analyzed 
 * and logged accordingly indicating the location of the exception and type and message.
 * An optional user consumable message can be added.
 */

public class LoggableException extends Exception {

   private static final long serialVersionUID = 6884004282029382190L;

   private ErrorCode errorCode;
   private Level loggingLevel;
   private String message;
   private boolean suspectedDatabaseIssue;

   // Empty constructor should never be needed.
   @SuppressWarnings("unused")
   private LoggableException() {
      super();
   }

   public LoggableException(Class<?> callingClass, Level loggingLevel, String message) {
      this(callingClass, loggingLevel, (String) null, message, (Throwable) null, (ErrorCode) null);
   }

   public LoggableException(Class<?> callingClass, Level loggingLevel, String message, ErrorCode errorCode) {
      this(callingClass, loggingLevel, (String) null, message, (Throwable) null, errorCode);
   }

   protected LoggableException(Class<?> callingClass, Level loggingLevel, String additionalMessage, Throwable cause) {
      this(callingClass, loggingLevel, (String) null, additionalMessage, cause, (ErrorCode) null);
   }

   protected LoggableException(Class<?> callingClass, Level loggingLevel, String messagePrefix, String additionalMessage, Throwable cause,
         ErrorCode errorCode) {
      super(cause);

      this.loggingLevel = loggingLevel;
      this.errorCode = errorCode;

      if (!(cause instanceof LoggableException)) {

         // EXCEPTION_NAME thrown. EXCEPTION_MESSAGE
         // Dev Note: if 'cuase' is null, then ExceptionMessageHandler should return back the additional 
         //     message as the exception message.
         ExceptionMessageHandler exHandler = new ExceptionMessageHandler(cause, additionalMessage);
         if (cause instanceof NullPointerException && !exHandler.hasLocationInfo()) {
            String outfile = Logger.saveStackTrace(cause); // write out stack trace...

            String exceptionName = exHandler.getExceptionName();
            if (exceptionName != null && !exceptionName.isEmpty()) {
               exceptionName += " thrown. ";
            }
            message = exceptionName + " See the outfile for more details: " + outfile;

            if (messagePrefix != null && !messagePrefix.isEmpty()) {
               message = messagePrefix + message;
            }

            suspectedDatabaseIssue = exHandler.isSuspectedDatabaseIssue();
         }
         else {
            String exceptionName = exHandler.getExceptionName();
            if (exceptionName != null && !exceptionName.isEmpty()) {
               exceptionName += " thrown. ";
            }
            message = exceptionName + exHandler.getExceptionMessage();

            if (messagePrefix != null && !messagePrefix.isEmpty()) {
               message = messagePrefix + message;
            }

            suspectedDatabaseIssue = exHandler.isSuspectedDatabaseIssue();
         }
         Logger.log(callingClass, loggingLevel, this);
      }
      else {
         LoggableException lex = ((LoggableException) cause);

         message = lex.message; // add additionalMessage ????
         if (messagePrefix != null && !messagePrefix.isEmpty()) {
            message = messagePrefix + message;
         }

         suspectedDatabaseIssue = lex.suspectedDatabaseIssue;

         // which error code takes precedence if both exist, the supplied one or the one in the supplied LoggableException
         ErrorCode lexErrorCode = lex.getErrorCode();
         if (lexErrorCode == null) {
            lexErrorCode = errorCode;
         }
         setErrorCode(lexErrorCode);
      }
   }

   LoggableException(Class<?> callingClass, Level loggingLevel, String messagePrefix, String additionalMessage, Throwable cause) {
      this(callingClass, loggingLevel, messagePrefix, additionalMessage, cause, (ErrorCode) null);
   }

   public static LoggableException createLoggableException(Class<?> callingClass, Level loggingLevel, Throwable cause) {
      LoggableException lex = null;
      if (!(cause instanceof LoggableException)) {
         lex = new LoggableException(callingClass, loggingLevel, null, cause);
      }
      else {
         lex = (LoggableException) cause;
      }
      return lex;
   }

   public static LoggableException createLoggableException(Class<?> callingClass, Level loggingLevel, String additionalMessage, Throwable cause) {
      LoggableException lex = null;
      if (!(cause instanceof LoggableException)) {
         lex = new LoggableException(callingClass, loggingLevel, additionalMessage, cause);
      }
      else {
         lex = (LoggableException) cause;
      }
      return lex;
   }

   @Override
   public String getMessage() {
      return (message);
   }

   public boolean isSuspectedDatabaseIssue() {
      return suspectedDatabaseIssue;
   }

   public Level getSeverityLevel() {
      return (loggingLevel);
   }

   public void setErrorCode(ErrorCode errorCode) {
      this.errorCode = errorCode;
   }

   public ErrorCode getErrorCode() {
      return errorCode;
   }

   public boolean hasErrorCode() {
      return errorCode != null;
   }
}
