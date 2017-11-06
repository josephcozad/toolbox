package com.jc.app.rest.exceptions;

import com.jc.exception.ErrorCode;

public abstract class AbstractRESTServiceException extends Exception implements RESTServiceException {

   private static final long serialVersionUID = -3134153972124257051L;

   private final ErrorCode errorCode;

   public AbstractRESTServiceException(ErrorCode errorCode) {
      this(errorCode, null);
   }

   public AbstractRESTServiceException(ErrorCode errorCode, String message) {
      super(message);
      this.errorCode = errorCode;
   }

   @Override
   public ErrorCode getErrorCode() {
      return errorCode;
   }

   @Override
   public String getMessage() {
      String message = super.getMessage();
      if (message != null && !message.isEmpty()) {
         message = errorCode.toString() + ":" + message;
      }
      else {
         message = errorCode.toString();
      }
      return message;
   }
}
