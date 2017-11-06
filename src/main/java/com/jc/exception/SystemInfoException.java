package com.jc.exception;

import java.util.logging.Level;

/*
 * This class is intended to be used to throw an application generated exception 
 * that does not need to be logged, but that needs to be reported back to the 
 * user.
 */

public final class SystemInfoException extends Exception {

   private static final long serialVersionUID = -1084410585809063383L;

   private String userMessage;
   private ErrorCode errorCode;
   private Level loggingLevel = Level.SEVERE;

   public SystemInfoException(Level loggingLevel, ErrorCode errorCode) {
      this(loggingLevel, errorCode, (String) null);
   }

   public SystemInfoException(Level loggingLevel, ErrorCode errorCode, String userMessage) {
      this.errorCode = errorCode;
      this.userMessage = userMessage;
      this.loggingLevel = loggingLevel;
   }

   public Level getSeverityLevel() {
      return (loggingLevel);
   }

   public void setUserMessage(String message) {
      userMessage = message;
   }

   public String getUserMessage() {
      return userMessage;
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

   @Override
   public String getMessage() {
      return userMessage;
   }
}
