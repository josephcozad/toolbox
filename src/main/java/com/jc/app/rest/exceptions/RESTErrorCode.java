package com.jc.app.rest.exceptions;

import com.jc.exception.ErrorCode;

public class RESTErrorCode extends ErrorCode {

   /*
    * DEV NOTE: Reserved code range for this class is between 100 and 124; see ErrorCode for more info.
    */

   public final static RESTErrorCode UNAUTHORIZED_ACCESS = new RESTErrorCode(100, "REST_UNAUTH_ACCESS");
   public final static RESTErrorCode INVALID_PARAMETER = new RESTErrorCode(101, "REST_INVALID_PARAM");
   public final static RESTErrorCode MISSING_REQUIRED_PARAMETER = new RESTErrorCode(102, "REST_MISSING_REQ_PARAM");
   public final static RESTErrorCode INTERNAL_SERVICE_ERROR = new RESTErrorCode(103, "REST_INTERNAL_ERROR");
   public final static RESTErrorCode CONNECTION_REFUSED = new RESTErrorCode(104, "CONNECTION_REFUSED");
   public final static RESTErrorCode CONNECTION_TIMEOUT = new RESTErrorCode(105, "CONNECTION_TIMEOUT");
   public final static RESTErrorCode INVALID_DATE_FORMAT = new RESTErrorCode(106, "INVALID_DATE_FORMAT");
   public final static RESTErrorCode INVALID_FILTER_FIELD = new RESTErrorCode(107, "INVALID_FILTER_FIELD");
   public final static RESTErrorCode INVALID_JSON_SYNTAX = new RESTErrorCode(108, "INVALID_JSON_SYNTAX");
   public final static RESTErrorCode INVALID_JSON_DATATYPE = new RESTErrorCode(109, "INVALID_JSON_DATATYPE");
   public final static RESTErrorCode INVALID_SORT_FIELD = new RESTErrorCode(110, "INVALID_SORT_FIELD");
   public final static RESTErrorCode INVALID_USE_OF_ID_FIELD = new RESTErrorCode(111, "INVALID_USE_OF_ID_FIELD");

   protected RESTErrorCode(int code, String codeDesc) {
      super(code, codeDesc);
   }
}
