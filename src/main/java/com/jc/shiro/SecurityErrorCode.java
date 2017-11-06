package com.jc.shiro;

import com.jc.exception.ErrorCode;

public class SecurityErrorCode extends ErrorCode {

   /*
    * DEV NOTE: Reserved code range for this class is between 125 and 149; see ErrorCode for more info.
    */

   public final static SecurityErrorCode INVALID_CREDENTIALS = new SecurityErrorCode(125, "INVALID_CREDENTIALS");
   public final static SecurityErrorCode UNKNOWN_USER_ACCOUNT = new SecurityErrorCode(126, "UNKNOWN_USER_ACCOUNT");
   public final static SecurityErrorCode USER_ACCOUNT_LOCKED = new SecurityErrorCode(127, "USER_ACCOUNT_LOCKED");
   public final static SecurityErrorCode INVALID_PASSWORD = new SecurityErrorCode(128, "INVALID_PASSWORD");
   public final static SecurityErrorCode INVALID_SESSION = new SecurityErrorCode(129, "INVALID_SESSION");

   protected SecurityErrorCode(int code, String codeDesc) {
      super(code, codeDesc);
   }
}
