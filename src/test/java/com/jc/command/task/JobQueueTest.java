package com.jc.command.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.jc.FakeDataGenerator;
import com.jc.command.task.Job.JobStatus;
import com.jc.log.Logger;

public final class JobQueueTest {

   /*
      This test creates 5 jobs, adds them to the jobQ and then mid-way through stops (cancels)
      one and then adds one while the jobs are running.
    */
   @Test
   @Ignore
   public void testJobQueueBasic() throws Exception {
      boolean testStoppedTwice = false;
      boolean testRemoveJob = false;
      testBasicJobQueue(testStoppedTwice, testRemoveJob);
   }

   /*
      Same as above but tries to stop an already stopped job.
    */
   @Test
   @Ignore
   public void testJobQueueStopJobTwice() throws Exception {
      boolean testStoppedTwice = true;
      boolean testRemoveJob = false;
      testBasicJobQueue(testStoppedTwice, testRemoveJob);
   }

   /*
      Same as above but tries to remove the stopped job.
    */
   @Test
   @Ignore
   public void testJobQueueRemoveJobWhileRunning() throws Exception {
      boolean testStoppedTwice = false;
      boolean testRemoveJob = true;
      testBasicJobQueue(testStoppedTwice, testRemoveJob);
   }

   @Test
   //   @Ignore
   public void testJobQueueWithUnstoppableJob() throws Exception {

      Logger.setConsoleLoggingOn(false);

      JobQueue jobQueue = new JobQueue();

      int numTestJobs = 5;
      List<String> testJobIds = new ArrayList<>();
      for (int i = 0; i < numTestJobs; i++) {
         if (i == Math.ceil(numTestJobs / 2)) {
            Job job = createTestJob(10000, 20000);
            System.out.println("Est Job[" + job.getID() + "] Runtime: [" + job.getEstimatedRuntime() + "].");
            jobQueue.queueJob(job);
            testJobIds.add(job.getID());
         }
         else {
            Job job = createTestJob(5000, 10000);
            System.out.println("Est Job[" + job.getID() + "] Runtime: [" + job.getEstimatedRuntime() + "].");
            jobQueue.queueJob(job);
            testJobIds.add(job.getID());
         }
      }

      // -------------------------------------------------------------

      int numTasks = FakeDataGenerator.getRandomInteger(5, 10);
      int maxRunnableThreads = FakeDataGenerator.getRandomInteger(1, numTasks + 1);
      List<TestTask> taskVector = JobTestUtils.createTestTasks(numTasks, "UnStoppableTestJob_", 100000, 150000);

      String unstoppableId = "UnStoppableTestJob";

      TestTask firstTask = taskVector.get(0);
      firstTask.Name = unstoppableId + "_" + firstTask.Name;
      UnstoppableJob unstoppableJob = new UnstoppableJob(firstTask, true);
      unstoppableJob.setIDPrefix(unstoppableId + "_");
      unstoppableJob.setMaxNumThreads(maxRunnableThreads);
      taskVector.get(numTasks / 2).throwException(false);

      for (int i = 1; i < numTasks; i++) {
         TestTask testTask = taskVector.get(i);
         testTask.Name = unstoppableId + "_" + testTask.Name;
         unstoppableJob.addTask(testTask);
      }

      System.out.println("Est UnstoppableJob[" + unstoppableJob.getID() + "] Runtime: [" + unstoppableJob.getEstimatedRuntime() + "].");

      // ---------------------------------------------------------------------------

      jobQueue.queueJob(unstoppableJob);
      String jobIdToStop = unstoppableJob.getID();
      //    testJobIds.add(unstoppableJob.getID());

      long time = System.currentTimeMillis();

      jobQueue.runJobs();

      boolean allDone = false;
      boolean jobStopped = false;

      while (!allDone) {
         int numNowRunning = 0;
         for (int i = 0; i < testJobIds.size(); i++) {
            String jobId = testJobIds.get(i);
            JobStatus jobStatus = jobQueue.getJobStatus(jobId);
            if (jobStatus == JobStatus.RUNNING) {
               numNowRunning++;
            }
         }

         boolean done = true;
         for (String jobId : testJobIds) {
            JobStatus jobStatus = jobQueue.getJobStatus(jobId);
            if (jobStatus.isQueued() || jobStatus.isWaiting() || jobStatus.isRunning()) {
               done = false;
               break;
            }
         }

         if (done) {
            allDone = true;
         }

         if (numNowRunning == 4 && !jobStopped) { // when 4 of the 5 jobs are still running...
            jobStopped = true;
            System.out.println("Canceling job '" + jobIdToStop + ".");
            jobQueue.cancelJob(jobIdToStop);
         }
      }

      System.out.println("Total Test Runtime: " + (System.currentTimeMillis() - time));

      for (String jobId : testJobIds) {
         System.out.println(jobId + " status: " + jobQueue.getJobStatus(jobId));
      }
      System.out.println(jobIdToStop + " status: " + jobQueue.getJobStatus(jobIdToStop));

      jobQueue.removeJob(jobIdToStop);
      Vector<Job> jobVec = jobQueue.getJob(jobIdToStop);
      if (jobVec != null && !jobVec.isEmpty()) {
         Assert.fail("Job '" + jobIdToStop + "' is still in JobQueue with status of '" + jobQueue.getJobStatus(jobIdToStop) + "'.");
      }
   }

   // --------------------------------------------------------------

   private void testBasicJobQueue(boolean testStoppedTwice, boolean testRemoveJob) throws Exception {

      Logger.setConsoleLoggingOn(false);

      JobQueue jobQueue = new JobQueue();

      int numTestJobs = 5;
      String jobIdToStop = "";
      List<String> testJobIds = new ArrayList<>();
      for (int i = 0; i < numTestJobs; i++) {
         if (i == Math.ceil(numTestJobs / 2)) {
            Job job = createTestJob(10000, 20000);
            System.out.println("Est Job[" + job.getID() + "] Runtime: [" + job.getEstimatedRuntime() + "].");
            jobQueue.queueJob(job);
            testJobIds.add(job.getID());
            jobIdToStop = job.getID();
         }
         else {
            Job job = createTestJob(5000, 10000);
            System.out.println("Est Job[" + job.getID() + "] Runtime: [" + job.getEstimatedRuntime() + "].");
            jobQueue.queueJob(job);
            testJobIds.add(job.getID());
         }
      }

      Job newJobToAdd = createTestJob(5000, 10000);
      System.out.println("Est NewJobToAdd[" + newJobToAdd.getID() + "] Runtime: [" + newJobToAdd.getEstimatedRuntime() + "].");

      // ---------------------------------------------------------------------------

      long time = System.currentTimeMillis();

      jobQueue.runJobs();

      boolean allDone = false;
      boolean newJobAdded = false;
      boolean jobStopped = false;

      while (!allDone) {
         int numNowRunning = 0;
         for (int i = 0; i < testJobIds.size(); i++) {
            String jobId = testJobIds.get(i);
            JobStatus jobStatus = jobQueue.getJobStatus(jobId);
            if (jobStatus == JobStatus.RUNNING) {
               numNowRunning++;
            }
         }

         JobStatus jobStatus = jobQueue.getJobStatus(jobIdToStop);

         if (testStoppedTwice) {
            // tests stopping a job when it has already stopped.... should silently ignore request and not error.
            if (jobStopped && JobStatus.isStopped(jobStatus)) {
               jobQueue.cancelJob(jobIdToStop);
               System.out.println("Stopping job '" + jobIdToStop + " again.");
               testStoppedTwice = true;
            }
         }

         if (testRemoveJob) {
            // tests removing a stopped job... it's valid to remove stopped, errored, or done jobs.
            if (jobStopped && JobStatus.isStopped(jobStatus)) {
               // try to remove a job before it is stopped.
               jobQueue.removeJob(jobIdToStop);
               System.out.println("Removed job '" + jobIdToStop + ".");
            }
         }

         if (numNowRunning == 0) {
            allDone = true;
         }
         else if (numNowRunning == 4 && !jobStopped) {
            jobStopped = true;
            System.out.println("Canceling job '" + jobIdToStop + ".");
            jobQueue.cancelJob(jobIdToStop);
         }
         else if (numNowRunning == 3 && !newJobAdded) {
            jobQueue.queueJob(newJobToAdd);
            testJobIds.add(newJobToAdd.getID());
            System.out.println("Added job '" + newJobToAdd.getID() + "' to queue.");
            jobQueue.runJobs();
            newJobAdded = true;
         }
      }

      System.out.println("Total Test Runtime: " + (System.currentTimeMillis() - time));

      if (testRemoveJob) {
         Vector<Job> jobVec = jobQueue.getJob(jobIdToStop);
         if (jobVec != null && !jobVec.isEmpty()) {
            Assert.fail("Job '" + jobIdToStop + "' is still in JobQueue with status of '" + jobQueue.getJobStatus(jobIdToStop) + "'.");
         }
      }

      for (String jobId : testJobIds) {
         System.out.println(jobId + " status: " + jobQueue.getJobStatus(jobId));
      }
   }

   private Job createTestJob(int minTaskRunTime, int maxTaskRunTime) {
      boolean multiThreaded = true;
      Job[] masters = null;
      boolean includeException = false;

      int num_tasks = FakeDataGenerator.getRandomInteger(5, 10);
      int maxRunnableThreads = FakeDataGenerator.getRandomInteger(1, num_tasks + 1);

      List<TestTask> task_vector = JobTestUtils.createTestTasks(num_tasks, "TestJob_", minTaskRunTime, maxTaskRunTime);
      Job job = JobTestUtils.createJob("TestJob", multiThreaded, maxRunnableThreads, masters, includeException, task_vector);
      return job;
   }

   //   public String getLogData(String job_id) throws Exception {
   //   public void removeJob(String job_id) throws Exception {
   //
   //   /**
   //    * Queues a group of jobs according to the supplied group's priority. It's assumed that the 
   //    * supplied jobs are an ordered list of jobs and no further processing based on each job's priority will be done.
   //    */
   //   public void queueJobs(Vector<Job> jobs, float priority) throws Exception {
   //
   //   /**
   //    * Queues job objects according to each one's priority.
   //    */
   //   public void queueJob(Job ajob) throws Exception {
   //
   //   public void stopJobs() throws Exception {
   //
   //   public void cancelJob(String job_id) throws Exception {
   //
   //   public JobStatus getJobStatus(String job_id) throws Exception {
   //
   //   public double getAmountCompleted(String job_id) throws Exception {
   //
   //   public String getStatusMessage(String job_id) throws Exception {
   //
   //   public long getEstimatedRuntime(String job_id) throws Exception {
   //
   //   /**
   //    * Returns a Vector object of Job objects associated to the id supplied. The id can be either a job id or a process id.
   //    */
   //   public Vector<Job> getJob(String job_id) throws Exception {
   //
   //
   //     Associates a real job_id with a virtual job_id so that if a virtual job_id is given to the JobQueue, 
   //     information referencing the real id will be returned. If the virtual_job_id already is associated 
   //     with a job_id, it will be re-associated with the supplied job_id.
   //
   //   public void associateJobID(String virtual_job_id, String job_id) throws Exception {
   //
   //   // Returns the associated job id if one exists.
   ////   String getJobIDForVirtualJobID(String virtual_job_id) {
   //
   //   public String getVirtualIDByJobID(String job_id) {
   //      
   //   protected void logMessage(Level level, String message) {
   //   protected void logMessage(Level level, String message, Throwable exc) {
   //
   //   // ------------------------ Code Supporting Processes ------------------------ //
   //
   //   public void registerAsAProcess(Vector<Job> process, String process_id) throws Exception {
   //
   //   // This method 'de-registers' and 'de-queues' the jobs
   //   // associated with the process_id
   //   public void removeProcess(String process_id) throws Exception {
   //
   //   public boolean isAProcess(String process_id) {
   //
   //   /*
   //    * Return the process id associated with the supplied job id if one
   //    * exits, otherwise return a null value.
   //    */
   //   public String getProcessID(String job_id) {
   //
   //   public Vector<Job> getJobsForProcess(String process_id) {
   //
   //   public JobStatus getStatusForProcess(String process_id) throws Exception {
   //
   //   public String getProcessLogData(String process_id) throws Exception {
   //
   //   public long getProcessEstimatedRuntime(String process_id) throws Exception {
   //
   //   public String getProcessStatusMessage(String process_id) throws Exception {
   //
   //   public double getProcessAmountCompleted(String process_id) throws Exception {
   //
   //   // -------------------- Implements JobStatusListener -------------------------- //
   //
   //   public void jobStatusChanged(String job_id, JobStatus old_status, JobStatus new_status) {

}
