package com.jc.db.dao;

import javax.persistence.EntityManager;

public interface DAOTransaction {

   public static String DEFAULT_DATASOURCE = "defaultDatasource";

   public String getDataSource();

   public String getPasswordSecurityKey();

   public boolean hasEntityManager();

   public EntityManager getEntityManager();

   public abstract void startTransaction() throws Exception;

   public abstract void endTransaction() throws Exception;

   public abstract void rollBackTransaction() throws Exception;

   public abstract boolean isExternalTransactionMonitoring() throws Exception;

   public abstract Exception handleGlobalSessionException(Class<?> refClass, String message, Exception ex);

}
