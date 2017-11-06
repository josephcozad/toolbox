package com.jc.command.task;

public class LongTask extends Task {

   private static final long serialVersionUID = 6152896623324546857L;

   private String Name;

   public LongTask(String name, long work) {
      Name = name;
      setEstimatedProcessingTime(work);
   }

   // overrides parent
   @Override
   public String getID() {
      if (Name == null) {
         Name = super.getID();
      }
      return (Name);
   }

   @Override
   public void doTask() throws Exception {
      long length_of_task = getEstimatedProcessingTime();
      long total_sleep_time = 0;
      boolean done = false;
      while (!done) {
         long sleep_time = Math.round(Math.random() * 1000);
         Thread.sleep(sleep_time); //sleep for a second

         total_sleep_time += sleep_time;

         double amount = (double) total_sleep_time / (double) length_of_task;
         updateAmountCompleted(amount);

         long est_time = (length_of_task - total_sleep_time) < 0 ? 0 : length_of_task - total_sleep_time;
         updateEstimatedRuntime(est_time);

         if (total_sleep_time >= length_of_task) {
            updateStatusMessage("Completed task.");
            setTaskResult("Total sleep time: " + total_sleep_time + " ms.");
            done = true;
         }
         else {
            updateStatusMessage("Completed " + total_sleep_time + " out of " + length_of_task + ".");
         }
      }
   }

   @Override
   protected void updateStatus() {}
}
