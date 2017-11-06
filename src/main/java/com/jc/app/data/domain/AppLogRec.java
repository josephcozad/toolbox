package com.jc.app.data.domain;

public class AppLogRec {

   private String nanoSecs;
   private String datestamp;
   private String loglevel;
   private String classFieldInfo;
   private String message;

   public AppLogRec() {}

   public String getNanoSecs() {
      return nanoSecs;
   }

   public void setNanoSecs(String nanoSecs) {
      this.nanoSecs = nanoSecs;
   }

   public String getDatestamp() {
      return datestamp;
   }

   public void setDatestamp(String datestamp) {
      this.datestamp = datestamp;
   }

   public String getLoglevel() {
      return loglevel;
   }

   public void setLoglevel(String loglevel) {
      this.loglevel = loglevel;
   }

   public String getClassFieldInfo() {
      return classFieldInfo;
   }

   public void setClassFieldInfo(String classFieldInfo) {
      this.classFieldInfo = classFieldInfo;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}
