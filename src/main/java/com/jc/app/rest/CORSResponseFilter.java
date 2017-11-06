package com.jc.app.rest;

import java.io.IOException;
import java.util.logging.Level;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.jc.log.Logger;

public abstract class CORSResponseFilter implements ContainerResponseFilter {

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

      String authenticateHeaderKey = "";
      try {
         authenticateHeaderKey = getAuthenticateHeaderKey();
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }

      responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
      responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, " + authenticateHeaderKey);
      responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
   }

   public abstract String getAuthenticateHeaderKey() throws Exception;

}
