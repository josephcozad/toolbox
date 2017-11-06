package com.jc.log.audit;

public class AuditInfo {

   public final static String AUDITLOG_LOGID = "audit";

   private Class<?> referringClass;
   private String action;
   private String comment;

   public Class<?> getReferringClass() {
      return referringClass;
   }

   public void setReferringClass(Class<?> referringClass) {
      this.referringClass = referringClass;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public String getComment() {
      return comment;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public boolean hasComment() {
      return comment != null && !comment.isEmpty();
   }
}
