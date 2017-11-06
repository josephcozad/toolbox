package com.jc.app.rest;

public class RESTfulResult {

   public int statusCode; // HttpURLConnection
   public String content;
   public String message;
   public String url;
   public String inputParams;

   @Override
   public String toString() {
      boolean hasContent = content != null && !content.isEmpty();
      return "STATUS[" + statusCode + "] CONTENT[" + hasContent + "] MSG[" + message + "]";
   }
}
