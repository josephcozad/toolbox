package com.jc.command.task;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import com.jc.command.task.Job.JobStatus;
import com.jc.util.FileSystem;

// A 'process' is a group of jobs that appear as a single job.
public class Process {

   // all processes must be accessed through this collection
   private static final Hashtable<String, Process> PROCESS_STORE = new Hashtable<String, Process>();

   private final String Id;

   private final Vector<Job> Jobs;

   private long LastStatsDate;

   private final Hashtable<JobStatus, Integer> Stats;

   private Process(String id, Vector<Job> jobs) {
      Id = id;
      Jobs = new Vector<Job>();
      Stats = new Hashtable<JobStatus, Integer>();

      if (jobs != null) {
         Jobs.addAll(jobs);
      }
   }

   String getId() {
      return Id;
   }

   public Vector<Job> getJobs() {
      //      return Collections.unmodifiableList(Jobs);
      return (Jobs);
   }

   public static void register(String id, Vector<Job> jobs) {
      PROCESS_STORE.put(id, new Process(id, jobs));
   }

   public static boolean unregister(String id) {
      return PROCESS_STORE.remove(id) != null;
   }

   public static String getProcessIdFor(String jobId) {
      for (Process process : PROCESS_STORE.values()) {
         for (Job job : process.getJobs()) {
            if (job.getID().equals(jobId)) {
               return process.getId();
            }
         }
      }
      return null;
   }

   public static Vector<Job> getJobsFor(String processId) {
      Process process = valueOf(processId);
      if (process == null) {
         return new Vector<Job>();
      }
      return process.Jobs;
   }

   public static Process valueOf(String id) {
      for (Process process : PROCESS_STORE.values()) {
         if (process.Id.equals(id)) {
            return process;
         }
      }
      return null;
   }

   public static boolean isProcess(String id) {
      return PROCESS_STORE.containsKey(id);
   }

   public static JobStatus getJobStatus(String id, boolean currentlyQueueing) {
      return getJobStatus(id, currentlyQueueing, true);
   }

   public static JobStatus getJobStatus(String id, boolean currentlyQueueing, boolean recalc) {
      Process process = valueOf(id);

      if (process == null) {
         return JobStatus.UNKNOWN;
      }

      StringBuilder unknownState = new StringBuilder();
      int total = process.Jobs.size();

      int doneCount = 0;
      int erroredCount = 0;
      int runningCount = 0;
      int queuedCount = 0;
      int stoppedCount = 0;
      int unknownCount = 0;

      if (recalc || process.LastStatsDate == 0) {
         for (Job job : process.Jobs) {
            if (job.isQueued()) {
               ++queuedCount;
            }
            else if (job.isRunning() || job.isWaiting()) {
               ++runningCount;
            }
            else if (job.isErrored()) {
               ++erroredCount;
            }
            else if (job.isDone()) {
               ++doneCount;
            }
            else if (job.isStopped()) {
               ++stoppedCount;
            }
            else { // Unknown state
               if (unknownState.length() > 0) {
                  unknownState.append(",");
               }
               unknownState.append(job.getID());
               ++unknownCount;
            }
         }

         process.Stats.put(JobStatus.DONE, doneCount);
         process.Stats.put(JobStatus.ERRORED, erroredCount);
         process.Stats.put(JobStatus.RUNNING, runningCount);
         process.Stats.put(JobStatus.QUEUED, queuedCount);
         process.Stats.put(JobStatus.STOPPED, stoppedCount);
         process.Stats.put(JobStatus.UNKNOWN, unknownCount);

         process.LastStatsDate = System.currentTimeMillis();
      }
      else {
         // TODO log this instead?
         System.out.println("Last recalc date: " + new Date(process.LastStatsDate));
         doneCount = process.Stats.get(JobStatus.DONE);
         erroredCount = process.Stats.get(JobStatus.ERRORED);
         runningCount = process.Stats.get(JobStatus.RUNNING);
         queuedCount = process.Stats.get(JobStatus.QUEUED);
         stoppedCount = process.Stats.get(JobStatus.STOPPED);
         unknownCount = process.Stats.get(JobStatus.UNKNOWN);
      }

      String message = "D[" + doneCount + "] E[" + erroredCount + "] R[" + runningCount + "] Q[" + queuedCount + "] S[" + stoppedCount + "] U[" + unknownCount
            + "] #JOBS[" + total + "]";

      if (unknownCount > 0) {
         if (currentlyQueueing) {
            return JobStatus.UNKNOWN; // Valid unknown state while queuing process jobs...
         }
         throw new IllegalStateException("ERROR: " + unknownCount + " of the jobs[" + unknownState.toString() + "] in the process[" + id
               + "] is in an unknown state. " + message);
      }

      if (doneCount == total) {
         // If all are done...
         return JobStatus.DONE;
      }
      if (stoppedCount == total) {
         // If all are stopped...
         return JobStatus.STOPPED;
      }
      if (erroredCount > 0) {
         // If at least one has errored
         return JobStatus.ERRORED;
      }
      if (runningCount > 0) {
         // If at least one is running or waiting and none have errored
         return JobStatus.RUNNING;
      }
      if (queuedCount > 0) {
         // If at least one is queued and none are running, waiting, or errored
         return JobStatus.QUEUED;
      }
      if (stoppedCount > 0) {
         // If at least one is stopped and none are running, waiting, queued, or errored
         return JobStatus.STOPPED;
      }
      throw new IllegalStateException("ERROR: determining status of process[" + id + "]. " + message);
   }

   public static String getLog(String id) {
      Process process = valueOf(id);

      if (process == null) {
         return "";
      }

      StringBuilder log = new StringBuilder();
      for (Job job : process.Jobs) {
         if (log.length() > 0) {
            log.append(FileSystem.NEWLINE);
         }
         log.append(job.getLogData());
      }
      return log.toString();
   }

   public static long getEstimatedRuntime(String id) {
      Process process = valueOf(id);

      if (process == null) {
         return 0;
      }

      long estimatedRuntime = 0;
      for (Job job : process.Jobs) {
         estimatedRuntime += job.getEstimatedRuntime();
      }

      return estimatedRuntime;
   }

   public static double getAmountCompleted(String id) {
      Process process = valueOf(id);

      if (process == null) {
         return 0;
      }

      if (process.Jobs.isEmpty()) {
         // TODO logged this message:
         // "No jobs were found for process id " + id + " while trying to get the amount completed.";
         return 0;
      }

      double amountCompleted = 0.0d;
      for (Job job : process.Jobs) {
         if (job.isRunning() || job.isDone()) {
            amountCompleted += job.getAmountCompleted();
         }
      }

      return amountCompleted / process.Jobs.size();
   }
}
