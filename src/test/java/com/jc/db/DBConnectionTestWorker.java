package com.jc.db;

import com.jc.command.task.Task;

public class DBConnectionTestWorker extends Task {

   private static final long serialVersionUID = 6795678475119150373L;

   private static boolean DEADLOCK_TEST;

   private final String Prefix;
   private String Name;
   private String StatusMessage;

   public DBConnectionTestWorker(String name, long work) {
      Prefix = name;
      setEstimatedProcessingTime(work);
      DEADLOCK_TEST = false;
   }

   public static void turnOnDeadlockTesting() {
      DEADLOCK_TEST = true;
   }

   public static void releaseAllWorkers() {
      DEADLOCK_TEST = false;
   }

   @Override
   public void doTask() throws Exception {
      StatusMessage = "Working....";

      long length_of_task = getEstimatedProcessingTime();

      Object[] inParams = new Object[] {
            length_of_task
      };

      int[] outParams = new int[] {
      //       DBConnection.ORACLE_TYPE_CURSOR
      };

      if (!DEADLOCK_TEST) {
         DBConnection conn = DBConnection.getInstance();
         Name = conn.getID();
         DBResult result = conn.executeStoredProcedure("RUN_FOR_MILLISECS", inParams, outParams);
         if (!result.hasErrors()) {
            // get result here....
         }
         else {
            throw (new Exception(result.getErrorMessage()));
         }
      }

      StatusMessage = "Completed task.";
   }

   public String getStatusMessage() {
      return (StatusMessage);
   }

   @Override
   public String getID() {
      return (Prefix + "." + Name);
   }

   @Override
   protected void updateStatus() {}
}
