package com.jc.db.dao.jpa;

import java.util.logging.Level;

import com.jc.exception.LoggableException;

public class JPATransactionException extends LoggableException {

   private static final long serialVersionUID = 6745606430336178479L;

   public JPATransactionException(String datasource) {
      super(JPATransaction.class, Level.SEVERE, "No EntityManagerFactory exists for datasource '" + datasource + "'.");
   }
}
