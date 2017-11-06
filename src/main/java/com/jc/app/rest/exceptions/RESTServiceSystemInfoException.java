package com.jc.app.rest.exceptions;

import com.jc.app.rest.Status;
import com.jc.exception.SystemInfoException;

public class RESTServiceSystemInfoException extends AbstractRESTServiceException {

   private static final long serialVersionUID = -1837124195243294765L;

   private final SystemInfoException siex;

   public RESTServiceSystemInfoException(SystemInfoException siex) {
      super(siex.getErrorCode(), siex.getUserMessage());
      this.siex = siex;
   }

   @Override
   public Status getStatus() {
      return Status.BAD_REQUEST;
   }
}
