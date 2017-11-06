package com.jc.db.dao;

import com.jc.db.DatabaseErrorCode;

public class InvalidFilterMethodException extends Exception {

   private static final long serialVersionUID = 6984149315188328023L;

   private final DatabaseErrorCode errorCode;

   public InvalidFilterMethodException(DatabaseErrorCode errorCode) {
      this(errorCode, null);
   }

   public InvalidFilterMethodException(DatabaseErrorCode errorCode, String message) {
      super(message);
      this.errorCode = errorCode;
   }

   public DatabaseErrorCode getErrorCode() {
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
