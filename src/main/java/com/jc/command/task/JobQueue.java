package com.jc.command.task;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import org.json.JSONObject;

import com.jc.command.task.Job.JobStatus;
import com.jc.log.Logger;
import com.jc.util.FileSystem;

/*
 * A JobQueue is a collection of Jobs that are divided up into one or more queues, 
 * in which each job must finish before the next one begins. The Jobs in a JobQueue
 * run asynchronously to the main program thread.
 * 
 * A JobRunner runs one and only one Job synchronously inside the main program thread.
 * 
 * Job: a group of unrelated tasks that can be run synchronously (by the JobRunner)
 * or asynchronously (by the JobQueue); each job has it's own priority level.
 * 
 * Slave: a type of job that is dependent on one or more 'master' jobs to finish before 
 * running; 'masters' are by default required to finish successfully for the slave 
 * to begin running; this can optionally be changed.
 * 
 * Job Group: an ordered group of unqueued jobs that are queued by the JobQueue all 
 * with the same priority level. Jobs within the Job Group are started in the order 
 * they are presented to the JobQueue.
 * 
 * Process: a group of queued jobs that are registered with the JobQueue and appear 
 * as one job.
 */

public class JobQueue implements JobStatusListener {

   final static String LOG_ID = "jobQueue";

   private static TreeMap<Float, Vector<Job>> JobInfo; // Jobs ordered by their priority (float).
   private static Hashtable<String, Job> Jobs; // Jobs by their id.
   private static Hashtable<String, Vector<Job>> Processes; // A 'process' is a group of jobs that appear as a single job.
   private static Hashtable<String, String> JobIDXref; // A cross reference of virtual job id --> real job id; note the real id may be a process id.

   private boolean Stopped;
   private boolean QueuingJobs;

   public JobQueue() {
      if (JobInfo == null) {
         JobInfo = new TreeMap<>();
      }

      if (Jobs == null) {
         Jobs = new Hashtable<>();
      }

      Stopped = true;
      QueuingJobs = false;
   }

   // -------------------------------------------------

   public List<String> getAllJobIds() {
      Set<String> jobIdSet = Jobs.keySet();
      List<String> jobIdList = new ArrayList<>();
      jobIdList.addAll(jobIdSet);
      return jobIdList;
   }

   public List<String> getAllProcessIds() {
      Set<String> processIdSet = Processes.keySet();
      List<String> processIdList = new ArrayList<>();
      processIdList.addAll(processIdSet);
      return processIdList;
   }

   // -------------------------------------------------

   public String getLogData(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      StringBuffer log_data = new StringBuffer("");
      if (isAProcess(job_id)) {
         String proc_log_data = getProcessLogData(job_id);
         log_data.append(proc_log_data);
      }
      else {
         if (Jobs.containsKey(job_id)) {
            Job ajob = Jobs.get(job_id);
            log_data.append(ajob.getLogData());
         }
      }
      return (log_data.toString());
   }

   public void removeJob(String job_id) throws Exception {
      JobStatus jobStatus = getJobStatus(job_id);
      if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
         String orig_job_id = job_id;
         job_id = getJobIDForVirtualJobID(job_id);
         if (isAProcess(job_id)) {
            removeProcess(job_id);
         }
         else {
            if (Jobs.containsKey(job_id)) {

               Job ajob = Jobs.get(job_id);
               if (ajob.isQueued() || ajob.isRunning()) {
                  ajob.stop(); // Request stop...
               }

               ajob.cleanUp();
               Jobs.remove(job_id);

               // look for job in the JobInfo
               Float priority = new Float(ajob.getPriority());
               Vector<Job> jobs = JobInfo.get(priority);
               jobs.remove(ajob);

               logMessage(Level.INFO, "Removed job " + job_id + " from the job queue.");
            }
         }

         // remove any associations the orig_job_id had...
         if (JobIDXref != null && JobIDXref.containsKey(orig_job_id)) {
            JobIDXref.remove(orig_job_id);
         }
      }
      else {
         throw new Exception("Job '" + job_id + "' is " + jobStatus + " and must be stopped first before it can be removed.");
      }
   }

   /**
    * Queues a group of jobs according to the supplied group's priority. It's assumed that the supplied jobs are an ordered list of jobs and no further processing based on each job's priority will be done.
    */
   public void queueJobs(Vector<Job> jobs, float priority) throws Exception {
      QueuingJobs = true;

      for (int i = 0; i < jobs.size(); i++) {
         Job ajob = jobs.elementAt(i);

         String jobid = ajob.getID();
         if (getJobsForProcess(jobid) != null) {
            throw (new Exception("The job id " + jobid + ", matches a previously registered process."));
         }

         if (Jobs.containsKey(jobid)) {
            throw (new Exception("The job id " + jobid + ", has already been queued and cannot be 'requeued'."));
         }

         ajob.addJobStatusListener(this);

         // Add to the queue...
         Jobs.put(jobid, ajob);
      }

      // Search JobInfo to make sure that the jobs being queued are
      // not already assigned a priority.
      Object[] keys = JobInfo.keySet().toArray();
      for (Object key : keys) {
         Vector<Job> job_vec = JobInfo.get(key);
         for (int j = 0; j < jobs.size(); j++) {
            Job ajob = jobs.elementAt(j);
            if (job_vec.contains(ajob)) {
               throw (new Exception("The job id " + ajob.getID() + ", has already been assigned a priority and cannot be reassigned."));
            }
         }
      }

      Float group_priority = new Float(priority);
      if (!JobInfo.containsKey(group_priority)) {
         JobInfo.put(group_priority, jobs);
      }
      else {
         Vector<Job> job_vec = JobInfo.get(group_priority);
         for (int i = 0; i < jobs.size(); i++) {
            Job ajob = jobs.elementAt(i);
            job_vec.add(ajob);
         }
      }

      int num_jobs = jobs.size();
      for (int i = 0; i < num_jobs; i++) {
         Job ajob = jobs.get(i);
         logMessage(Level.INFO, "Queued job[" + ajob.getID() + "] with priority[" + ajob.getPriority() + "].");
         ajob.setState(JobStatus.QUEUED);
      }

      QueuingJobs = false;
   }

   /**
    * Queues job objects according to each one's priority.
    */
   public void queueJob(Job ajob) throws Exception {
      QueuingJobs = true;

      String jobid = ajob.getID();

      // Search Processes to see if the job id matches any process ids; they cannot be the same.
      if (getJobsForProcess(jobid) != null) {
         throw (new Exception("The job id " + jobid + ", matches a previously registered process."));
      }

      // Search Jobs to see if the job has already been queued.
      if (Jobs.containsKey(jobid)) {
         throw (new Exception("The job id " + jobid + ", has already been queued and cannot be 'requeued'."));
      }

      // Search JobInfo to make sure that the jobs being queued are
      // not already assigned a priority.
      Object[] keys = JobInfo.keySet().toArray();
      for (Object key : keys) {
         Vector<Job> job_vec = JobInfo.get(key);
         if (job_vec.contains(ajob)) {
            throw (new Exception("The job id " + jobid + ", has already been assigned a priority and cannot be reassigned."));
         }
      }

      ajob.addJobStatusListener(this);

      // Add to the queue...
      Jobs.put(jobid, ajob);

      Float priority = new Float(ajob.getPriority());
      if (!JobInfo.containsKey(priority)) {
         Vector<Job> job_group = new Vector<>();
         job_group.add(ajob);
         JobInfo.put(priority, job_group);
      }
      else {
         Vector<Job> job_group = JobInfo.get(priority);
         job_group.add(ajob);
      }

      logMessage(Level.INFO, "Queued job[" + ajob.getID() + "] with priority[" + ajob.getPriority() + "].");

      ajob.setState(JobStatus.QUEUED);

      QueuingJobs = false;
   }

   public void runJobs() throws Exception {
      // outputJobQueueContents(); // For debugging purposes only.
      Stopped = false;
      if (JobInfo != null) {
         Object[] keys = JobInfo.descendingKeySet().toArray();
         for (Object key : keys) {
            Vector<Job> job_group = JobInfo.get(key);
            int num_jobs = job_group.size();
            for (int j = 0; j < num_jobs; j++) {
               Job ajob = job_group.get(j);
               synchronized (ajob) {
                  if (ajob.isQueued()) {
                     logMessage(Level.INFO,
                           "Starting job[" + ajob.getID() + "] with priority[" + key.toString() + "]; job status is [" + ajob.getState() + "].");
                     ajob.start();
                  }
               }
            }
            Thread.sleep(2500); // wait 2.5 secs before starting the next group of jobs.
         }
      }
   }

   public void cancelAllJobs() throws Exception {
      Stopped = true;
      if (Jobs != null) {
         Object[] keys = Jobs.keySet().toArray();
         for (Object key : keys) {
            Job ajob = Jobs.get(key);
            if (ajob.isRunning() || ajob.isQueued() || ajob.isWaiting()) {
               ajob.stop();
            }
         }
      }
      else {
         throw (new RuntimeException("ERROR: JobQueue was null when trying to stop all running jobs."));
      }
   }

   public void cancelJob(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      Vector<Job> job_vec = getJob(job_id);
      int num_jobs = job_vec.size();
      for (int i = 0; i < num_jobs; i++) {
         Job ajob = job_vec.elementAt(i);
         if (ajob.isRunning() || ajob.isQueued() || ajob.isWaiting()) {
            logMessage(Level.INFO, "Requesting Job[" + job_id + "." + ajob.getID() + "] stop with status of [" + ajob.getState() + "]");
            ajob.stop();
         }
         else {
            logMessage(Level.INFO, "Cancel request not done; Job[" + job_id + "." + ajob.getID() + "] status is [" + ajob.getState() + "]");
         }
      }
   }

   public JobStatus getJobStatus(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      Vector<Job> job_vec = getJob(job_id);
      JobStatus status = JobStatus.UNKNOWN;
      if (job_vec != null) {
         if (job_vec.size() > 1) {
            // logMessage("Getting status for process[" + job_id + "].");
            status = getStatusForProcess(job_id);
         }
         else {
            Job ajob = job_vec.elementAt(0);
            status = ajob.getState();
            if (status.equals(JobStatus.UNKNOWN)) {
               logMessage(Level.WARNING, "Job id[" + job_id + "] is in an unknown state [" + ajob.getState() + "].");
            }
         }
      }
      else {
         logMessage(Level.WARNING, "No job id[" + job_id + "] was found in the list of jobs.");
      }
      return (status);
   }

   public double getAmountCompleted(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      double amount_completed = 0.0d;
      if (isAProcess(job_id)) {
         amount_completed = getProcessAmountCompleted(job_id);
      }
      else {
         Vector<Job> job_vec = getJob(job_id);
         if (job_vec != null && job_vec.size() > 0) {
            Job ajob = job_vec.elementAt(0);
            amount_completed = ajob.getAmountCompleted(); // * 100;
         }
         else {
            logMessage(Level.WARNING, "No job id[" + job_id + "] was found in the list of jobs.");
         }
      }
      return (amount_completed);
   }

   public String getStatusMessage(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      String message = "";
      if (isAProcess(job_id)) {
         message = getProcessStatusMessage(job_id);
      }
      else {
         Vector<Job> job_vec = getJob(job_id);
         if (job_vec != null && job_vec.size() > 0) {
            Job ajob = job_vec.elementAt(0);
            message = ajob.getStatusMessage();
         }
         else {
            logMessage(Level.WARNING, "No job id[" + job_id + "] was found in the list of jobs.");
         }
      }
      return (message);
   }

   public long getEstimatedRuntime(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      long estimated_runtime = 0L;
      if (isAProcess(job_id)) {
         estimated_runtime = getProcessEstimatedRuntime(job_id);
      }
      else {
         Vector<Job> job_vec = getJob(job_id);
         if (job_vec != null && job_vec.size() > 0) {
            Job ajob = job_vec.elementAt(0);
            estimated_runtime = ajob.getEstimatedRuntime();
         }
         else {
            logMessage(Level.WARNING, "No job id[" + job_id + "] was found in the list of jobs.");
         }
      }
      return (estimated_runtime);
   }

   /**
    * Returns a Vector object of Job objects associated to the id supplied. The id can be either a job id or a process id.
    */
   public Vector<Job> getJob(String job_id) throws Exception {
      job_id = getJobIDForVirtualJobID(job_id);
      Vector<Job> job_vec = null;
      if (isAProcess(job_id)) {
         job_vec = getJobsForProcess(job_id);
      }
      else {
         if (Jobs != null && Jobs.containsKey(job_id)) {
            Job ajob = Jobs.get(job_id);
            job_vec = new Vector<>();
            job_vec.add(ajob);
         }
      }
      return (job_vec);
   }

   /**
    * Associates a real job_id with a virtual job_id so that if a virtual job_id is given to the JobQueue, information referencing the real id will be returned. If the virtual_job_id already is associated with a job_id, it will be re-associated with the supplied job_id.
    */
   public void associateJobID(String virtual_job_id, String job_id) throws Exception {
      if (JobIDXref == null) {
         JobIDXref = new Hashtable<>();
      }
      JobIDXref.put(virtual_job_id, job_id);
   }

   // Returns the associated job id if one exists.
   String getJobIDForVirtualJobID(String virtual_job_id) {
      String job_id = virtual_job_id;
      if (JobIDXref != null && JobIDXref.containsKey(virtual_job_id)) {
         job_id = JobIDXref.get(virtual_job_id);
      }
      return (job_id);
   }

   public String getVirtualIDByJobID(String job_id) {
      String virtual_job_id = job_id;
      if (JobIDXref != null && (JobIDXref.containsValue(job_id) || JobIDXref.containsValue(job_id + "_proc"))) {
         for (int i = 0; i < JobIDXref.size(); i++) {
            String key = JobIDXref.keys().nextElement();
            String val = JobIDXref.get(key);
            if (val.contains(job_id) && key.contains("async")) {
               virtual_job_id = key;
               break;
            }
         }
      }
      return virtual_job_id;
   }

   protected void logMessage(Level level, String message) {
      logMessage(level, message, null);
   }

   protected void logMessage(Level level, String message, Throwable exc) {
      try {
         //         if (ConfigInfo.getInstance().hasProperty("log." + LOG_ID + ".directory")) {
         String statement = getLoggingStatement(message);
         if (exc != null) {
            Logger.log(LOG_ID, getClass(), level, statement, exc);
         }
         else {
            Logger.log(LOG_ID, getClass(), level, statement);
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
      // DEV NOTE: statement must have the same number of fields as the statement in same method in Job.java.
      String statement = getID() + Logger.LOGFILE_FIELD_SEPARATOR + message + Logger.LOGFILE_FIELD_SEPARATOR + "n/a";
      return statement;
   }

   private String getID() {
      return ("JobQueue");
   }

   // ------------------------------------------------------------------

   public JSONObject getStatistics() {
      int numJobs = 0;
      if (Jobs != null) {
         numJobs = Jobs.size();
      }

      int numJobsQueued = 0;
      int numJobsWaiting = 0;
      int numJobsRunning = 0;
      int numJobsErrored = 0;
      int numJobsStopped = 0;
      int numJobsDone = 0;

      if (Jobs != null && !QueuingJobs) {
         for (Map.Entry<String, Job> entry : Jobs.entrySet()) {
            //        String jobId = entry.getKey();
            Job ajob = entry.getValue();
            // ...
            if (ajob.isQueued()) {
               numJobsQueued++;
            }
            else if (ajob.isWaiting()) {
               numJobsWaiting++;
            }
            else if (ajob.isRunning()) {
               numJobsRunning++;
            }
            else if (ajob.isErrored()) {
               numJobsErrored++;
            }
            else if (ajob.isStopped()) {
               numJobsStopped++;
            }
            else if (ajob.isDone()) {
               numJobsDone++;
            }
         }
      }

      JSONObject jobQueueStatistics = new JSONObject();
      jobQueueStatistics.put("numJobs", numJobs);
      jobQueueStatistics.put("numJobsQueued", numJobsQueued);
      jobQueueStatistics.put("numJobsWaiting", numJobsWaiting);
      jobQueueStatistics.put("numJobsRunning", numJobsRunning);
      jobQueueStatistics.put("numJobsErrored", numJobsErrored);
      jobQueueStatistics.put("numJobsStopped", numJobsStopped);
      jobQueueStatistics.put("numJobsDone", numJobsDone);

      return jobQueueStatistics;

   }

   // ------------------------ Code Supporting Processes ------------------------ //

   public void registerAsAProcess(Vector<Job> process, String process_id) throws Exception {
      // A process id cannot be the same as a job id.
      if (getJob(process_id) != null) {
         throw (new Exception("ERROR: process " + process_id + " matches a job already queued with the same id."));
      }

      if (Processes == null) {
         Processes = new Hashtable<>();
      }

      // Check to see if there's a process already registered and if so,
      // if the jobs in the registered process are the same as the ones
      // supplied to this method.
      Vector<Job> job_vec = getJobsForProcess(process_id);
      if (job_vec != null) {
         int num_jobs = process.size();
         for (int i = 0; i < num_jobs; i++) {
            Job job_proc = process.elementAt(i);
            if (!job_vec.contains(job_proc)) {
               throw (new Exception("ERROR: a process " + process_id + " is already registered with different jobs."));
            }
         }
      }

      Processes.put(process_id, process);
      logMessage(Level.INFO, "Registered job_process[" + process_id + "].");

   }

   // This method 'de-registers' and 'de-queues' the jobs
   // associated with the process_id
   public void removeProcess(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);
      Vector<Job> job_vec = getJobsForProcess(process_id);
      if (job_vec != null) {
         int num_jobs = job_vec.size();
         for (int i = 0; i < num_jobs; i++) {
            Job ajob = job_vec.elementAt(i);
            String job_id = ajob.getID();
            removeJob(job_id);
         }

         Processes.remove(process_id);
         logMessage(Level.INFO, "Removed job process " + process_id + " from the job queue.");
      }
      // else process doesn't exist, ignore it.
   }

   public boolean isAProcess(String process_id) {
      process_id = getJobIDForVirtualJobID(process_id);
      return (getJobsForProcess(process_id) != null);
   }

   /*
    * Return the process id associated with the supplied job id if one
    * exits, otherwise return a null value.
    */
   public String getProcessID(String job_id) {
      job_id = getJobIDForVirtualJobID(job_id);
      String process_id = null;
      if (Processes != null && Processes.size() > 0) {
         Object[] keys = Processes.keySet().toArray();
         for (int i = 0; i < keys.length && process_id == null; i++) {
            Vector<Job> jobs_vec = Processes.get(keys[i]);
            int num_jobs = jobs_vec.size();
            for (int j = 0; j < num_jobs; j++) {
               Job ajob = jobs_vec.get(j);
               if (ajob.getID().equals(job_id)) {
                  process_id = (String) keys[i];
               }
            }
         }
      }
      return (process_id);
   }

   public Vector<Job> getJobsForProcess(String process_id) {
      process_id = getJobIDForVirtualJobID(process_id);
      Vector<Job> process = null;
      if (Processes != null && Processes.size() > 0 && Processes.containsKey(process_id)) {
         process = Processes.get(process_id);
      }
      return (process);
   }

   public JobStatus getStatusForProcess(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);

      Vector<Job> job_vec = getJob(process_id);

      int num_jobs = job_vec.size();

      int num_done = 0;
      int num_errored = 0;
      int num_running = 0;
      int num_queued = 0;
      int num_stopped = 0;
      int num_unknown = 0;

      StringBuffer joblist_unknown_state = new StringBuffer();
      for (int i = 0; i < num_jobs; i++) {
         Job ajob = job_vec.elementAt(i);
         if (ajob.isQueued()) {
            num_queued++;
         }
         else if (ajob.isRunning() || ajob.isWaiting()) {
            num_running++;
         }
         else if (ajob.isErrored()) {
            num_errored++;
         }
         else if (ajob.isDone()) {
            num_done++;
         }
         else if (ajob.isStopped()) {
            num_stopped++;
         }
         else { // Unknown state
            if (joblist_unknown_state.length() > 0) {
               joblist_unknown_state.append(",");
            }
            joblist_unknown_state.append(ajob.getID());
            num_unknown++;
         }
      }

      String message = "D[" + num_done + "] E[" + num_errored + "] R[" + num_running + "] Q[" + num_queued + "] S[" + num_stopped + "] U[" + num_unknown
            + "] #JOBS[" + num_jobs + "]";

      JobStatus status = JobStatus.UNKNOWN; // Unknown...
      if (((num_done + num_errored + num_running + num_queued + num_stopped + num_unknown) != num_jobs)) {
         // ERROR... throw exception.
         throw (new Exception("ERROR: determining status of process[" + process_id + "]; mis-match in number of jobs to status info. " + message));
      }
      else {
         if (num_unknown > 0) {
            if (QueuingJobs) {
               status = JobStatus.UNKNOWN; // Valid unknown state while queuing process jobs...
            }
            else {
               throw (new Exception("ERROR: " + num_unknown + " of the jobs[" + joblist_unknown_state.toString() + "] in the process[" + process_id
                     + "] is in an unknown state. " + message));
            }
         }
         else if (num_done == num_jobs) { // If all are done...
            status = JobStatus.DONE; // Done...
         }
         else if (num_stopped == num_jobs) { // If all are stopped...
            status = JobStatus.STOPPED; // Stopped...
         }
         else if (num_errored == num_jobs) { // If all are errored...
            status = JobStatus.ERRORED; // Errored...
         }
         else if (num_errored > 0) { // If one or more has errored
            status = JobStatus.ERRORED; // Errored...
         }
         else if (num_running > 0 && num_errored == 0) { // If one is running or waiting, and none have errored, regardless of any other status
            status = JobStatus.RUNNING; // Running...
         }
         else if (num_queued > 0 && num_running == 0) { // If one is queued and none are running or waiting
            status = JobStatus.QUEUED; // Queued...
         }
         else if (num_stopped > 0 && (num_running == 0 && num_queued == 0 && num_errored == 0)) { // If one is stopped and none are running, waiting, or queued
            status = JobStatus.STOPPED; // Stopped...
         }
         else {
            // ERROR... throw exception.
            throw (new Exception("ERROR: determining status of process[" + process_id + "]. " + message));
         }
      }
      return (status);
   }

   public String getProcessLogData(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);
      StringBuffer log_data = new StringBuffer("");
      Vector<Job> job_vec = getJob(process_id);
      int num_jobs = job_vec.size();
      for (int i = 0; i < num_jobs; i++) {
         Job ajob = job_vec.elementAt(i);
         log_data.append(ajob.getLogData());
         if (i + 1 < num_jobs) {
            log_data.append(FileSystem.NEWLINE);
         }
      }
      return (log_data.toString());
   }

   public long getProcessEstimatedRuntime(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);
      long estimated_runtime = 0L;
      Vector<Job> job_vec = getJob(process_id);
      int num_jobs = job_vec.size();
      for (int i = 0; i < num_jobs; i++) {
         Job ajob = job_vec.elementAt(i);
         long estjobtime = ajob.getEstimatedRuntime();
         estimated_runtime += estjobtime;
      }
      return (estimated_runtime);
   }

   public String getProcessStatusMessage(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);
      StringBuffer status_message = new StringBuffer("");

      // logMessage("Getting status for process[" + process_id + "].");
      JobStatus status = getStatusForProcess(process_id);
      if (status.isDone()) {
         status_message.append(process_id + " is done.");
      }
      else if (status.isQueued()) {
         status_message.append(process_id + " is queued.");
      }
      else if (status.isStopped()) {
         status_message.append(process_id + " was stopped.");
      }
      else if (status.isRunning()) {
         Vector<Job> job_vec = getJob(process_id);
         int num_jobs = job_vec.size();
         for (int i = 0; i < num_jobs; i++) {
            Job ajob = job_vec.elementAt(i);
            if (ajob.isRunning()) {
               if (status_message.length() > 0) {
                  status_message.append("; ");
               }
               status_message.append(ajob.getStatusMessage());
            }
         }
      }
      else if (status.isErrored()) {
         Vector<Job> job_vec = getJob(process_id);
         int num_jobs = job_vec.size();
         for (int i = 0; i < num_jobs; i++) {
            Job ajob = job_vec.elementAt(i);
            if (ajob.isErrored()) {
               if (status_message.length() > 0) {
                  status_message.append("; ");
               }
               status_message.append(ajob.getStatusMessage());
            }
         }
      }
      return (status_message.toString());
   }

   public double getProcessAmountCompleted(String process_id) throws Exception {
      process_id = getJobIDForVirtualJobID(process_id);
      double amount_completed = 0.0d;
      Vector<Job> job_vec = getJob(process_id);
      int num_jobs = job_vec.size();
      if (num_jobs > 0) {
         for (int i = 0; i < num_jobs; i++) {
            Job ajob = job_vec.elementAt(i);
            if (ajob.isRunning() || ajob.isDone()) {
               amount_completed += ajob.getAmountCompleted();
            }
         }

         amount_completed /= num_jobs;
      }
      else {
         logMessage(Level.WARNING, "No jobs were found for process id " + process_id + " while trying to get the amount completed.");
      }
      return (amount_completed);
   }

   // -------------------- Implements JobStatusListener -------------------------- //

   @Override
   public void jobStatusChanged(String job_id, JobStatus old_status, JobStatus new_status) {
      job_id = getJobIDForVirtualJobID(job_id);
      if (!Stopped) {
         logMessage(Level.INFO, "Job[" + job_id + "] was [" + old_status + "] now [" + new_status + "].");
      }
   }
}
