package com.jc.db;

public class NullValueException extends Exception {

   private static final long serialVersionUID = 4262334387044814487L;

   public NullValueException() {
      super();
   }

   public NullValueException(String message) {
      super(message);
   }

   public NullValueException(String message, Throwable cause) {
      super(message, cause);
   }

   public NullValueException(Throwable cause) {
      super(cause);
   }
}
