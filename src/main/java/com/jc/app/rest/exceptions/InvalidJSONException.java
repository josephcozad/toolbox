package com.jc.app.rest.exceptions;

import org.json.JSONException;

import com.jc.app.rest.Status;

public final class InvalidJSONException extends AbstractRESTServiceException {

   private static final long serialVersionUID = -6678496443973311273L;

   public InvalidJSONException(JSONException jsonEx) {
      super(RESTErrorCode.INVALID_JSON_SYNTAX, jsonEx.getMessage());
   }

   @Override
   public Status getStatus() {
      // HTTP Code for missing required data; or the content of a parameter is not correct
      return Status.BAD_REQUEST;
   }
}
