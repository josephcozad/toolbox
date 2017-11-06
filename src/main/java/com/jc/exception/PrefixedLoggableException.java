package com.jc.exception;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/*
 * This type of LoggableException prepends a code to the beginning of the log message that is also
 * prepended to the beginning of the user message. The prepended code takes the form of:
 * 
 *   serviceId-appErrorCode: loggableMessage
 *   
 * User messages take the form of:
 * 
 *    System Error: (serviceId-appErrorCode): userMessage
 *    
 */

public class PrefixedLoggableException extends LoggableException {

   private static final long serialVersionUID = 8573252426032847770L;

   private static final String DEFAULT_SERVICEID = "SYS";

   private static String appErrorCode;
   private String userMessage;

   public PrefixedLoggableException(Class<?> callingClass, Level loggingLevel, String loggableMessage, String serviceId) {
      this(callingClass, loggingLevel, loggableMessage, (String) null, serviceId);
   }

   public PrefixedLoggableException(Class<?> callingClass, Level loggingLevel, String loggableMessage, String userMessage, String serviceId) {
      super(callingClass, loggingLevel, getLogMessagePrefix(serviceId) + loggableMessage);

      if (userMessage == null || userMessage.isEmpty()) {
         userMessage = "System Error: " + serviceId + "-" + appErrorCode;
      }
      else {
         userMessage = "System Error: (" + serviceId + "-" + appErrorCode + "): " + userMessage;
      }
      setUserMessage(userMessage);
   }

   public PrefixedLoggableException(Class<?> callingClass, Level loggingLevel, Throwable cause, String additionalMessage, String userMessage,
         String serviceId) {
      super(callingClass, loggingLevel, getLogMessagePrefix(serviceId), additionalMessage, cause);

      if (isSuspectedDatabaseIssue()) {
         userMessage = "Possible issue with database or database data.";
      }

      if (serviceId == null || serviceId.isEmpty()) {
         serviceId = DEFAULT_SERVICEID;
      }

      if (userMessage == null || userMessage.isEmpty()) {
         userMessage = "System Error: " + serviceId + "-" + appErrorCode;
      }
      else {
         userMessage = "System Error: (" + serviceId + "-" + appErrorCode + "): " + userMessage;
      }
      setUserMessage(userMessage);
   }

   //   @Override
   //   public String getMessage() {
   //      String message = userMessage;
   //      if (message == null || message.isEmpty()) {
   //         message = super.getMessage();
   //      }
   //      return message;
   //   }

   public void setUserMessage(String message) {
      userMessage = message;
   }

   public String getUserMessage() {
      return userMessage;
   }

   public boolean hasUserMessage() {
      return userMessage != null && !userMessage.isEmpty();
   }

   private static String getLogMessagePrefix(String serviceId) {
      Date date = new Date(); // now
      SimpleDateFormat dt = new SimpleDateFormat("yyyyMMddhhmmss");
      appErrorCode = dt.format(date);
      String prefix = serviceId + "-" + appErrorCode + ": ";
      if (serviceId == null || serviceId.isEmpty()) {
         prefix = DEFAULT_SERVICEID + "-" + appErrorCode + ": ";
      }
      return prefix;
   }
}
