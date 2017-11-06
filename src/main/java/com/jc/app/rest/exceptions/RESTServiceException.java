package com.jc.app.rest.exceptions;

import com.jc.app.rest.Status;
import com.jc.exception.ErrorCode;

public interface RESTServiceException {

   public Status getStatus();

   public ErrorCode getErrorCode();

   public String getMessage();

}
