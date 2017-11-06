package com.jc.command.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

public class JobQMonitor extends Job {

   public JobQMonitor(JobQueue jobQueue) {
      this(jobQueue, false, (60000 * 60));
      // maxJobStateTime by default is 1 hour.
   }

   public JobQMonitor(JobQueue jobQueue, boolean clearAll, long maxJobStateTime) {
      super(new ClearJobQueue(jobQueue), false);

      setIDPrefix(getClass().getSimpleName() + "_");
      setMaxNumThreads(1);
      addJobStatusListener(this);

      ClearJobQueue task = null;
      Map<String, JobTaskInfo> taskMap = getTasks();
      for (JobTaskInfo value : taskMap.values()) {
         task = (ClearJobQueue) value.getTaskObject();
      }

      task.setMaxJobStateTime(maxJobStateTime);

      if (clearAll) {
         task.clearAll();
      }
   }

   private static class ClearJobQueue extends Task {

      private final JobQueue jobQueue;
      private boolean Running;
      private final double SleepTime; // In minutes...
      private boolean clearAll;
      private long maxJobStateTime;

      public ClearJobQueue(JobQueue jobQueue) {
         SleepTime = 1.5d;
         this.jobQueue = jobQueue;
         setIDPrefix(getClass().getSimpleName() + "_");
         clearAll = false;
      }

      public void clearAll() {
         clearAll = true;
      }

      public void setMaxJobStateTime(long maxJobStateTime) {
         this.maxJobStateTime = maxJobStateTime;
      }

      @Override
      public void doTask() throws Exception {
         Running = true;
         while (Running) {
            try {
               Thread.sleep((long) (60000 * SleepTime)); // sleep number of minutes.

               if (clearAll) {
                  clearAllJobsFromJobQueue();
               }
               else {
                  // A map of the jobIds belonging to running processes. Note that some jobs of a process may be in 
                  // different states, including done, stopped or errored, and others running, queued, or waiting.
                  Map<String, String> jobIdsToExclude = clearAllStoppedProcesses();
                  clearAllStoppedNonProcessJobs(jobIdsToExclude);
               }
            }
            catch (InterruptedException iex) {
               // eat it...
            }
         }
      }

      @Override
      protected void updateStatusMessage(String message) {
         if (message.contains(INTERRUPTED_TASK_STATUS_MSG)) {
            super.updateStatusMessage("Task stopped.");
         }
         else if (message.contains(ERRORED_TASK_STATUS_MSG)) {
            super.updateStatusMessage("Task errored.");
         }

         super.updateStatusMessage(message);
      }

      private void clearAllJobsFromJobQueue() throws Exception {
         // Cancel everything...
         jobQueue.cancelAllJobs();
         Thread.sleep((30000)); // wait 30 seconds for jobs to stop.

         // Remove all jobs regardless of if they are part of a process or not...
         List<String> jobIdList = jobQueue.getAllJobIds();
         if (!jobIdList.isEmpty()) {
            for (String jobId : jobIdList) {
               JobStatus jobStatus = jobQueue.getJobStatus(jobId);
               if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                  jobQueue.removeJob(jobId);
               }
               else {
                  // try canceling again...
                  jobQueue.cancelJob(jobId);
                  Thread.sleep((30000)); // wait 30 seconds for jobs to stop.
                  jobStatus = jobQueue.getStatusForProcess(jobId);
                  if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                     jobQueue.removeProcess(jobId);
                  }
                  else {
                     logMessage(Level.WARNING, "While trying to clear the jobQueue, unable to stop job with id of '" + jobId + "'.");
                  }
               }
            }
         }
      }

      private Map<String, String> clearAllStoppedProcesses() throws Exception {
         Map<String, String> jobIdsToExclude = new HashMap<>();

         List<String> processIdToStop = new ArrayList<>(); // these are processIds that had to be stopped, and need to be removed.

         // Remove "finished" processes first....
         List<String> processIdList = jobQueue.getAllProcessIds();
         if (!processIdList.isEmpty()) {
            for (String processId : processIdList) {
               JobStatus jobStatus = jobQueue.getStatusForProcess(processId);
               if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                  jobQueue.removeProcess(processId);
               }
               else {
                  // if the process is running, check the status time of all the process jobs, if the 
                  // youngest one is older than the maxJobStateTime, stop all running process jobs, and 
                  // remove the process.
                  long youngestStatusTime = Long.MAX_VALUE;
                  Vector<Job> processJobs = jobQueue.getJobsForProcess(processId);
                  for (Job aJob : processJobs) {
                     JobStatus jobStatus1 = jobQueue.getJobStatus(aJob.getID());
                     if (jobStatus1.isRunning() || jobStatus1.isWaiting() || jobStatus1.isQueued()) {
                        long statusTime = aJob.getStatusTimeStamp();
                        if (statusTime < youngestStatusTime) {
                           youngestStatusTime = statusTime;
                        }
                     }
                  }

                  if (youngestStatusTime > maxJobStateTime) {
                     jobQueue.cancelJob(processId);
                     processIdToStop.add(processId);
                  }
                  else { // add running process jobs ids to exclude in next step.
                     for (Job processJob : processJobs) {
                        String jobId = processJob.getID();
                        jobIdsToExclude.put(jobId, jobId);
                     }
                  }
               }
            }

            // if there are processIds that were stopped above, give them 30 secs and try to remove them...
            Thread.sleep((30000)); // wait 30 seconds for jobs to stop.
            if (!processIdToStop.isEmpty()) {
               for (String processId : processIdToStop) {
                  JobStatus jobStatus = jobQueue.getStatusForProcess(processId);
                  if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                     jobQueue.removeProcess(processId);
                  }
                  else {
                     logMessage(Level.WARNING, "While trying to clear the jobQueue, unable to stop process with id of '" + processId + "'.");
                  }
               }
            }
            // else no more processes to remove...   
         }

         return jobIdsToExclude;
      }

      private void clearAllStoppedNonProcessJobs(Map<String, String> jobIdsToExclude) throws Exception {
         List<String> jobIdToStop = new ArrayList<>(); // these are jobids that had to be stopped, and need to be removed.

         // Remove "finished" jobs if they are not part of a process...
         List<String> jobIdList = jobQueue.getAllJobIds();
         if (!jobIdList.isEmpty()) {
            for (String jobId : jobIdList) {
               if (!jobIdsToExclude.containsKey(jobId)) {
                  JobStatus jobStatus = jobQueue.getJobStatus(jobId);
                  if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                     jobQueue.removeJob(jobId);
                  }
                  else {
                     // if the job is running, check it's state age, if "too old" then stop and remove...     
                     Vector<Job> jobVec = jobQueue.getJob(jobId);
                     if (jobVec.size() == 1) {
                        Job aJob = jobVec.get(0);
                        long statusTimeStamp = aJob.getStatusTimeStamp();
                        if (statusTimeStamp > maxJobStateTime) {
                           jobQueue.cancelJob(jobId); // then try to stop it.
                           jobIdToStop.add(jobId);
                        }
                        // else let it run...
                     }
                     else {
                        logMessage(Level.WARNING, "Found a jobId '" + jobId + "' in the jobIdsToExclude map that is a process, expected on one job, found '"
                              + jobVec.size() + "' jobs.");
                     }
                  }
               }
               // else ignore it...
            }
         }
         // else no jobs to remove...

         // if there are jobIds that were stopped above, give them 30 secs and try to remove them...
         Thread.sleep((30000)); // wait 30 seconds for jobs to stop.
         if (!jobIdToStop.isEmpty()) {
            for (String jobId : jobIdList) {
               JobStatus jobStatus = jobQueue.getJobStatus(jobId);
               if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                  jobQueue.removeJob(jobId);
               }
               else {
                  logMessage(Level.WARNING, "While trying to clear the jobQueue, unable to stop job with id of '" + jobId + "'.");
               }
            }
         }
         // else no more jobs to remove...         
      }

   }
}
