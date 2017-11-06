package com.jc.app.rest;

import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public enum Status implements StatusType {

   OK(200, "OK"),
   CREATED(201, "Created"),
   NO_CONTENT(204, "No Content"),
   NOT_MODIFIED(304, "Not Modified"),
   TEMPORARY_REDIRECT(307, "Temporary Redirect"),
   BAD_REQUEST(400, "Bad Request"),
   UNAUTHORIZED(401, "Unauthorized"),
   FORBIDDEN(403, "Forbidden"),
   NOT_FOUND(404, "Not Found"),
   GONE(410, "Gone"),
   INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
   NOT_IMPLEMENTED(501, "Not Implemented"),
   SERVICE_UNAVAILABLE(503, "Service Unavailable");

   private final int code;
   private final String reason;
   private Family family;

   Status(final int statusCode, final String reasonPhrase) {
      this.code = statusCode;
      this.reason = reasonPhrase;
      switch (code / 100) {
         case 1:
            this.family = Family.INFORMATIONAL;
            break;
         case 2:
            this.family = Family.SUCCESSFUL;
            break;
         case 3:
            this.family = Family.REDIRECTION;
            break;
         case 4:
            this.family = Family.CLIENT_ERROR;
            break;
         case 5:
            this.family = Family.SERVER_ERROR;
            break;
         default:
            this.family = Family.OTHER;
            break;
      }
   }

   /**
    * Get the class of status code
    * 
    * @return the class of status code
    */
   @Override
   public Family getFamily() {
      return family;
   }

   /**
    * Get the associated status code
    * 
    * @return the status code
    */
   @Override
   public int getStatusCode() {
      return code;
   }

   /**
    * Get the reason phrase
    * 
    * @return the reason phrase
    */
   @Override
   public String getReasonPhrase() {
      return toString();
   }

   /**
    * Get the reason phrase
    * 
    * @return the reason phrase
    */
   @Override
   public String toString() {
      return reason;
   }

   /**
    * Convert a numerical status code into the corresponding Status
    * 
    * @param statusCode
    *           the numerical status code
    * @return the matching Status or null is no matching Status is defined
    */
   public static Status fromStatusCode(final int statusCode) {
      for (Status s : Status.values()) {
         if (s.code == statusCode) {
            return s;
         }
      }
      return null;
   }
}
