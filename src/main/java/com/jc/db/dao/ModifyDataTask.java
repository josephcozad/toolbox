package com.jc.db.dao;

import com.jc.command.task.Task;

public abstract class ModifyDataTask extends Task {

   private static final long serialVersionUID = -5614912673105114764L;

   private final DAOFactory daoFactory;

   public ModifyDataTask(DAOFactory daoFactory) {
      this.daoFactory = daoFactory;
   }

   @Override
   public void doTask() throws Exception {
      DAOTransaction daoTransaction = daoFactory.getDAOTransaction();

      try {

         boolean processingTransaction = daoTransaction.isExternalTransactionMonitoring(); // transaction is already started.
         if (!processingTransaction) {// start DB transaction here....
            daoTransaction.startTransaction();
         }

         modifyData(daoFactory);

         if (!processingTransaction) { // wasn't already started somewhere else...
            // end DB transaction here....
            if (Dao.ROLLBACK_ON) {
               daoTransaction.rollBackTransaction();
               transactionRolledBack();
            }
            else {
               daoTransaction.endTransaction();
               transactionEnded();
            }
         }
      }
      catch (Exception ex) {
         throw daoTransaction.handleGlobalSessionException(getClass(), "Error while processing provider info.", ex);
      }
   }

   @Override
   protected void updateStatus() {
      // TODO Auto-generated method stub
   }

   public abstract void modifyData(DAOFactory daoFactory) throws Exception;

   public void transactionRolledBack() throws Exception {

   }

   public void transactionEnded() throws Exception {

   }
}
