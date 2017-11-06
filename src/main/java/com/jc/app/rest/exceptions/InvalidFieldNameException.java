package com.jc.app.rest.exceptions;

import com.jc.app.rest.Status;

public final class InvalidFieldNameException extends AbstractRESTServiceException {

   private static final long serialVersionUID = -7726183943561911580L;

   public InvalidFieldNameException(RESTErrorCode errorCode, String message) throws Exception {
      super(errorCode, message);
   }

   @Override
   public Status getStatus() {
      // HTTP Code for missing required data; or the content of a parameter is not correct
      return Status.BAD_REQUEST;
   }
}
