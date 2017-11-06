package com.jc.log;

public abstract class LogDatasource {

   private String nanoSecs;
   private String dateStamp;
   private String logLevel;
   private String classFieldInfo;
   private String message;

   public abstract void persist();

   public abstract void close();

   public void setNanoSecs(String nanoSecs) {
      this.nanoSecs = nanoSecs;
   }

   public String getNanoSecs() {
      return nanoSecs;
   }

   public void setDateStamp(String dateStamp) {
      this.dateStamp = dateStamp;
   }

   public String getDateStamp() {
      return dateStamp;
   }

   public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
   }

   public String getLogLevel() {
      return logLevel;
   }

   public void setClassFieldInfo(String classFieldInfo) {
      this.classFieldInfo = classFieldInfo;
   }

   public String getClassFieldInfo() {
      return classFieldInfo;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getMessage() {
      return message;
   }
}
