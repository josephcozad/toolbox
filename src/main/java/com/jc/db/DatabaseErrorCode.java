package com.jc.db;

import com.jc.exception.ErrorCode;

public class DatabaseErrorCode extends ErrorCode {

   /*
    * DEV NOTE: Reserved code range for this class is between 150 and 299; see ErrorCode for more info.
    */

   public final static DatabaseErrorCode ADD_DATA = new DatabaseErrorCode(150, "ADD_DATA");
   public final static DatabaseErrorCode UPDATE_DATA = new DatabaseErrorCode(151, "UPDATE_DATA");
   public final static DatabaseErrorCode REMOVE_DATA = new DatabaseErrorCode(152, "REMOVE_DATA");
   public final static DatabaseErrorCode INVALID_DATATYPE = new DatabaseErrorCode(153, "INVALID_DATATYPE");
   public final static DatabaseErrorCode EMPTY_LIST = new DatabaseErrorCode(154, "EMPTY_LIST");
   public final static DatabaseErrorCode TOO_MANY_FIELD_VALUES = new DatabaseErrorCode(155, "TOO_MANY_FIELD_VALUES");
   public final static DatabaseErrorCode NULL_FIELD_VALUE = new DatabaseErrorCode(156, "NULL_FIELD_VALUE");
   public final static DatabaseErrorCode FIELD_VALUE_NOT_ALLOWED = new DatabaseErrorCode(157, "FIELD_VALUE_NOT_ALLOWED");
   public final static DatabaseErrorCode TWO_FIELD_VALUES_REQUIRED = new DatabaseErrorCode(158, "TWO_FIELD_VALUES_REQUIRED");
   public final static DatabaseErrorCode UNSUPPORTED_FILTER_METHOD = new DatabaseErrorCode(159, "UNSUPPORTED_FILTER_METHOD");

   protected DatabaseErrorCode(int code, String codeDesc) {
      super(code, codeDesc);
   }
}
