package com.jc.log.audit;

import java.util.List;

public interface AuditableUser {

   public List<AuditInfo> getAuditInfo();

   public void addAuditInfo(AuditInfo auditInfo);

   public void clearAuditInfo();

}
