package com.jc.db.dao.jdbc;

import com.jc.log.ExceptionMessageHandler;

public final class JDBCQueryException extends IllegalStateException {

   private static final long serialVersionUID = 9176989747269788853L;

   private Throwable errorException;

   private String valueClassName;
   private String setMethodName;
   private String paramTypeName;
   private String enityClassName;

   public JDBCQueryException(Throwable errorException) {
      super();
      this.errorException = errorException;
   }

   public JDBCQueryException(String valueClassName, String setMethodName, String paramTypeName, String enityClassName) {
      super();
      this.valueClassName = valueClassName;
      this.setMethodName = setMethodName;
      this.paramTypeName = paramTypeName;
      this.enityClassName = enityClassName;
   }

   @Override
   public String getMessage() {
      if (errorException != null) {
         return ExceptionMessageHandler.formatExceptionMessage(errorException);
      }
      else {
         return "Column type, " + valueClassName + ", does not match the entity's field, " + setMethodName + ", which has a type of '" + paramTypeName
               + "' for entity class " + enityClassName + ".";
      }
   }
}
