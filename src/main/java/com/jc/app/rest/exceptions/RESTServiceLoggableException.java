package com.jc.app.rest.exceptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import com.jc.app.rest.Status;
import com.jc.exception.ErrorCode;
import com.jc.exception.LoggableException;

public class RESTServiceLoggableException extends LoggableException implements RESTServiceException {

   private static final long serialVersionUID = -8678025567255816785L;

   private static String appErrorCode;
   private String userMessage;

   public RESTServiceLoggableException(Class<?> callingClass, Level loggingLevel, String loggableMessage, ErrorCode errorCode) {
      super(callingClass, loggingLevel, getLogMessagePrefix("REST", errorCode), loggableMessage, (Throwable) null, errorCode);
      if (RESTErrorCode.INTERNAL_SERVICE_ERROR.equals(errorCode)) {
         setUserMessage("REST-" + appErrorCode);
      }
   }

   public RESTServiceLoggableException(Class<?> callingClass, Level loggingLevel, Throwable cause, ErrorCode errorCode) {
      super(callingClass, loggingLevel, getLogMessagePrefix("REST", errorCode), (String) null, cause, errorCode);
      if (RESTErrorCode.INTERNAL_SERVICE_ERROR.equals(errorCode)) {
         setUserMessage("REST-" + appErrorCode);
      }
   }

   public void setUserMessage(String message) {
      userMessage = message;
   }

   public String getUserMessage() {
      return userMessage;
   }

   public boolean hasUserMessage() {
      return userMessage != null && !userMessage.isEmpty();
   }

   @Override
   public Status getStatus() {
      // HTTP Code for missing required data; or the content of a parameter is not correct
      return Status.INTERNAL_SERVER_ERROR;
   }

   @Override
   public String getMessage() {
      ErrorCode errorCode = getErrorCode();
      String message = errorCode.toString() + ":" + super.getMessage();
      if (hasUserMessage()) {
         message = errorCode.toString() + ":" + userMessage;
      }
      return message;
   }

   private static String getLogMessagePrefix(String serviceId, ErrorCode errorCode) {
      String prefix = null;
      if (RESTErrorCode.INTERNAL_SERVICE_ERROR.equals(errorCode)) {
         Date date = new Date(); // now
         SimpleDateFormat dt = new SimpleDateFormat("yyyyMMddhhmmss");
         appErrorCode = dt.format(date);
         prefix = serviceId + "-" + appErrorCode + ": ";
      }
      return prefix;
   }
}
