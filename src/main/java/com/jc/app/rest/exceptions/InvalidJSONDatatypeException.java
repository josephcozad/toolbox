package com.jc.app.rest.exceptions;

import com.jc.app.rest.Status;

public final class InvalidJSONDatatypeException extends AbstractRESTServiceException {

   private static final long serialVersionUID = -3596274751493696629L;

   public InvalidJSONDatatypeException(String message) {
      super(RESTErrorCode.INVALID_JSON_DATATYPE, message);
   }

   @Override
   public Status getStatus() {
      // HTTP Code for missing required data; or the content of a parameter is not correct
      return Status.BAD_REQUEST;
   }
}
