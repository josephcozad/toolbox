package com.jc.exception;

import java.util.logging.Level;

public class DatabaseSessionConnectionException extends LoggableException {

   private static final long serialVersionUID = 806009499370579004L;

   public DatabaseSessionConnectionException(Class<?> callingClass) {
      super(callingClass, Level.SEVERE, "Unable to establish a database session.");
   }
}
