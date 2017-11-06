package com.jc.command.task;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import com.jc.log.Logger;
import com.jc.util.FileSystem;
import com.jc.util.StringUtils;

/**
 * A Job is a collection of one or more Task objects that are run synchronously, one after the other.
 */

public class Job implements TaskListener, JobStatusListener, Runnable {

   public final static String JOB_DONE_MESSAGE = "done";

   //   private final static Random RANDOM_GENERATOR = new Random();

   private String IdNumber;
   private String IDPrefix; // A string value that can be set to "customize" the ID.

   private Long StatusTimeStamp; // The time the job entered the current status.

   private String jobThreadId; // the id given by ThreadMonitor of the thread running this job.

   private double AmountCompleted;
   private String StatusMessage;
   private LinkedHashMap<String, JobTaskInfo> Tasks;
   private String RootTaskID;
   private int TasksDone;
   private float Priority;

   private JobStatus State;

   private Vector<JobStatusListener> Listeners;

   private final boolean MultiThreaded; // Indicates whether tasks are run asynchronously or not; default is false.
   private int NumThreads;

   private LinkedHashMap<String, StringBuffer> Logs;

   private boolean SlaveMode;
   private Job[] MyMasters;
   private boolean RequireMastersFinish; // Indicates that the Masters must finish successfully to take action.
   private boolean MastersPartiallyDoneOk; // At least one finished without error or being stopped.
   private boolean MastersDoneOk; // All finished successfully without error.

   public Job(Task task, boolean multi_threaded) {

      UUID uuid = UUID.randomUUID();
      IdNumber = uuid.toString();
      IdNumber = IdNumber.replaceAll("-", "");
      IdNumber = IdNumber.substring(0, 15);

      State = JobStatus.UNKNOWN;

      MultiThreaded = multi_threaded;

      Tasks = new LinkedHashMap<>();

      String task_id = task.getID();
      if (!MultiThreaded) {
         RootTaskID = task_id;
      }

      JobTaskInfo info = new JobTaskInfo(task);
      Tasks.put(task.getID(), info);
      task.addTaskListener(this);

      logMessage(Level.INFO, "Queued task[" + task_id + "].");

      Priority = 1.0f; // Default priority.

      Logs = new LinkedHashMap<>();
      Logs.put(task_id, new StringBuffer());

      SlaveMode = false;
   }

   public Job(Task task, Job[] masters, boolean asynchronous) {
      this(task, asynchronous);

      if (masters != null && masters.length > 0) {
         RequireMastersFinish = true;
         MastersDoneOk = false;
         MastersPartiallyDoneOk = false;

         Vector<Job> avec = new Vector<>();
         for (Job a_job : masters) {
            if (a_job != null && !a_job.equals(this)) {
               a_job.addJobStatusListener(this);
               avec.add(a_job);
            }
         }

         int num_masters = avec.size();
         if (num_masters > 0) {
            MyMasters = new Job[num_masters];
            avec.copyInto(MyMasters);
         }
         else {
            throw (new RuntimeException("ERROR: A Slave object must have at least one 'master' job on which it depends."));
         }

         SlaveMode = true;
      }
   }

   public void setIDPrefix(String prefix) {
      IDPrefix = prefix;
   }

   public String getIDPrefix() {
      String prefix = "";
      if (IDPrefix != null && IDPrefix.length() > 0) {
         prefix = IDPrefix;
      }
      return (prefix);
   }

   /**
    * Returns the job's thread id assigned to it by the ThreadMonitor. Use this to query the ThreadMonitor about this Job.
    */
   public String getJobThreadId() {
      return jobThreadId;
   }

   public String getLogData() {
      StringBuffer sb = new StringBuffer();
      Object[] keys = Logs.keySet().toArray();
      for (Object key : keys) {
         String log = getLogData((String) key);
         if (log.length() > 0) {
            sb.append(log + FileSystem.NEWLINE);
         }
      }
      return (sb.toString());
   }

   public String getLogData(String taskid) {
      String log_data = "";
      if (Logs.containsKey(taskid)) {
         StringBuffer sb = Logs.get(taskid);
         log_data = sb.toString();
      }
      return (log_data);
   }

   public Vector<Object> getResult() {
      Vector<Object> results = null;
      Object[] keys = Tasks.keySet().toArray();
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         if (info.getTaskObject().isDone() || info.getTaskObject().isErrored() || info.getTaskObject().isInterrupted()) {
            Object task_result = info.getTaskObject().getResult();
            if (results == null) {
               results = new Vector<>();
            }
            results.add(task_result);
         }
      }
      return (results);
   }

   public double getAmountCompleted() {
      return AmountCompleted;
   }

   public String getStatusMessage() {
      String message = StatusMessage;
      if (message == null && slaveMode()) {
         if (isQueued() || isWaiting()) {
            message = "Waiting for dependent jobs to complete before running.";
         }
         else if (isStopped()) {
            message = "Stopped.";
         }
      }
      return (message);
   }

   public long getEstimatedRuntime() {
      long estimated_runtime = 0;
      Object[] keys = Tasks.keySet().toArray();
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         estimated_runtime += info.getEstimatedRuntime();
      }
      return (estimated_runtime);
   }

   public boolean isDone() {
      return (getState().isDone());
   }

   public boolean isStopped() {
      return (getState().isStopped());
   }

   public boolean isErrored() {
      return (getState().isErrored());
   }

   public boolean isRunning() {
      return (getState().isRunning());
   }

   public boolean isQueued() {
      return (getState().isQueued());
   }

   public boolean isWaiting() {
      return (getState().isWaiting());
   }

   public void start() {
      if (slaveMode()) {
         logMessage(Level.INFO, "Start slave thread.");
         jobThreadId = ThreadMonitor.startNewThread(this, Thread.NORM_PRIORITY - 2);
         logMessage(Level.INFO, "Slave thread[" + jobThreadId + "] started.");
         setState(JobStatus.WAITING);
      }
      else {
         if (State.equals(JobStatus.UNKNOWN)) {
            setState(JobStatus.QUEUED);
         }
         startTasks();
      }
   }

   public void stop() {
      new JobStopManager(this);
   }

   public void addTask(Task task) {
      if (!MultiThreaded) {
         JobTaskInfo root_task_info = Tasks.get(RootTaskID);
         root_task_info.getTaskObject().setNextCommand(task);
      }

      task.addTaskListener(this);

      String task_id = task.getID();
      JobTaskInfo info = new JobTaskInfo(task);
      Tasks.put(task_id, info);

      logMessage(Level.INFO, "Queued task[" + task_id + "].");

      Logs.put(task_id, new StringBuffer());
   }

   public void setPriority(float priority) {
      Priority = priority;
   }

   public String getID() {
      String prefix = getIDPrefix();
      if (prefix == null || prefix.length() < 1) {
         String classname = getClass().getName();
         classname = classname.substring(classname.lastIndexOf(".") + 1, classname.length());
         prefix = classname + "_";
      }
      return (prefix + IdNumber);
   }

   /**
    * Adds a JobStatusListener to be notified of a change in job state.
    */
   public void addJobStatusListener(JobStatusListener listener) {
      if (Listeners == null) {
         Listeners = new Vector<>();
      }

      if (!Listeners.contains(listener)) {
         Listeners.addElement(listener);
      }
   }

   /**
    * Removes a JobStatusListener.
    */
   public void removeJobStatusListener(JobStatusListener listener) {
      if (Listeners != null && !Listeners.isEmpty() && Listeners.contains(listener)) {
         Listeners.removeElement(listener);
      }

      if (Listeners.isEmpty()) {
         Listeners = null;
      }
   }

   @Override
   public String toString() {
      return "Job [" + getID() + ": " + getState() + "]";
   }

   public void setMaxNumThreads(int max_threads) {
      NumThreads = max_threads;
   }

   public LinkedHashMap<String, JobTaskInfo> getTasks() {
      return (Tasks);
   }

   public JobStatus getState() {
      return (State);
   }

   public long getStatusTimeStamp() {
      return StatusTimeStamp;
   }

   // ---------------------------- Protected Methods ---------------------------- //

   protected void setState(JobStatus new_state) {
      JobStatus old_state = State;
      State = new_state;
      StatusTimeStamp = System.currentTimeMillis();
      notifyListenersStatusChanged(getID(), old_state, new_state);

      if (slaveMode()) {
         // Make sure we stop this thread when we are done.
         if (isDone() || isErrored() || isStopped()) {
            stop();
         }
      }
   }

   protected float getPriority() {
      return (Priority);
   }

   /**
    * Outputs a "standard" formatted debugging statement that can be used in a spreadsheet.
    */
   protected void logMessage(Level level, String message) {
      logMessage(level, message, null);
   }

   protected void logMessage(Level level, String message, Throwable exc) {
      try {
         //         if (ConfigInfo.getInstance().hasProperty("log." + JobQueue.LOG_ID + ".directory")) {
         String statement = getLoggingStatement(message);
         if (exc != null) {
            Logger.log(JobQueue.LOG_ID, getClass(), level, statement, exc);
         }
         else {
            Logger.log(JobQueue.LOG_ID, getClass(), level, statement);
         }
         //         }
         //         else {
         //            //            String statement = getDebuggingStatement(message);
         //            //            System.out.println(statement);
         //            //            if (exc != null) {
         //            //               System.out.println(LoggableException.formatExceptionMessage(exc));
         //            //            }
         //         }
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

   // ---------------------------- Package Only Methods ---------------------------- //

   Job[] getMasters() {
      return (MyMasters);
   }

   void cleanUp() {
      Object[] keys = Tasks.keySet().toArray();
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         Task task = info.getTaskObject();
         if (task.isRunning()) {
            task.interrupt();
         }
      }
      Tasks.clear();
      Tasks = null;

      Logs.clear();
      Logs = null;

      Listeners.clear();
      Listeners = null;
   }

   // ---------------------------- Private Methods ---------------------------- //

   private void startTasks() {
      cacluateTaskWeights();
      if (!MultiThreaded) {
         NumThreads = 1;
         logMessage(Level.INFO, "Operating in single threaded mode.");
         logMessage(Level.INFO, "Job will use " + NumThreads + " threads to complete " + Tasks.size() + " tasks.");
         JobTaskInfo info = Tasks.get(RootTaskID);
         Task task = info.getTaskObject();
         logMessage(Level.INFO, "Starting job task[" + task.getID() + "].");
         task.start();
         setState(JobStatus.RUNNING);
      }
      else {
         logMessage(Level.INFO, "Operating in multi-threaded mode.");
         if (NumThreads == 1) {
            // Error... can't run in MultiThreaded mode with only one thread.
         }
         else if (NumThreads <= 0) {
            logMessage(Level.INFO, "NumThreads has not been set, set to number of tasks [" + Tasks.size() + "].");
            NumThreads = Tasks.size();
         }
         else {
            int num_tasks = Tasks.size();
            if (NumThreads > num_tasks) {
               logMessage(Level.INFO, "NumThreads[" + NumThreads + "] set greater than number of tasks, set to number of tasks [" + num_tasks + "].");
               NumThreads = num_tasks;
            }
         }
         logMessage(Level.INFO, "Job will use " + NumThreads + " threads to complete tasks.");

         for (int i = 0; i < NumThreads; i++) {
            startNextAsyncTask();
         }

         // Before setting this to run, check that the job is still queued or waiting. Some task,
         // may have errored by the time all the tasks are started and that error may have caused
         // the Job's state to have changed from Queued or Waiting to something else.
         if (State.isQueued() || State.isWaiting()) {
            setState(JobStatus.RUNNING);
         }
      }
   }

   private void notifyListenersStatusChanged(String job_id, JobStatus old_status, JobStatus new_status) {
      if (Listeners != null && !Listeners.isEmpty()) {
         Enumeration<JobStatusListener> listener_list = Listeners.elements();
         while (listener_list.hasMoreElements()) {
            JobStatusListener listener = listener_list.nextElement();
            listener.jobStatusChanged(job_id, old_status, new_status);
         }
      }
   }

   private void interrupt(JobStatus state) {
      logMessage(Level.INFO, "JOB CUR_STATE[" + getState() + "] NEW_STATE[" + state + "]");

      Object[] keys = Tasks.keySet().toArray();
      logMessage(Level.INFO, "Number of task in job: " + Tasks.size());

      // Cancel all the waiting and not started tasks first.
      logMessage(Level.INFO, "Interrupting all the waiting and not started tasks first...");
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         Task task = info.getTaskObject();
         logMessage(Level.INFO, task.getID() + " task state is [" + task.getState() + "].");
         if (task.isNotRunning() || task.isWaiting()) {
            long time = System.currentTimeMillis();
            logMessage(Level.INFO, "START Request stop for task[" + task.getID() + "]");
            task.interrupt();
            logMessage(Level.INFO, "END Request stop for task[" + task.getID() + "] total time taken [" + (System.currentTimeMillis() - time) + "]");
         }
      }

      // Then cancel all the running ones.
      logMessage(Level.INFO, "Interrupting all the running tasks.");
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         Task task = info.getTaskObject();
         logMessage(Level.INFO, task.getID() + " task state is [" + task.getState() + "].");
         if (task.isRunning()) {
            long time = System.currentTimeMillis();
            logMessage(Level.INFO, "START Request stop for task[" + task.getID() + "]");
            task.interrupt();
            logMessage(Level.INFO, "END Request stop for task[" + task.getID() + "] total time taken [" + (System.currentTimeMillis() - time) + "]");
         }
      }

      // Double check that all are stopped and log any that aren't.
      logMessage(Level.INFO, "Make sure all tasks are stopped or done.");
      boolean all_stopped = true;
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         Task task = info.getTaskObject();
         if (task.isRunning() || task.isNotRunning() || task.isWaiting()) {
            logMessage(Level.INFO, task.getID() + " task STILL NOT STOPPED, state is [" + task.getState() + "].");
            all_stopped = false;
         }
      }

      if (all_stopped) {
         logMessage(Level.INFO, "All tasks are stopped or done.");
      }
      else {
         logMessage(Level.WARNING, "NOT all tasks were stopped or done.");
         // System.out.println("WARNING: NOT all tasks were stopped or done.");
         // Task.outputActiveTaskInfo();
      }

      setState(state);
   }

   private void cacluateTaskWeights() {
      long estimated_runtime = getEstimatedRuntime(); // This is the estimated job time.
      Object[] keys = Tasks.keySet().toArray();
      for (Object key : keys) {
         JobTaskInfo info = Tasks.get(key);
         if (estimated_runtime > 0) {
            long task_esttime = info.getEstimatedRuntime();
            if (task_esttime > 0) {
               info.calculateWeight(estimated_runtime);
            }
            else {
               double weight = (double) 1 / (double) Tasks.size();
               info.setWeight(weight);
            }
         }
         else {
            double weight = (double) 1 / (double) Tasks.size();
            info.setWeight(weight);
         }
      }
   }

   // ------------------- Supporting Methods To Run Jobs Asynchronously ------------------- //

   private void startNextAsyncTask() {
      boolean found = false;
      Object[] keys = Tasks.keySet().toArray();
      for (int i = 0; i < keys.length && !found; i++) {
         JobTaskInfo info = Tasks.get(keys[i]);
         Task task = info.getTaskObject();
         if (task.isNotRunning()) {
            found = true;
            logMessage(Level.INFO, "Starting job task[" + task.getID() + "].");
            task.start();
            while (task.isNotRunning()) {
               logMessage(Level.INFO, "Waiting for job task[" + task.getID() + "] to 'start'; task state [" + task.getState() + "].");
            }
         }
      }
   }

   // ------------------- Supporting Methods To Run Jobs in Slave Mode ------------------- //

   public boolean slaveMode() {
      return (SlaveMode);
   }

   public void requireMastersFinish(boolean valid) {
      RequireMastersFinish = valid;
   }

   // Masters must all finish, come to a done state, before an action can take place, otherwise will stop.
   public boolean mastersMustFinish() {
      return (RequireMastersFinish);
   }

   /**
    * Implements the Runnable interface. NOTE: THIS METHOD SHOULD NEVER BE CALLED DIRECTLY, ONLY CALLED WHEN IN SLAVE MODE BY THE start() METHOD.
    */
   @Override
   public void run() {
      if (slaveMode()) {
         boolean done = false;
         while (!done) {
            if (mastersDone()) {
               logMessage(Level.INFO, "Masters done...");
               if (mastersMustFinish()) { // All the masters must finish successfully, none can be stopped or errored.
                  if (MastersDoneOk) {
                     logMessage(Level.INFO, "Masters must finish successfully, and they have.");
                     done = true;
                     logMessage(Level.INFO, "Starting tasks...");
                     startTasks();
                  }
                  else { // If they all finished but one or more were stopped or errored, then don't proceed.
                     logMessage(Level.INFO, "Masters must finish successfully, and they did not.");
                     stop();
                     done = true;
                  }
               }
               else if (MastersPartiallyDoneOk || MastersDoneOk) { // Doesn't matter how the masters finish, just as long as they are not running or queued.
                  logMessage(Level.INFO, "Masters DO NOT have to all finish successfully, and haven't.");
                  done = true;
                  logMessage(Level.INFO, "Starting tasks...");
                  startTasks();
               }
               else {
                  logMessage(Level.INFO, "(MastersPartiallyDoneOk) Masters DID NOT complete successfully...");
                  stop();
                  done = true;
               }
            }
            else {
               long time = System.currentTimeMillis();
               try {
                  long sleep_time = calcSleepTime(); // calculates the amount of time left for the masters to finish.
                  logMessage(Level.INFO, "Masters NOT done... sleep for [" + sleep_time + "]ms");
                  Thread.sleep(sleep_time);
                  logMessage(Level.INFO, "Woke up while waiting for masters to complete... total sleep time [" + (System.currentTimeMillis() - time) + "]ms.");
               }
               catch (InterruptedException e) {
                  logMessage(Level.INFO,
                        "Interrupted: Woke up while waiting for masters to complete... total sleep time [" + (System.currentTimeMillis() - time) + "]ms.");
                  // do nothing
               }
            }
         }
      }
      else {
         throw new RuntimeException("A Job object can only be 'run' when in slave mode using the Job constructor that takes an array of 'master' Job objects.");
      }
   }

   // Checks to see if all the masters are done.
   boolean mastersDone() {
      boolean done = false;

      int num_done = 0;
      int num_errored = 0;
      int num_stopped = 0;
      int num_queued = 0;
      int num_running = 0;
      int num_waiting = 0;
      int num_unknown = 0;

      // System.err.println("---------------------------------------------------");

      for (Job myMaster : MyMasters) {
         // logMessage("MASTER["+MyMasters[i].getID()+"] MASTER_STATE["+Job.translateStateCode(MyMasters[i].State)+"]"); //SLAVE["+getID()+"]
         if (myMaster.isDone()) {
            num_done += 1;
         }
         else if (myMaster.isStopped()) {
            num_stopped += 1;
         }
         else if (myMaster.isErrored()) {
            num_errored += 1;
         }
         else if (myMaster.isQueued()) {
            num_queued += 1;
         }
         else if (myMaster.isRunning()) {
            num_running += 1;
         }
         else if (myMaster.isWaiting()) {
            num_waiting += 1;
         }
         else if (myMaster.State.equals(JobStatus.UNKNOWN)) {
            num_unknown += 1;
         }
      }

      String masters_state = "RUNNING";

      if ((num_done + num_stopped + num_errored) == MyMasters.length) {
         done = true;
         if (num_done == MyMasters.length) {
            MastersDoneOk = true;
            masters_state = "ALL DONE";
         }
         else if (num_done < MyMasters.length && num_done > 0) {
            MastersPartiallyDoneOk = true;
            masters_state = "PARTIAL DONE";
         }
      }
      // SLAVE["+getID()+"]
      // logMessage("     #MASTERS["+MyMasters.length+"] STATE["+masters_state+"] QUEUED["+num_queued+"] RUNNING["+num_running+"] WAITING["+num_waiting+"] UKNONWN["+num_unknown+"] DONE["+num_done+"] STOPPED["+num_stopped+"] ERRORED["+num_errored+"]");
      // System.err.println("---------------------------------------------------");
      return (done);
   }

   void wakeUp() {
      logMessage(Level.INFO, "START Request job thread[" + jobThreadId + "] interrupt.");
      ThreadMonitor.stopThread(jobThreadId);
      logMessage(Level.INFO, "END Request job thread[" + jobThreadId + "] interrupt.");
   }

   private long calcSleepTime() {
      long sleep_time = 0;
      for (Job myMaster : MyMasters) {
         long estimated_runtime = myMaster.getEstimatedRuntime();
         double percent_to_complete = 1 - myMaster.getAmountCompleted();
         sleep_time += (long) (estimated_runtime * percent_to_complete);
         logMessage(Level.INFO, "MasterID[" + myMaster.getID() + "] Est Runtime[" + estimated_runtime + "] Percent to Completion[" + percent_to_complete
               + "] Sleep Time[" + sleep_time + "]");
      }

      // Only return 80% of the time, so that the slave wakes up at least once before it's masters are done to recalc a final sleep time.
      sleep_time = (long) (sleep_time * 0.8);
      if (sleep_time < 5000) {
         sleep_time = 5000;
      }
      return (sleep_time);
   }

   // ------------------------ Implements JobStatusListener Interface ------------------------ //

   @Override
   public synchronized void jobStatusChanged(String job_id, JobStatus old_status, JobStatus new_status) {
      if (MyMasters != null && MyMasters.length > 0) {
         boolean masters_done = mastersDone();
         logMessage(Level.INFO, "JobID[" + job_id + "] Old Status[" + old_status + "] New Status[" + new_status + "] AllMastersDone[" + masters_done
               + "] + MastersDoneOk[" + MastersPartiallyDoneOk + "] MastersPartiallyDoneOk[" + MastersPartiallyDoneOk + "]");
         // System.out.println("THIS["+getID()+"] JobID["+job_id+"] Old Status["+translateStateCode(old_status)+"] New Status["+translateStateCode(new_status)+"] AllMastersDone["+masters_done+"] + MastersDoneOk["+MastersPartiallyDoneOk+"] MastersPartiallyDoneOk["+MastersPartiallyDoneOk+"]");
         boolean found = false;
         for (int i = 0; i < MyMasters.length && !found; i++) {
            // Look for the master whose job_id is related to the new_status and if
            // they are in an errored or stopped state, then check if all the other masters
            // have completed and if so, stop the slave.
            String master_id = MyMasters[i].getID();
            if (master_id.equals(job_id)) {
               found = true;
               if (new_status.isDone() || new_status.isErrored() || new_status.isStopped()) {
                  if (masters_done) {
                     wakeUp();
                  }
               }
            }
         }
         // System.out.println("-------------------------------------------------");
      }
   }

   // ------------------------ Implements TaskListener Interface ------------------------ //

   @Override
   public void taskInterrupted(Task a_task) {
      logMessage(Level.INFO, "Task Interrupted: TASK[" + a_task.getID() + "] TASK.INTER[" + a_task.isInterrupted() + "] TASK.ERR[" + a_task.isErrored() + "]");
      if (a_task.isErrored()) {
         interrupt(JobStatus.ERRORED);
      }
   }

   @Override
   public void taskDone(Task a_task) {
      String task_id = a_task.getID();
      JobTaskInfo info = Tasks.get(task_id);
      info.setAmountCompleted(1.0);
      TasksDone++;

      if (TasksDone == Tasks.size()) { // All tasks are done...
         logMessage(Level.INFO, "All " + Tasks.size() + " tasks completed.");

         AmountCompleted = 1.0;
         StatusMessage = JOB_DONE_MESSAGE;

         setState(JobStatus.DONE);
      }
      else if (MultiThreaded) {
         startNextAsyncTask();
      }
      else if (!MultiThreaded) {
         Task next_task = a_task.getNextTask();
         logMessage(Level.INFO, "Finished task[" + a_task.getID() + "], starting next task[" + next_task.getID() + "].");
      }
   }

   @Override
   public void updateAmountCompleted(String task_id, double task_amount_completed) {
      // System.out.println("updateAmountCompleted(task_id["+task_id+"], task_amount_completed["+task_amount_completed+"])");
      // System.out.println("**************** Job AmountCompleted B4: " + AmountCompleted);

      if (task_amount_completed > 1.0d) {
         task_amount_completed = 1.0d;
      }
      else if (task_amount_completed < 0.01d) {
         task_amount_completed = 0.01d;
      }

      JobTaskInfo info = Tasks.get(task_id);
      // System.out.println("     info.getAmountCompleted()["+info.getAmountCompleted()+"] < task_amount_completed["+task_amount_completed+"]");
      if (info.getAmountCompleted() < task_amount_completed) {
         info.setAmountCompleted(task_amount_completed);

         double amount_completed = 0.0d;
         Object[] keys = Tasks.keySet().toArray();
         for (Object key : keys) {
            info = Tasks.get(key);
            Task task = info.getTaskObject();
            if (task.isDone() && info.getAmountCompleted() >= 1.0d) {
               amount_completed += info.getWeight();
               // System.out.println("**************** Adding DONE task[" + task.getID() + "] Weight[" + info.getWeight() + "] amount_completed[" +
               // amount_completed + "].");
            }
            else {
               amount_completed += info.getAmountCompleted() * info.getWeight();
               // System.out.println("**************** Adding NOT DONE task[" + task.getID() + "] Task.AmountCompleted[" + info.getAmountCompleted() +
               // "] Weight[" + info.getWeight() + "] amount_completed[" + amount_completed + "].");
            }
         }
         AmountCompleted = amount_completed;
      }
      // System.out.println("**************** Job AmountCompleted AFTER: " + AmountCompleted);
      // System.out.println();
   }

   @Override
   public void updateStatusMessage(String task_id, String message) {
      if (message != null && !message.isEmpty()) {
         String new_status_msg = task_id + "|" + message;
         if ((StatusMessage == null) || (!StatusMessage.equalsIgnoreCase(new_status_msg))) {
            StatusMessage = new_status_msg;

            if (Logs != null && Logs.containsKey(task_id)) {
               StringBuffer log = Logs.get(task_id);
               if ((message.indexOf("BEGIN") > -1) || (message.indexOf("END") > -1)) {
                  // this is intentionally ignored and left empty...
               }
               else {
                  message = StringUtils.getSpaces(5) + message;
                  log.append(message + FileSystem.NEWLINE);
               }
            }
            else {
               logMessage(Level.INFO, "No log buffer was found for task " + task_id + " while trying to update it's log.");
            }
         }
      }
   }

   @Override
   public void updateEstimatedRuntime(String task_id, long time) {
      JobTaskInfo info = Tasks.get(task_id);
      // logMessage("Updating estimated runtime for task["+task_id+"] from "+info.getEstimatedRuntime()+" to "+time+".");
      info.setEstimatedRuntime(time);
   }

   private class JobStopManager implements Runnable {

      private Job MyJob;
      private Thread JobCancelThread;

      private JobStopManager(Job ajob) {
         MyJob = ajob;
         JobCancelThread = new Thread(this);
         JobCancelThread.setName("JobStopManager_" + MyJob.getID());
         JobCancelThread.start();
      }

      /**
       * Implements the Runnable interface.
       */
      @Override
      public void run() {
         if (MyJob != null) {
            JobStatus cur_state = MyJob.getState();
            if (cur_state.isQueued() || cur_state.isWaiting() || cur_state.isRunning()) {
               MyJob.interrupt(JobStatus.STOPPED);
            }

            MyJob = null;
            if (JobCancelThread != null) {
               JobCancelThread.interrupt();
               JobCancelThread = null;
            }
         }
         else {

         }
      }
   }

   public static enum JobStatus {

      UNKNOWN("UNKNOWN"),
      QUEUED("QUEUED"),
      RUNNING("RUNNING"),
      DONE("DONE"),
      STOPPED("STOPPED"),
      ERRORED("ERRORED"),

      // This is different from QUEUED in that the object is "waiting" on others to finish. Object is
      // not Queued but not Running either.
      WAITING("WAITING");

      private String text;

      private JobStatus(String text) {
         this.text = text;
      }

      public static boolean isQueued(JobStatus status) {
         return QUEUED.equals(status);
      }

      public boolean isQueued() {
         return isQueued(this);
      }

      public static boolean isRunning(JobStatus status) {
         return RUNNING.equals(status);
      }

      public boolean isRunning() {
         return isRunning(this);
      }

      public static boolean isDone(JobStatus status) {
         return DONE.equals(status);
      }

      public boolean isDone() {
         return isDone(this);
      }

      public static boolean isStopped(JobStatus status) {
         return STOPPED.equals(status);
      }

      public boolean isStopped() {
         return isStopped(this);
      }

      public static boolean isErrored(JobStatus status) {
         return ERRORED.equals(status);
      }

      public boolean isErrored() {
         return isErrored(this);
      }

      public static boolean isWaiting(JobStatus status) {
         return WAITING.equals(status);
      }

      public boolean isWaiting() {
         return isWaiting(this);
      }

      @Override
      public String toString() {
         return text;
      }
   }
}
