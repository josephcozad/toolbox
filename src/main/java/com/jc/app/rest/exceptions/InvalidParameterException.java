package com.jc.app.rest.exceptions;

import java.util.ArrayList;
import java.util.List;

import com.jc.app.rest.Status;
import com.jc.exception.ErrorCode;

public final class InvalidParameterException extends AbstractRESTServiceException {

   private static final long serialVersionUID = -310847154137245487L;

   private List<String> parameterList;

   public InvalidParameterException(ErrorCode errorCode) {
      super(errorCode);
   }

   public InvalidParameterException(ErrorCode errorCode, String message) {
      super(errorCode, message);
      this.parameterList = new ArrayList<>();
   }

   @Override
   public Status getStatus() {
      // HTTP Code for missing required data; or the content of a parameter is not correct
      return Status.BAD_REQUEST;
   }

   public void addParameter(String parameterName) {
      if (parameterList == null) {
         this.parameterList = new ArrayList<>();
      }
      this.parameterList.add(parameterName);
   }

   public void addParameters(List<String> parameterList) {
      if (parameterList == null) {
         this.parameterList = new ArrayList<>();
      }
      this.parameterList.addAll(parameterList);
   }

   public List<String> getParameterList() {
      return parameterList;
   }
}
