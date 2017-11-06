package com.jc.command.task;

/**
 * A class that implements this interface will be notified
 * when a task is completed.
 */
public interface TaskListener {

   /**
    * When a task is done, it's listeners are notified passing
    * itself to each listener. The listeners then can use the
    * instanceof operator to determine what type of task was
    * completed and take appropriate action from there.
    */
   public void taskDone(Task a_task);

   /**
    * The amount of the task that has been completed thus far.
    */
   public void updateAmountCompleted(String task_id, double amount_completed);

   /**
    * An optional status message directly from the task.
    */
   public void updateStatusMessage(String task_id, String message);

   /**
    * The estimated amount of time left to complete the task.
    */
   public void updateEstimatedRuntime(String task_id, long time);

   /**
    * Let's the TaskListener know that the task has been interrupted.
    */
   public void taskInterrupted(Task a_task);
}
