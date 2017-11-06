package com.jc.app.service;

import com.jc.exception.ErrorCode;

public class AppServiceErrorCode extends ErrorCode {

   /*
    * DEV NOTE: Reserved code range for this class is between 300 and 399; see ErrorCode for more info.
    */

   public final static AppServiceErrorCode INVALID_FROM_DATE = new AppServiceErrorCode(300, "INVALID_FROM_DATE"); // from date cannot be before to date...
   public final static AppServiceErrorCode INVALID_DATE_FORMAT = new AppServiceErrorCode(301, "INVALID_DATE_FORMAT"); // date format supplied invalid...

   protected AppServiceErrorCode(int code, String codeDesc) {
      super(code, codeDesc);
   }
}
