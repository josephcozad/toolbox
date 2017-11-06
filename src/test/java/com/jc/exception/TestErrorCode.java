package com.jc.exception;

public class TestErrorCode extends ErrorCode {

   public final static int ERROR_CODE_ID = 2300;
   public final static String ERROR_CODE_DESC = "ErrorCodeTest";

   public final static TestErrorCode TEST_ERROR_CODE = new TestErrorCode(ERROR_CODE_ID, ERROR_CODE_DESC);

   protected TestErrorCode(int code, String codeDesc) {
      super(code, codeDesc);
   }

}
