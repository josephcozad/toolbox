package com.jc.app.rest.exceptions;

public class RESTConnectionException extends Exception {

   private static final long serialVersionUID = -1987957184200174259L;

   private final RESTErrorCode errorCode;
   private final String url;
   private final String inputParams;

   public RESTConnectionException(RESTErrorCode errorCode, String url, String inputParams) {
      super();

      this.errorCode = errorCode;
      this.url = url;
      this.inputParams = inputParams;
   }

   public String getUrl() {
      return url;
   }

   public String getInputParams() {
      return inputParams;
   }

   public boolean timedOut() {
      return errorCode.equals(RESTErrorCode.CONNECTION_TIMEOUT);
   }

   public boolean refused() {
      return errorCode.equals(RESTErrorCode.CONNECTION_REFUSED);
   }

   @Override
   public String getMessage() {
      String message = errorCode.toString() + ": url of " + url;
      if (inputParams != null) {
         message += " and inputParams of " + inputParams;
      }
      return message;
   }
}
