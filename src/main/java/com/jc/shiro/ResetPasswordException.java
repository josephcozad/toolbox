package com.jc.shiro;

import org.apache.shiro.authc.AuthenticationException;

public class ResetPasswordException extends AuthenticationException {

   private static final long serialVersionUID = 4155094376454004764L;

   public ResetPasswordException() {
      super();
   }

   public ResetPasswordException(String message) {
      super(message);
   }

   public ResetPasswordException(Throwable cause) {
      super(cause);
   }

   public ResetPasswordException(String message, Throwable cause) {
      super(message, cause);
   }
}
