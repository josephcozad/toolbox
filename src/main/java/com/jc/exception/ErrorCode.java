package com.jc.exception;

import java.util.HashMap;
import java.util.Map;

public abstract class ErrorCode {

   /*
    * DEV NOTE: The following are error code ranges currently in use...
    *      0 - 100  -- Reserved (Unused)
    *    100 - 124  -- Reserved (RESTErrorCode)
    *    125 - 149  -- Reserved (SecurityErrorCode)
    *    150 - 299  -- Reserved (DatabaseErrorCode)
    *    300 - 399  -- Reserved (AppServiceErrorCode)
    *    
    *        > 1000 -- Application specific.
    */

   private static Map<Integer, ErrorCode> ERROR_CODE_XREF = new HashMap<>();

   private int code = 0;
   private String codeDesc = "UKNOWN_ERROR_CODE";

   protected ErrorCode(int code, String codeDesc) {
      this.code = code;
      this.codeDesc = codeDesc;

      if (!ERROR_CODE_XREF.containsKey(code)) {
         ERROR_CODE_XREF.put(code, this);
      }
      else {
         System.err.println("Unable to create ErrorCode " + code + " for " + codeDesc + "; " + code + " already exists as a code value.");
         throw new IllegalArgumentException("Unable to create ErrorCode " + code + " for " + codeDesc + "; " + code + " already exists as a code value.");
      }
   }

   public int getCode() {
      return code;
   }

   public static ErrorCode fromCode(final int code) {
      ErrorCode errorCodeObj = null;
      if (ERROR_CODE_XREF.containsKey(code)) {
         errorCodeObj = ERROR_CODE_XREF.get(code);
      }
      return errorCodeObj;
   }

   @Override
   public String toString() {
      return codeDesc.toUpperCase();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + code;
      result = prime * result + ((codeDesc == null) ? 0 : codeDesc.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof ErrorCode)) {
         return false;
      }
      ErrorCode other = (ErrorCode) obj;
      if (code != other.code) {
         return false;
      }
      if (codeDesc == null) {
         if (other.codeDesc != null) {
            return false;
         }
      }
      else if (!codeDesc.equals(other.codeDesc)) {
         return false;
      }
      return true;
   }
}
