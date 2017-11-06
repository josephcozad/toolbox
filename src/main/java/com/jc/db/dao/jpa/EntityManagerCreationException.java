package com.jc.db.dao.jpa;

import java.util.logging.Level;

import com.jc.exception.LoggableException;

public class EntityManagerCreationException extends LoggableException {

   private static final long serialVersionUID = -4176617720285089071L;

   public EntityManagerCreationException(String datasource) {
      super(JPATransaction.class, Level.SEVERE, "Unable to create an entity manager for datasource '" + datasource + "'.");
   }
}
