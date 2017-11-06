package com.jc.log.audit;

import java.util.logging.Level;

import com.jc.log.LogDatasource;
import com.jc.log.Logger;

public class AuditLogDatasource extends LogDatasource {

   @Override
   public void persist() {
      try {
         String message = getMessage();

         AuditLogger logger = AuditLogger.getInstance();
         logger.persistAuditLogMessage(message);
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   @Override
   public void close() {
      // intentionally does nothing...
   }
}
