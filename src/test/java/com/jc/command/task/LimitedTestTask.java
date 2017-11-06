package com.jc.command.task;

public class LimitedTestTask extends Task {

   private static final long serialVersionUID = -7378547891475935279L;

   public final static String EXCEPTION_MESSAGE = "Something very bad happend here.";

   private String Name;

   private boolean ThrowException;
   private boolean StatusTrackingStarted;
   private boolean StatusTrackingStopped;
   private long LastStatusUpdateTime;
   private long TimeB4LastStatusUpdateTime;

   public LimitedTestTask(String name, long work) {
      Name = name;
      setEstimatedProcessingTime(work);
   }

   void throwException(boolean value) {
      ThrowException = value;
   }

   long getTimeSinceLastStatusUpdate() {
      return (LastStatusUpdateTime - TimeB4LastStatusUpdateTime);
   }

   boolean statusTrackingStarted() {
      return (StatusTrackingStarted);
   }

   boolean statusTrackingStopped() {
      return (StatusTrackingStopped);
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
         else if (ThrowException && amount > 0.5) {
            throw (new Exception(EXCEPTION_MESSAGE));
         }
         else {
            updateStatusMessage("Completed " + total_sleep_time + " out of " + length_of_task + ".");
         }
      }
   }

   // Overrides parent method...
   @Override
   public String getID() {
      if (Name == null) {
         Name = super.getID();
      }
      return (Name);
   }

   // Overrides parent method...
   @Override
   protected void startStatusTracking() {
      super.startStatusTracking();
      if (statusTrackingOn()) {
         StatusTrackingStarted = true;
      }
   }

   // Overrides parent method...
   @Override
   protected synchronized void stopStatusTracking() {
      super.stopStatusTracking();
      if (statusTrackingOn()) {
         StatusTrackingStopped = true;
      }
   }

   // Implements abstract method...
   @Override
   protected void updateStatus() {
      TimeB4LastStatusUpdateTime = LastStatusUpdateTime;
      LastStatusUpdateTime = System.currentTimeMillis();
   }
}
