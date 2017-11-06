package com.jc.command.task;

public class TestTask extends Task {

   private static final long serialVersionUID = 4371582721633732963L;

   public final static String EXCEPTION_MESSAGE = "Something very bad happened here.";

   String Name;

   private boolean ThrowException;
   private boolean StatusTrackingStarted;
   private boolean StatusTrackingStopped;
   private long LastStatusUpdateTime;
   private long TimeB4LastStatusUpdateTime;

   private double amountCompleted;

   private TestTask MyTestTask;

   public TestTask(String name, long work) {
      Name = name;
      setEstimatedProcessingTime(work);
   }

   public void setTestTask(TestTask a_task) {
      MyTestTask = a_task;
   }

   void throwException(boolean value) {
      ThrowException = value;
   }

   long getTimeSinceLastStatusUpdate() {
      return LastStatusUpdateTime - TimeB4LastStatusUpdateTime;
   }

   boolean statusTrackingStarted() {
      return StatusTrackingStarted;
   }

   boolean statusTrackingStopped() {
      return StatusTrackingStopped;
   }

   synchronized void doSomething() {
      boolean value = true;
      while (value) {
         try {
            Thread.sleep(10000);
         }
         catch (Exception ex) {
            value = false;
         }
      }
   }

   @Override
   public void doTask() throws Exception {
      updateStatusMessage("Starting task(" + getID() + ").");

      long length_of_task = getEstimatedProcessingTime();
      long total_sleep_time = 0;
      boolean done = false;

      amountCompleted = 0;

      updateStatusMessage("Starting task(" + getID() + ").");

      while (!done) {
         long sleep_time = Math.round(Math.random() * 1000);
         Thread.sleep(sleep_time); // sleep for a second

         total_sleep_time += sleep_time;

         amountCompleted = (double) total_sleep_time / (double) length_of_task;
         updateAmountCompleted(amountCompleted);

         if (MyTestTask != null && amountCompleted > 0.25d) {
            MyTestTask.doSomething();
            MyTestTask = null; // don't repeat this once it's been done.
         }

         long est_time = (length_of_task - total_sleep_time) < 0 ? 0 : length_of_task - total_sleep_time;
         updateEstimatedRuntime(est_time);

         if (total_sleep_time >= length_of_task) {
            updateStatusMessage("Completed task.");
            setTaskResult("Total sleep time: " + total_sleep_time + " ms.");
            done = true;
         }
         else if (ThrowException && amountCompleted > 0.5) {
            throw new Exception(EXCEPTION_MESSAGE);
         }
         else {
            updateStatusMessage("Completed " + total_sleep_time + " out of " + length_of_task + ".");
         }
      }
   }

   @Override
   public String getID() {
      if (Name == null) {
         Name = super.getID();
         String prefix = super.getIDPrefix();
         if (prefix != null) {
            Name = prefix + Name;
         }
      }
      return (Name);
   }

   @Override
   protected void startStatusTracking() {
      super.startStatusTracking();
      if (statusTrackingOn()) {
         StatusTrackingStarted = true;
      }
   }

   @Override
   protected synchronized void stopStatusTracking() {
      super.stopStatusTracking();
      if (statusTrackingOn()) {
         StatusTrackingStopped = true;
      }
   }

   @Override
   protected void updateStatus() {
      TimeB4LastStatusUpdateTime = LastStatusUpdateTime;
      LastStatusUpdateTime = System.currentTimeMillis();
   }

   @Override
   protected void updateStatusMessage(String message) {
      if (message.contains(INTERRUPTED_TASK_STATUS_MSG) && amountCompleted > 0) {
         super.updateStatusMessage("Task stopped.");
      }
      else if (message.contains(ERRORED_TASK_STATUS_MSG)) {
         super.updateStatusMessage("Task errored.");
      }

      super.updateStatusMessage(message);
   }
}
