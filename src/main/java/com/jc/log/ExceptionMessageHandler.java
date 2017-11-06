package com.jc.log;

import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.SQLGrammarException;

import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;

public class ExceptionMessageHandler {

   private final static String[] EXCLUDED_METHOD_NAMES = {
         "format", "addMessage", "handleGlobalSessionException", "logMessage", "logDebugMessage",
   };

   private static List<String> INCLUDE_PACKAGE_NAMES = new ArrayList<>();

   static {
      List<String> includedPackageNames = new ArrayList<>();
      includedPackageNames.add("com.jc");
      setIncludedPackageNames(includedPackageNames);
   }

   private boolean suspectedDatabaseIssue;
   private String exceptionMessage;
   private String exceptionName;
   private String exceptionLocation;
   private boolean hasLocationInfo;

   public ExceptionMessageHandler(Throwable cause) {
      this(cause, null);
   }

   public ExceptionMessageHandler(Throwable cause, String additionalMessage) {

      if (INCLUDE_PACKAGE_NAMES == null || INCLUDE_PACKAGE_NAMES.isEmpty()) {
         Logger.log(getClass(), Level.SEVERE,
               "No packages are contained in 'INCLUDE_PACKAGE_NAMES', this could cause in accurate logging of Exception messages.");
      }

      // Dev Note: if 'cause' is null, then ExceptionMessageHandler should return back the additional 
      //     message as the exception message.
      exceptionMessage = "";
      exceptionName = "";
      exceptionLocation = "";

      Throwable baseCause = getBaseCause(cause);
      if (baseCause != null) {
         exceptionName = baseCause.getClass().getSimpleName();

         if (baseCause instanceof java.lang.IllegalArgumentException) {
            String message = baseCause.getMessage();
            if (message != null && !message.isEmpty()) {
               exceptionMessage += message + " is an illegal argument.";
            }
         }
         else if (suspectedDatabaseIssue) {
            exceptionMessage += getSuspectedDatabaseIssueMessage(baseCause);
         }
         else {
            String message = baseCause.getMessage();
            if (message != null && !message.isEmpty()) {
               exceptionMessage += message;
            }
         }

         if (additionalMessage != null && !additionalMessage.isEmpty()) {
            exceptionMessage += " " + additionalMessage;
         }

         exceptionLocation = getExceptionLocation(baseCause);
      }
      else {
         // Create a default message...
         exceptionMessage = "No exception object was supplied.";
         if (additionalMessage != null && !additionalMessage.isEmpty()) {
            exceptionMessage = additionalMessage;
         }
      }

      if (exceptionLocation == null || exceptionLocation.isEmpty()) { //No location available.
         exceptionLocation = getExceptionLocation(new Exception());
      }
   }

   public static void setIncludedPackageNames(List<String> includedPackageNames) {
      INCLUDE_PACKAGE_NAMES = includedPackageNames;
   }

   public boolean hasLocationInfo() {
      return hasLocationInfo;
   }

   public String getExceptionLocation() {
      return exceptionLocation;
   }

   public String getExceptionName() {
      return exceptionName;
   }

   public String getExceptionMessage() {
      return exceptionMessage;
   }

   public boolean isSuspectedDatabaseIssue() {
      return suspectedDatabaseIssue;
   }

   /*
    * Formats the supplied Exception object into a readable useful message that 
    * can be displayed. Message shows the name of the exception thrown, and where 
    * in the code the exception was thrown.
    */
   public static String formatExceptionMessage(Throwable cause) {
      // Format of returned value: MESSAGE EXCEPTION_NAME was thrown at LOCATION.
      ExceptionMessageHandler exceptionHandler = new ExceptionMessageHandler(cause);
      String exceptionMessage = exceptionHandler.getExceptionMessage();
      if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
         exceptionMessage += " ";
      }
      return exceptionMessage + exceptionHandler.getExceptionName() + " was thrown at " + exceptionHandler.getExceptionLocation();
   }

   // ---------------------- Private Methods ----------------------

   private String getExceptionLocation(Throwable cause) {
      StackTraceElement[] stackTrace = null;
      if (cause != null) {
         stackTrace = cause.getStackTrace();
      }

      if (stackTrace == null) {
         stackTrace = Thread.currentThread().getStackTrace();
      }

      String className = null;
      String methodName = null;
      int lineNumber = -1;

      for (StackTraceElement stackElement : stackTrace) {
         className = stackElement.getClassName();
         // look for a class name whose package is included and not excluded...
         if (includedPackage(className) && (!excludedPackage(className) && !excludedClassName(className))) {
            methodName = stackElement.getMethodName();
            if (!excludeMethodName(methodName)) {
               methodName = stackElement.getMethodName();
               // remove the package info from the class name...
               className = className.substring(className.lastIndexOf(".") + 1, className.length());
               lineNumber = stackElement.getLineNumber();
               break;
            }
            else {
               className = null;
               methodName = null;
            }
         }
         else {
            className = null;
         }
      }

      String location = "No loggable package location found in stacktrace.";
      if (className != null && methodName != null) {
         location = className + "." + methodName + "(" + lineNumber + ")";
         hasLocationInfo = true;
      }

      return (location);
   }

   private static boolean includedPackage(String className) {
      boolean included = true;
      for (String testClassName : INCLUDE_PACKAGE_NAMES) {
         included = className.contains(testClassName);
         if (!included) {
            break;
         }
      }

      return included;
   }

   private static boolean excludedPackage(String className) {
      String logPackageName = LogFileFormatter.class.getPackage().getName();
      boolean exclude = className.contains(logPackageName + "."); // Exclude everything in the log package and sub-packages.

      // ClassName should not extend java.lang.Exception
      try {
         Class<?> classObj = Class.forName(className);
         do {
            if (classObj == java.lang.Exception.class) {
               exclude = true;
               break;
            }
            else {
               classObj = classObj.getSuperclass();
            }
         } while (classObj != java.lang.Object.class);
      }
      catch (ClassNotFoundException cnfex) {}

      return exclude;
   }

   private static boolean excludedClassName(String className) {
      boolean excluded = false;
      try {
         Class<?> aclass = Class.forName(className);
         while (aclass != null) {
            //      if (aclass.isInstance(Logger.class)) {
            if (aclass == Logger.class) {
               excluded = true;
               break;
            }
            aclass = aclass.getSuperclass();
         }
      }
      catch (Exception ex) {} // eat it!
      return excluded;
   }

   private static boolean excludeMethodName(String methodName) {
      boolean excluded = false;
      for (String testMethodName : EXCLUDED_METHOD_NAMES) {
         excluded = testMethodName.equalsIgnoreCase(methodName);
         if (excluded) {
            break;
         }
      }

      return excluded;
   }

   private Throwable getBaseCause(Throwable cause) {
      if (cause != null) {
         boolean endOfExceptionChain = false;
         String exMsg = cause.getMessage();
         while (!endOfExceptionChain) {

            if (!suspectedDatabaseIssue) {
               if (cause instanceof PersistenceException || cause instanceof HibernateException || cause instanceof JDBCConnectionException
                     || cause instanceof CommunicationsException) {
                  suspectedDatabaseIssue = true;
               }
            }

            if (cause.getCause() != null) {
               cause = cause.getCause();
               String causeMsg = cause.getMessage();
               if (causeMsg != null) {
                  if (!causeMsg.equals(exMsg)) {
                     exMsg = cause.getMessage();
                  }
                  else {
                     endOfExceptionChain = true;
                  }
               }
               else {
                  endOfExceptionChain = true;
               }
            }
            else {
               endOfExceptionChain = true;
            }
         }
      }

      return cause;
   }

   private String getSuspectedDatabaseIssueMessage(Throwable cause) {
      String message = "Possible issue with database or database data: " + cause.getMessage();

      Throwable cause1 = cause.getCause();
      Throwable cause2 = null;
      Throwable cause3 = null;

      if (cause1 != null) {
         cause2 = cause1.getCause();
         if (cause2 != null) {
            cause3 = cause2.getCause();
         }
      }

      if (cause instanceof JDBCConnectionException) {
         if (cause1 != null && cause1 instanceof CommunicationsException) {
            if (cause2 != null && cause2 instanceof ConnectException && cause3 == null) {
               message = "Possible database connection issue: IP or port number could be incorrect.";
            }
         }
         else if (cause1 != null && cause1 instanceof SQLException && cause2 == null) {
            message = "Possible database connection issue: username or password could be incorrect.";
         }
      }
      else if (cause instanceof SQLGrammarException) {
         if (cause1 != null && cause1 instanceof MySQLSyntaxErrorException && cause2 == null) {
            message = "Possible database connection issue: schema could be incorrect.";
         }
      }
      else if (cause2 instanceof SQLIntegrityConstraintViolationException) {
         message = "Possible database constraint violation: " + cause2.getMessage();
      }

      return message;
   }
}
