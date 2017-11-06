package com.jc.app.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

import com.jc.util.ConfigInfo;

public abstract class AbstractPingResource extends RESTResource {

   @GET
   @Produces(MediaType.TEXT_PLAIN)
   public Response doPing(@Context HttpServletRequest servletRequest, @Context SecurityContext sc, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
      try {
         String reply = "pong";

         MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
         String version = queryParams.getFirst("version");
         if (version != null) { // send back the version...
            reply = ConfigInfo.getInstance().getProperty("app.version");
         }

         return Response.ok(reply, MediaType.TEXT_PLAIN).build();
      }
      catch (Exception ex) {
         return createExceptionResponse(getClass(), ex);
      }
   }

   @POST
   @Produces(MediaType.APPLICATION_JSON)
   public Response doPingPost(String jsonParamStr, @Context UriInfo info) {
      try {
         JSONObject parameters = new JSONObject(jsonParamStr);
         if (parameters.has("version")) {
            String currentVersion = ConfigInfo.getInstance().getProperty("app.version");
            parameters = new JSONObject();
            parameters.put("version", currentVersion);
         }
         return Response.ok(parameters.toString(), MediaType.APPLICATION_JSON).build();
      }
      catch (Exception ex) {
         return createExceptionResponse(getClass(), ex);
      }
   }
}
