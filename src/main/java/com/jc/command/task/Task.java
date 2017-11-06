package com.jc.command.task;

import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.jc.command.Command;
import com.jc.log.ExceptionMessageHandler;
import com.jc.log.Logger;
import com.jc.util.ConfigInfo;

/**
 * A task is a type of Command that operates in its own thread and is expected to take some "lengthy" amount of time.
 * 
 * An extending class should override the execute method in roughly the following manner:
 * 
 * <PRE>
 * public Constructor()
 * {
 *    // Setting the estimated processing time in the constructor
 *    // allows objects to find out how much time it will take to
 *    // execute before executing.
 *    setEstimatedProcessingTime(some_int);
 * }
 * 
 * public void execute()
 * {
 *    while(!isDone())
 *    {
 *       startStatusTracking(); // if you want to get updates about status and amount completed during processing.
 *       [....Do some processing....]
 *       updateAmountCompleted(amount_completed);
 *       if(status_has_changed)
 *       {
 *          updateStatusMessage(message); // Timed out or something.
 *       }
 *       stopStatusTracking();
 *    }
 *    taskDone();
 * }
 * </PRE>
 */

public abstract class Task extends Command implements Runnable {

   private static final long serialVersionUID = 453801807600686107L;

   private static ConcurrentHashMap<String, TaskRunInfo> ActiveTasks = new ConcurrentHashMap<>();

   public final static String ERRORED_TASK_STATUS_MSG = "END executing task (ERRORED): ";
   public final static String INTERRUPTED_TASK_STATUS_MSG = "END executing task (INTERRUPTED): ";
   public final static String SUCCESSFUL_TASK_STATUS_MSG = "END executing task (SUCCESSFUL): ";

   private final static Random RANDOM_GENERATOR = new Random();

   static {
      try {
         // Default max number of active tasks is set for an infinite number.
         String classname = Task.class.getName();
         TaskRunInfo info = new TaskRunInfo(classname);
         info.setMaxActiveTaskLimit(0);
         ActiveTasks.put(classname, info);
      }
      catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private Object TaskResult;
   private long EstimatedProcessingTime;
   private Vector<TaskListener> TaskListeners;
   private TaskStatus State;
   private boolean AnotherCommand;
   private final int IdNumber;
   private String IDPrefix;

   private String taskThreadId; // the id given by ThreadMonitor of the thread running this task.

   private boolean StatusTrackingOn;
   private StatusTracker Status;
   private long StatusCheckInterval;

   private long ActualRuntime;
   private int NumWorkUnits;
   private int NumWorkUnitsCompleted;

   private boolean DebugMode;

   protected Task() {
      //      IdNumber = Math.abs(ThreadLocalRandom.current().nextInt());
      IdNumber = Math.abs(RANDOM_GENERATOR.nextInt());
      StatusCheckInterval = 5000; // 5 seconds by default...
      State = TaskStatus.NOT_RUNNING;
      StatusTrackingOn = false; // Off by default...
      DebugMode = false;
   }

   public static void setMaxActiveTaskLimit(String task_classname, int max_num) {
      // Disallow the default changes for Task class.
      if (!task_classname.equals(Task.class.getName())) {
         TaskRunInfo info = getTaskRunInfo(task_classname);
         info.setMaxActiveTaskLimit(max_num);
      }
   }

   public static void setTaskLimitAsExclusive(String task_classname, boolean exclusive) {
      // Disallow the default changes for Task class.
      if (!task_classname.equals(Task.class.getName())) {
         TaskRunInfo info = getTaskRunInfo(task_classname);
         info.setExclusive(exclusive);
      }
   }

   public static void outputActiveTaskInfo() {
      for (Entry<String, TaskRunInfo> entry : ActiveTasks.entrySet()) {
         System.err.println(entry.getKey());
         System.err.println(entry.getValue().outputActiveThreads());
         System.err.println("---------------------------------------------");
      }
   }

   public static int getMaxActiveTaskLimit(Task task_obj) {
      TaskRunInfo task_info = getTaskRunInfo(task_obj);
      return (task_info.getMaxActiveTaskLimit());
   }

   private static TaskRunInfo getTaskRunInfo(String task_classname) {
      TaskRunInfo info = null;
      if (!ActiveTasks.containsKey(task_classname)) {
         info = new TaskRunInfo(task_classname);
         ActiveTasks.put(task_classname, info);
      }
      else {
         info = ActiveTasks.get(task_classname);
      }
      return info;
   }

   private static TaskRunInfo getTaskRunInfo(Task task_obj) {
      TaskRunInfo info = null;

      Class<? extends Task> class_obj = task_obj.getClass();
      String task_classname = class_obj.getName();

      boolean found = false;
      while (!found && class_obj != null) {
         String class_name = class_obj.getName();
         info = getTaskRunInfo(class_name);
         if (info.getMaxActiveTaskLimit() > -1) {
            if ((!info.isExclusive()) || (info.isExclusive() && task_classname.equals(class_name))) {
               found = true;
            }
         }

         if (!found) {
            class_obj = (Class<? extends Task>) class_obj.getSuperclass();
         }
      }
      return info;
   }

   public abstract void doTask() throws Exception;

   @Override
   public void execute() {
      // Task may not have been running when it was "interrupted" and thus be in
      // an interrupted state; if so don't execute.
      if (!isInterrupted()) {
         State = TaskStatus.WAITING;

         boolean was_waiting = false; // This is set only if the task was waiting and then interrupted while waiting.
         TaskRunInfo info = getTaskRunInfo(this);
         logMessage(Level.INFO, "Retrieved TaskRunInfo for " + info.getTaskClassName());
         logMessage(Level.INFO, "ClassName[" + getClass().getName() + "] MaxTasks[" + info.getMaxActiveTaskLimit() + "]");
         if (info.getMaxActiveTaskLimit() > 0) {
            // Limit the number of tasks of this class type that can run at any one given time.
            was_waiting = wait(info); // returns true only if waiting was interrupted.
         }
         else {
            logMessage(Level.INFO, "No thread limitation imposed on this class of Task. Good to go.");
         }

         String exception_msg = "";
         if (!isInterrupted()) {
            State = TaskStatus.RUNNING;

            updateStatusMessage("BEGIN executing task: " + getID());

            boolean done = false;
            while (!done) {
               try {
                  startStatusTracking();
                  doTask();
                  taskDone();
                  State = TaskStatus.DONE;
                  stopStatusTracking();
                  updateStatusMessage(SUCCESSFUL_TASK_STATUS_MSG + getID());
                  notifyListenersTaskDone();

                  // Execute the next task if there is one.
                  Task next_task = getNextTask();
                  if (next_task != null) {
                     next_task.start();
                  }
               }
               catch (InterruptedException e) {
                  exception_msg = e.getMessage();
                  if (exception_msg == null || !exception_msg.equalsIgnoreCase("sleep interrupted")) {
                     logMessage(Level.INFO, "Interrupted: " + exception_msg, e);
                  }
                  else {
                     exception_msg = "terminated";
                  }
                  State = TaskStatus.INTERRUPTED;
                  logMessage(Level.INFO, "State set to INTERRUPTED.");

                  stopStatusTracking();

                  if (exception_msg != null && exception_msg.length() > 0) {
                     exception_msg = " - " + exception_msg;
                  }
                  updateStatusMessage(INTERRUPTED_TASK_STATUS_MSG + getID() + exception_msg);

                  logMessage(Level.INFO, "Notify listeners task interrupted.");
                  notifyListenersTaskInterrupted();
               }
               catch (Exception e) {
                  exception_msg = e.getMessage();
                  logMessage(Level.INFO, "Exception Thrown: " + exception_msg, e);
                  State = TaskStatus.ERRORED;
                  logMessage(Level.INFO, "State set to ERRORED.");

                  if (e instanceof ConcurrentModificationException) {
                     String outfile = Logger.saveStackTrace(e);
                     String message = "ConcurrentModificationException occurred with " + getID() + ", see the outfile for more details: " + outfile;
                     Logger.log(getClass(), Level.SEVERE, message);
                  }

                  stopStatusTracking();

                  exception_msg = " - " + ExceptionMessageHandler.formatExceptionMessage(e);
                  updateStatusMessage(ERRORED_TASK_STATUS_MSG + getID() + exception_msg);

                  logMessage(Level.INFO, "Notify listeners task errored.");
                  notifyListenersTaskInterrupted();
               }
               finally {
                  done = true;
               }
            }
         }

         if (!was_waiting && info.getMaxActiveTaskLimit() > 0) {
            logMessage(Level.INFO, "Releasing resource: " + info);
            info.deactivateThread(this);
            logMessage(Level.INFO, "Done releasing resource: " + info);
         }
      }
      logMessage(Level.INFO, "End");
   }

   /**
    * Implements the Runnable interface.
    */
   @Override
   public void run() {
      execute();
   }

   /**
    * Returns the task's thread id assigned to it by the ThreadMonitor. Use this to query the ThreadMonitor about this Task.
    */
   public String getTaskThreadId() {
      return taskThreadId;
   }

   /**
    * Returns the StatusTracker's thread id assigned to it by the ThreadMonitor. Use this to query the ThreadMonitor about this Task.
    */
   public String getStatusThreadId() {
      String threadId = null;
      if (Status != null) {
         threadId = Status.getStatusThreadId();
      }
      return threadId;
   }

   /**
    * Returns the result of the task.
    */
   public Object getResult() {
      return TaskResult;
   }

   /**
    * Returns the amount of estimated time it will take to process the task.
    */
   public long getEstimatedProcessingTime() {
      return EstimatedProcessingTime;
   }

   /**
    * Indicates whether the task is done processing or not.
    */
   public boolean isDone() {
      return TaskStatus.isDone(State);
   }

   /**
    * Indicates whether the task is waiting to start running. This occurs if there are a limited number of tasks that can run at a given time.
    */
   public boolean isWaiting() {
      return TaskStatus.isWaiting(State);
   }

   /**
    * Indicates whether the task is running or not.
    */
   public boolean isRunning() {
      return TaskStatus.isRunning(State);
   }

   /**
    * Indicates whether the task was interrupted or not.
    */
   public boolean isInterrupted() {
      return TaskStatus.isInterrupted(State);
   }

   /**
    * Indicates whether the task was errored or not.
    */
   public boolean isErrored() {
      return TaskStatus.isErrored(State);
   }

   /**
    * Indicates whether the task is in initial state.
    */
   public boolean isNotRunning() {
      return TaskStatus.isNotRunning(State);
   }

   /**
    * Overrides parent's method. Also registers all listeners with the next command object.
    */
   @Override
   public void setNextCommand(Command next) {
      AnotherCommand = true;
      super.setNextCommand(next);
      if (next instanceof Task) {
         setNextTasksListeners((Task) next);
      }
   }

   /**
    * Adds a TaskListener to be notified once the task is completed.
    */
   public void addTaskListener(TaskListener listener) {
      if (TaskListeners == null) {
         TaskListeners = new Vector<>();
      }

      if (!TaskListeners.contains(listener)) {
         TaskListeners.addElement(listener);
      }

      if (AnotherCommand) {
         setNextTasksListeners(getNextTask());
      }
   }

   /**
    * Removes a TaskListener.
    */
   public void removeTaskListener(TaskListener listener) {
      if (TaskListeners != null && !TaskListeners.isEmpty() && TaskListeners.contains(listener)) {
         TaskListeners.removeElement(listener);
      }

      if (TaskListeners.isEmpty()) {
         TaskListeners = null;
      }
   }

   /**
    * Starts the task in it's own thread.
    */
   public void start() {
      taskThreadId = ThreadMonitor.startNewThread(this, Thread.NORM_PRIORITY);
      logMessage(Level.INFO, "Starting task thread[" + taskThreadId + "].");
   }

   /**
    * Interrupts the worker thread, stopping the worker from what it's doing.
    */
   public void interrupt() {
      logMessage(Level.INFO, "START Request task thread[" + taskThreadId + "] interrupt.");
      if (!TaskStatus.isNotRunning(State)) {
         ThreadMonitor.stopThread(taskThreadId);
      }

      TaskStatus prev_state = State;
      State = TaskStatus.INTERRUPTED;
      if (TaskStatus.isNotRunning(prev_state)) { // Task never ran...
         updateStatusMessage(INTERRUPTED_TASK_STATUS_MSG + getID());
      }
      logMessage(Level.INFO, "END Request task thread[" + taskThreadId + "] interrupt.");
   }

   /**
    * Returns the unique id number associated with this instance of the task.
    */
   public String getID() {
      String prefix = getIDPrefix();
      if (prefix == null || prefix.length() < 1) {
         prefix = getClass().getName() + "_";
      }
      return prefix + IdNumber;
   }

   public void setIDPrefix(String prefix) {
      IDPrefix = prefix;
   }

   public String getIDPrefix() {
      String prefix = "";
      if (IDPrefix != null && IDPrefix.length() > 0) {
         prefix = IDPrefix;
      }
      return prefix;
   }

   // ------------------------------- Protected Methods ------------------------------- //

   public TaskStatus getState() {
      return State;
   }

   // This should not be overriden.
   protected final int getIDNumber() {
      return IdNumber;
   }

   /**
    * Sets the interval amount of time that the Status will be checked.
    */
   protected void setStatusCheckInterval(long interval) {
      if (interval >= 1000) {
         StatusCheckInterval = interval;
      }
      else {
         StatusCheckInterval = 1000; // if they wanted less than a second then at least give them a second.
      }
   }

   /**
    * Returns an instance of the next task to be completed.
    */
   protected Task getNextTask() {
      Task next_task = null;
      Command next = getNextCommand();
      if (next instanceof Task) {
         next_task = (Task) next;
      }
      return next_task;
   }

   /**
    * Returns a Vector object of TaskListeners.
    */
   protected Vector<TaskListener> getTaskListeners() {
      return TaskListeners;
   }

   /**
    * Sets the estimated time it will take to process the task. Called by an extended class before execution of the task begins, usually in the constructor.
    */
   protected void setEstimatedProcessingTime(long est_proc_time) {
      EstimatedProcessingTime = est_proc_time;
   }

   /**
    * Sets the task's result. Called by an extended class after task execution.
    */
   protected void setTaskResult(Object task_result) {
      TaskResult = task_result;
   }

   /**
    * Notifies all registered TaskListeners of the status of the task in a text form. Called by an extended class during task execution.
    */
   protected void updateStatusMessage(String message) {
      logMessage(Level.INFO, message);
      Vector<TaskListener> task_listeners = getTaskListeners();
      if (task_listeners != null && !task_listeners.isEmpty()) {
         Enumeration<TaskListener> listener_list = task_listeners.elements();
         while (listener_list.hasMoreElements()) {
            TaskListener listener = listener_list.nextElement();
            listener.updateStatusMessage(getID(), message);
         }
      }
   }

   /**
    * Notifies all registered TaskListeners of the amount of the task that has been completed so far. Called by an extended class during task execution.
    */
   protected void updateAmountCompleted(double amount_completed) {
      // logMessage("AmountCompleted: " + amount_completed);
      Vector<TaskListener> task_listeners = getTaskListeners();
      if (task_listeners != null && !task_listeners.isEmpty()) {
         Enumeration<TaskListener> listener_list = task_listeners.elements();
         while (listener_list.hasMoreElements()) {
            TaskListener listener = listener_list.nextElement();
            listener.updateAmountCompleted(getID(), amount_completed);
         }
      }
   }

   /**
    * Notifies all registered TaskListeners of the amount of the estimated time to complete the task. Called by an extended class during task execution.
    */
   protected void updateEstimatedRuntime(long time) {
      // logMessage("EstimatedRuntime: " + time);
      Vector<TaskListener> task_listeners = getTaskListeners();
      if (task_listeners != null && !task_listeners.isEmpty()) {
         Enumeration<TaskListener> listener_list = task_listeners.elements();
         while (listener_list.hasMoreElements()) {
            TaskListener listener = listener_list.nextElement();
            listener.updateEstimatedRuntime(getID(), time);
         }
      }
   }

   /**
    * Override this method so that it can be called every "StatusCheckInterval" to update the task's status. This method should check the current status, and then, as appropriate, call the Task's updateAmountCompleted(), updateEstimatedRuntime(), and updateStatusMessage().
    */
   protected void updateStatus() {};

   /**
    * Notifies all registered TaskListeners that the task is done. Called by an extended class after execution of the task is completed.
    */
   protected void taskDone() {
      ThreadMonitor.stopThread(taskThreadId);
   }

   /**
    * Notifies all registered TaskListeners that the task is interrupted. Called by an extended class after execution of the task is completed.
    */
   protected void notifyListenersTaskInterrupted() {
      Vector<TaskListener> task_listeners = getTaskListeners();
      if (task_listeners != null && !task_listeners.isEmpty()) {
         Enumeration<TaskListener> listener_list = task_listeners.elements();
         while (listener_list.hasMoreElements()) {
            TaskListener listener = listener_list.nextElement();
            listener.taskInterrupted(this); // Return the reference to this object so that the listener can determine what kind of task completed and get the
            // result from the task object.
         }
      }
   }

   /**
    * Notifies all registered TaskListeners that the task is done. Called by an extended class after execution of the task is completed.
    */
   protected void notifyListenersTaskDone() {
      Vector<TaskListener> task_listeners = getTaskListeners();
      if (task_listeners != null && !task_listeners.isEmpty()) {
         Enumeration<TaskListener> listener_list = task_listeners.elements();
         while (listener_list.hasMoreElements()) {
            TaskListener listener = listener_list.nextElement();
            listener.taskDone(this); // Return the reference to this object so that the listener can determine what kind of task completed and get the result
            // from the task object.
         }
      }
   }

   /**
    * Toggles Status Tracking, on or off based on the supplied value. By default Status Tracking is off.
    */
   protected void setStatusTrackingOn(boolean on) {
      StatusTrackingOn = on;
   }

   /**
    * Indicates if Status Tracking is turned on or off.
    */
   protected boolean statusTrackingOn() {
      return StatusTrackingOn;
   }

   /**
    * Starts a thread to track the status of the Task. Should be called in the execute() method.
    */
   protected void startStatusTracking() {
      if (statusTrackingOn()) {
         updateStatusMessage("Starting status tracking.");
         Status = new StatusTracker(this, StatusCheckInterval);
      }
   }

   /**
    * Stops a thread to track the status of the Task. Should be called in the execute() method.
    */
   protected synchronized void stopStatusTracking() {
      if (statusTrackingOn()) {
         logMessage(Level.INFO, "START Request stop for status tracking.");
         if (Status != null) {
            Status.interrupt();
         }

         Status = null;
         logMessage(Level.INFO, "END Request stop for status tracking.");
         updateStatusMessage("Status tracking stopped.");
      }
   }

   protected void setNumberOfWorkUnits(int num_workunits) {
      NumWorkUnits = num_workunits;
   }

   protected void workUnitCompleted(long time_to_complete) {
      NumWorkUnitsCompleted += 1;
      ActualRuntime += time_to_complete;

      long estimated_runtime = (ActualRuntime / NumWorkUnitsCompleted) * (NumWorkUnits - NumWorkUnitsCompleted);
      updateEstimatedRuntime(estimated_runtime);

      double amount_completed = NumWorkUnitsCompleted * (1 / (double) NumWorkUnits);
      updateAmountCompleted(amount_completed);
   }

   protected boolean isDebugMode() {
      if (!DebugMode) {
         try {
            DebugMode = ConfigInfo.getInstance().hasProperty("log." + JobQueue.LOG_ID + ".directory");
         }
         catch (Exception ex) {
            Logger.log(getClass(), Level.SEVERE, ex);
         }
      }

      return DebugMode;
   }

   protected void setDebugMode(boolean on) {
      if (DebugMode != on) {
         DebugMode = on;
      }
   }

   /**
    * Outputs a "standard" formatted debugging statement that can be used in a spreadsheet.
    */
   protected void logMessage(Level level, String message) {
      logMessage(level, message, null);
   }

   protected void logMessage(Level level, String message, Throwable exc) {
      try {
         if (ConfigInfo.getInstance().hasProperty("log." + JobQueue.LOG_ID + ".directory")) {
            String statement = getLoggingStatement(message);
            if (exc != null) {
               Logger.log(JobQueue.LOG_ID, getClass(), level, statement, exc);
            }
            else {
               Logger.log(JobQueue.LOG_ID, getClass(), level, statement);
            }
         }
         else {
            //            String statement = getDebuggingStatement(message);
            //            System.out.println(statement);
            //            if (exc != null) {
            //               System.out.println(LoggableException.formatExceptionMessage(exc));
            //            }
         }
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   private String getLoggingStatement(String message) {
      // DEV NOTE: statement must have the same number of fields as the statement in same method in JobQueue.java.
      String statement = getID() + Logger.LOGFILE_FIELD_SEPARATOR + message + Logger.LOGFILE_FIELD_SEPARATOR + State;
      return statement;
   }

   // ------------------------------- Private Methods ------------------------------- //

   private void setNextTasksListeners(Task next) {
      if ((TaskListeners != null) && (next != null)) {
         Enumeration<TaskListener> listeners = TaskListeners.elements();
         while (listeners.hasMoreElements()) {
            TaskListener listener = listeners.nextElement();
            next.addTaskListener(listener);
         }
      }
   }

   private boolean wait(TaskRunInfo info) {
      boolean interrupted = false;
      synchronized (info.TaskWaiting) {
         logMessage(Level.INFO, "Before taking resource: " + info);
         info.setTaskWaiting(); // Each task is put into a waiting state...
         logMessage(Level.INFO, "After setting wait to true: " + info);
         while (true) {
            if (!info.maxActiveTasksReached()) { // Continuous test to see when the number of active tasks falls below the maximum...
               info.activateThread(this); // Task is activated and released from waiting.
               logMessage(Level.INFO, "After taking resource: " + info);
               break;
            }
         }
      }
      return (interrupted);
   }

   private class StatusTracker implements Runnable {

      private final Task MyTask;
      private final long SleepTime;
      private boolean Running;
      private final String statusThreadId; // the id given by ThreadMonitor of the thread running this task.

      private StatusTracker(Task task, long sleep_time) {
         outputDebugStatement("Sleep intervals set to: [" + sleep_time + "]");
         SleepTime = sleep_time; // 5 seconds by default...
         MyTask = task;

         String thread_name = "StatusThread_" + task.getID();
         statusThreadId = ThreadMonitor.startNewThread(this, Thread.MIN_PRIORITY + 1);
      }

      /**
       * Implements the Runnable interface.
       */
      @Override
      public void run() {
         Running = true;
         while (Running) {
            try {
               outputDebugStatement("Going to sleep.");
               Thread.sleep(SleepTime);
               if (MyTask != null && (MyTask.isNotRunning() || MyTask.isRunning())) {
                  // outputDebugStatement("Updating status.");
                  MyTask.updateStatus();
               }
               else {
                  outputDebugStatement("Task[" + MyTask.getID() + "] not running or queued; status[" + MyTask.State + "].");
                  Running = false;
               }
            }
            catch (InterruptedException ex) {
               outputDebugStatement("Interrupt Exception.");
               Running = false;
               outputDebugStatement("Setting 'running' to false.");
            }
         }
      }

      public void interrupt() {
         outputDebugStatement("START request thread interrupt.");
         ThreadMonitor.stopThread(statusThreadId);
         outputDebugStatement("END request thread interrupt.");

         if (Running) {
            Running = false;
            outputDebugStatement("Setting 'running' to false.");
         }
      }

      /**
       * Returns the StatusTracker's thread id assigned to it by the ThreadMonitor. Use this to query the ThreadMonitor about this Task.
       */
      public String getStatusThreadId() {
         return statusThreadId;
      }

      // This one is for this private class only.
      private synchronized void outputDebugStatement(String message) {
         // // long time = System.nanoTime();
         // try {
         // if (ConfigInfo.getInstance().hasProperty("log."+JobQueue.LOG_ID+".directory")) {
         // StackTraceElement[] stack = new Exception().getStackTrace();
         // int index_num = 1; // stack srace array index number.
         //
         // String classname = stack[index_num].getClassName();
         // classname = classname.substring(classname.lastIndexOf(".") + 1, classname.length());
         // String methodname = stack[index_num].getMethodName();
         // int line_number = stack[index_num].getLineNumber();
         //
         // while ((index_num < stack.length) && methodname.equals("outputDebugStatement")) {
         // index_num++;
         // classname = stack[index_num].getClassName();
         // classname = classname.substring(classname.lastIndexOf(".") + 1, classname.length());
         // methodname = stack[index_num].getMethodName();
         // line_number = stack[index_num].getLineNumber();
         // }
         //
         // TaskStatus state = TaskStatus.RUNNING;
         // if (!Running) {
         // state = TaskStatus.INTERRUPTED;
         // }
         //
         // // DEV NOTE: statement must have the same number of fields as the statement in same method in JobQueue.java.
         // // System.nanoTime()+"|"+Thread.currentThread().getName()+"|"+getID()+".StatusTracker|"+classname+"."+methodname+"("+line_number+")|"+state+"|"
         // // + message;
         // // String statement = System.nanoTime() + "|" + getID() + ".StatusTracker|" + classname + "." + methodname + "(" + line_number + ")|" + state +
         // // "|" + message;
         // String statement = message + "|" + state;
         //
         // Logger logger = Logger.getInstance(JobQueue.LOG_ID);
         // logger.log(getID() + ".StatusTracker", statement);
         // }
         // }
         // catch (Exception ex) {
         // Logger.getInstance().log(ex);
         // }
         // // System.err.println("outputDebugStatement() --> " +
         // // (System.nanoTime()-time)/1000000);
      }
   }

   public static enum TaskStatus {

      NOT_RUNNING, RUNNING, INTERRUPTED, ERRORED, DONE, WAITING, UNKNOWN;

      public static boolean isNotRunning(TaskStatus status) {
         return NOT_RUNNING.equals(status);
      }

      public boolean isNotRunning() {
         return isNotRunning(this);
      }

      public static boolean isRunning(TaskStatus status) {
         return RUNNING.equals(status);
      }

      public boolean isRunning() {
         return isRunning(this);
      }

      public static boolean isInterrupted(TaskStatus status) {
         return INTERRUPTED.equals(status);
      }

      public boolean isInterrupted() {
         return isInterrupted(this);
      }

      public static boolean isErrored(TaskStatus status) {
         return ERRORED.equals(status);
      }

      public boolean isErrored() {
         return isErrored(this);
      }

      public static boolean isDone(TaskStatus status) {
         return DONE.equals(status);
      }

      public boolean isDone() {
         return isDone(this);
      }

      public static boolean isWaiting(TaskStatus status) {
         return WAITING.equals(status);
      }

      public boolean isWaiting() {
         return isWaiting(this);
      }

      // TODO need isUnknown()?
   }
}
