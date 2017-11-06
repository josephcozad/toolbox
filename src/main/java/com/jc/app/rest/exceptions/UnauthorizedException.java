package com.jc.app.rest.exceptions;

import java.util.logging.Level;

import com.jc.app.rest.Status;

public final class UnauthorizedException extends RESTServiceLoggableException {

   private static final long serialVersionUID = 780044739207243926L;

   private final String userName;
   private final String reasonAccessDenied;

   public UnauthorizedException(Class<?> callingClass, String userName, String reasonAccessDenied) {
      super(callingClass, Level.WARNING, userName + " was denied access during a REST resource call; reason: " + reasonAccessDenied + " .",
            RESTErrorCode.UNAUTHORIZED_ACCESS);

      setUserMessage("Access Denied");
      this.userName = userName;
      this.reasonAccessDenied = reasonAccessDenied;
   }

   @Override
   public Status getStatus() {
      return Status.FORBIDDEN;
   }

   public String getUserName() {
      return userName;
   }

   public String getReasonAccessDenied() {
      return reasonAccessDenied;
   }
}
