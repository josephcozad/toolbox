package com.jc.command.task;

import java.util.List;

/*
 *   A convenience class that wraps a single Job object to run it. Depending on
 *   the settings, the job's tasks can be run either asynchronously or synchronously;
 *   also the number of tasks that are run at any one time can also be set.
 *   Note that this class does not run as a thread. Use JobQueue to 
 *   run one or more Jobs each in their own threads.
 */

public class JobRunner {

   private String jobName;

   private boolean runMultiThreaded;
   private int maxTasksRun;
   private List<Task> jobTasks;

   private Job myJob;

   public JobRunner() {
      // by default run all tasks asynchronously
      runMultiThreaded = true;
      maxTasksRun = 0;
   }

   public void setJobTasks(List<Task> jobTasks) {
      this.jobTasks = jobTasks;
   }

   public void setRunMultiThreaded(boolean runMultiThreaded) {
      this.runMultiThreaded = runMultiThreaded;
      if (!runMultiThreaded) {
         maxTasksRun = 1;
      }
   }

   public void setMaxParallelTasks(int maxTasksRun) {
      if (!runMultiThreaded) {
         runMultiThreaded = true;
      }
      this.maxTasksRun = maxTasksRun;
   }

   public List<? extends Object> run() throws Exception {
      if (jobTasks != null && !jobTasks.isEmpty()) {
         myJob = new Job(jobTasks.get(0), runMultiThreaded);
         myJob.setIDPrefix(jobName);

         if (!runMultiThreaded) {
            maxTasksRun = 1; // force to run all tasks in one thread only (synchronously)...
         }

         if (maxTasksRun > 0) {
            myJob.setMaxNumThreads(maxTasksRun);
         }
         // run all tasks at the same time.

         for (Task jobTask : jobTasks) {
            if (jobTask.equals(jobTasks.get(0))) {
               continue;
            }
            myJob.addTask(jobTask);
         }

         long estRunTime = myJob.getEstimatedRuntime();
         if (estRunTime < 1001) {
            estRunTime = 1000;
         }
         else {
            estRunTime = Math.round(estRunTime * 0.75);
         }

         //      estRunTime = 60000;

         myJob.start();

         while (myJob.isRunning() || myJob.isQueued() || myJob.isWaiting()) {
            try {
               Thread.sleep(estRunTime);

               estRunTime = myJob.getEstimatedRuntime();
               if (estRunTime < 1001) {
                  estRunTime = 1000;
               }
               else {
                  estRunTime = Math.round(estRunTime * 0.75);
               }

               //               estRunTime = 60000;
               //               DecimalFormat df = new DecimalFormat("0.00");
               //               System.out.println("AC[" + df.format(myJob.getAmountCompleted()) + "] ~RT[" + myJob.getEstimatedRuntime() + "]ms. SM["
               //                     + myJob.getStatusMessage() + "]");

            }
            catch (InterruptedException e) {
               // eat it for now...
            }
         }

         return myJob.getResult();
      }
      else {
         throw new Exception("No job tasks exist to run.");
      }
   }

   public boolean hasErrors() {
      return myJob.isErrored();
   }

   public String getLogData() {
      return myJob.getLogData();
   }
}
