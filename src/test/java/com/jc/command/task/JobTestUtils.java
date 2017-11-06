package com.jc.command.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.jc.command.task.Job.JobStatus;
import com.jc.command.task.Task.TaskStatus;

public final class JobTestUtils {

   private static final String ASSIGNED_PREFIX_NOT_FOUND = "Id did not contain the assigned prefix.";

   private static final String FAILED_TO_GET_ID = "Failed to get an ID for the test job.";
   private static final String FAILED_TO_GET_SLAVE_ID = "Failed to get an ID for the test slave.";

   private static final String NO_LOG_DATA_AVAILABLE = "No log data available from the test job.";
   private static final String NO_SLAVE_LOG_DATA_AVAILABLE = "No log data available from the test slave.";

   private static final String NO_STATUS_MESSAGE_AVAILABLE = "No status message available from the test job.";

   private static final String NO_SLAVE_STATUS_MESSAGE_AVAILABLE = "No status message available from the test slave.";

   public static void runJobSuccessfully(boolean multi_threaded, boolean slave_mode, boolean multi_task) throws Exception {
      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 7;
      }

      long total_est_runtime = 0l;

      String test_job_prefix = "TestJob_A";

      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), multi_threaded);
      test_job.setIDPrefix(test_job_prefix);
      test_job.setMaxNumThreads(num_threads);

      for (TestTask test_task : task_vector) {
         total_est_runtime += test_task.getEstimatedProcessingTime();
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      double init_amount_completed = test_job.getAmountCompleted();
      long init_est_runtime = test_job.getEstimatedRuntime();

      Assert.assertEquals("Expected UNKNOWN job status.", JobStatus.UNKNOWN, test_job.getState());
      Assert.assertEquals("Amount completed was greater than expected.", 0, init_amount_completed, 0);
      Assert.assertEquals("Estimated runtime before starting is not as expected.", total_est_runtime, init_est_runtime, 0);

      double prev_amount_completed = init_amount_completed;
      long prev_est_runtime = init_est_runtime;

      test_job.start();

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(500);

         double amount_completed = test_job.getAmountCompleted();
         Assert.assertFalse("Amount completed (" + amount_completed + ") was < previous amount completed (" + prev_amount_completed + ").",
               amount_completed < prev_amount_completed);
         prev_amount_completed = amount_completed;

         long est_runtime = test_job.getEstimatedRuntime();
         Assert.assertFalse("Estimated runtime (" + est_runtime + ") was > previous estimated runtime (" + prev_est_runtime + ").",
               est_runtime > prev_est_runtime);
         prev_est_runtime = est_runtime;

         String status_msg = test_job.getStatusMessage();
         Assert.assertNotNull(NO_STATUS_MESSAGE_AVAILABLE, status_msg);
         Assert.assertFalse(NO_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());

         // System.out.println("AC["+amount_completed+"] ERT["+est_runtime+"] SM["+status_msg+"]");
      }

      Assert.assertTrue("Test job [" + test_job.getID() + "] did not finish successfully.", test_job.isDone());
      // exercise this method
      Assert.assertTrue(test_job.isDone());

      double amount_completed = test_job.getAmountCompleted();
      Assert.assertFalse("Amount completed (" + test_job.getAmountCompleted() + ") was less than expected.", amount_completed < 1);
      Assert.assertFalse("Amount completed (" + amount_completed + ") was <= initial amount completed (" + init_amount_completed + ").",
            amount_completed <= init_amount_completed);

      long estimated_runtime = test_job.getEstimatedRuntime();
      Assert.assertEquals("Estimated runtime for successfully completed job is not 0.", 0, estimated_runtime);
      Assert.assertFalse("Estimated runtime (" + estimated_runtime + ") was >= initial estimated runtime (" + init_est_runtime + ").",
            estimated_runtime >= init_est_runtime);

      String id = test_job.getID();
      Assert.assertNotNull(FAILED_TO_GET_ID, id);
      Assert.assertFalse(FAILED_TO_GET_ID, id.isEmpty());
      Assert.assertTrue(ASSIGNED_PREFIX_NOT_FOUND, id.contains("TestJob_A"));

      String job_log_data = test_job.getLogData();
      Assert.assertNotNull(NO_LOG_DATA_AVAILABLE, job_log_data);
      Assert.assertFalse(NO_LOG_DATA_AVAILABLE, job_log_data.isEmpty());

      System.out.println("JOB LOG (for review) ------------");
      System.out.println(job_log_data);
      System.out.println();

      for (TestTask test_task : task_vector) {
         String task_log_data = test_job.getLogData(test_task.getID());
         String msg = "No task log data for task '" + test_task.getID() + "' available from the test job.";
         Assert.assertNotNull(msg, task_log_data);
         Assert.assertFalse(msg, task_log_data.isEmpty());
      }

      for (TestTask test_task : task_vector) {
         Assert.assertEquals("Not all the job's tasks reported that they completed successfully.", TaskStatus.DONE, test_task.getState());
      }

      System.out.println("**** Done with runJobSuccessfully test.");
      System.out.println();
   }

   public static void runJobWithException(boolean multi_threaded, boolean slave_mode, boolean multi_task) throws Exception {
      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 7;
      }

      String test_job_prefix = "TestJob_A";

      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), multi_threaded);
      test_job.setIDPrefix(test_job_prefix);
      test_job.setMaxNumThreads(num_threads);

      for (int i = 0; i < num_tasks; i++) {
         TestTask test_task = task_vector.get(i);

         if (!multi_task || (multi_task && i == 2)) { // third of four tasks throws an exception.
            test_task.throwException(true);
         }

         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      test_job.start();

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(500);
      }

      Assert.assertTrue("Expected test job not to finish successfully.", test_job.isErrored());
      // exercise this method
      Assert.assertTrue(test_job.isErrored());

      double amount_completed = test_job.getAmountCompleted();
      Assert.assertFalse("Amount completed (" + amount_completed + ") was greater than expected.", amount_completed >= 1);

      long estimated_runtime = test_job.getEstimatedRuntime();
      Assert.assertFalse("Estimated runtime (" + estimated_runtime + ") for errored job is less than expected.", estimated_runtime < 1);

      String status_msg = test_job.getStatusMessage();
      Assert.assertNotNull(NO_STATUS_MESSAGE_AVAILABLE, status_msg);
      Assert.assertFalse(NO_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());

      String job_log_data = test_job.getLogData();
      Assert.assertNotNull(NO_LOG_DATA_AVAILABLE, job_log_data);
      Assert.assertFalse(NO_LOG_DATA_AVAILABLE, job_log_data.isEmpty());

      System.out.println("JOB LOG (for review) ------------");
      System.out.println(job_log_data);
      System.out.println();

      for (TestTask test_task : task_vector) {
         // System.out.println("**** TASK[" + test_task.getID() + "] STATE[" + test_task.getState() + "]");
         if (test_task.isRunning() || test_task.isWaiting()) {
            Assert.fail("Task '" + test_task.getID() + "' was " + test_task.getState() + " and should have been done, errored, stopped or queued to run.");
         }
      }

      int num_errored = 0;
      for (TestTask test_task : task_vector) {
         if (test_task.isErrored()) {
            num_errored++;
         }
      }

      // Only one task should have errored.
      Assert.assertEquals("Total number of errored tasks was incorrect.", 1, num_errored);

      System.out.println("**** Done with runJobWithException test.");
      System.out.println();
   }

   public static void runJobWithStop(boolean multi_threaded, boolean slave_mode, boolean multi_task) throws Exception {
      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 8;
      }

      String test_job_prefix = "TestJob_A";

      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), multi_threaded);
      test_job.setIDPrefix(test_job_prefix);
      test_job.setMaxNumThreads(num_threads);

      for (TestTask test_task : task_vector) {
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      test_job.start();

      Thread.sleep(500);

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(500);
         if (test_job.getAmountCompleted() >= 0.72) {
            test_job.stop();
         }
      }

      Assert.assertTrue("Test job did not stop correctly.", test_job.isStopped());
      // exercise this method
      Assert.assertTrue(test_job.isStopped());

      double amount_completed = test_job.getAmountCompleted();
      Assert.assertFalse("Amount completed (" + amount_completed + ") was greater than expected.", amount_completed >= 1);

      long estimated_runtime = test_job.getEstimatedRuntime();
      Assert.assertFalse("Estimated runtime (" + estimated_runtime + ") for errored job is less than expected.", estimated_runtime < 1);

      String status_msg = test_job.getStatusMessage();
      Assert.assertNotNull(NO_STATUS_MESSAGE_AVAILABLE, status_msg);
      Assert.assertFalse(NO_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());

      String job_log_data = test_job.getLogData();
      Assert.assertNotNull(NO_LOG_DATA_AVAILABLE, job_log_data);
      Assert.assertFalse(NO_LOG_DATA_AVAILABLE, job_log_data.isEmpty());

      System.out.println("JOB LOG (for review) ------------");
      System.out.println(job_log_data);
      System.out.println();

      for (TestTask test_task : task_vector) {
         // System.out.println("**** TASK["+test_task.getID()+"] STATE["+Task.translateStateCode(test_task.State)+"]");
         if (test_task.isRunning() || test_task.isWaiting()) {
            Assert.fail("Task '" + test_task.getID() + "' was " + test_task.getState() + " and should have been done, errored, stopped or queued to run.");
         }
      }

      int num_stopped = 0;
      for (TestTask test_task : task_vector) {
         if (test_task.isInterrupted()) {
            num_stopped++;
         }
      }

      Assert.assertNotEquals("Total number of stopped tasks (" + num_stopped + ") was incorrect, should have been > 0.", 0, num_stopped);

      System.out.println("**** Done with runJobWithStop test.");
      System.out.println();
   }

   public static void testJobStatusListener(boolean multi_threaded, boolean slave_mode) throws Exception {
      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 4;

      List<TestTask> task_vector = createTestTasks(num_tasks, null);

      TestJobStatusListener listenerA = new TestJobStatusListener();
      TestTask test_taskA = task_vector.get(0);
      Job test_jobA = new Job(test_taskA, multi_threaded);
      test_jobA.setMaxNumThreads(num_threads);
      test_jobA.addJobStatusListener(listenerA);

      TestJobStatusListener listenerB = new TestJobStatusListener();
      TestTask test_taskB = task_vector.get(1);
      Job test_jobB = new Job(test_taskB, multi_threaded);
      test_jobB.setMaxNumThreads(num_threads);
      test_jobB.addJobStatusListener(listenerB);

      TestJobStatusListener listenerC = new TestJobStatusListener();
      TestTask test_taskC = task_vector.get(2);
      test_taskC.throwException(true);
      Job test_jobC = new Job(test_taskC, multi_threaded);
      test_jobC.setMaxNumThreads(num_threads);
      test_jobC.addJobStatusListener(listenerC);

      test_jobA.start();
      test_jobB.start();
      test_jobC.start();

      long sleep_time = 0;
      while (test_taskA.isRunning() || test_taskA.isNotRunning() || test_taskB.isRunning() || test_taskB.isNotRunning() || test_taskC.isRunning()
            || test_taskC.isNotRunning()) {
         Thread.sleep(500);
         sleep_time += 500;

         if (sleep_time > 2768) {
            test_jobB.stop();
         }
      }

      TestJobInfo infoA = listenerA.getTaskInfo(test_jobA.getID());
      TestJobInfo infoB = listenerB.getTaskInfo(test_jobB.getID());
      TestJobInfo infoC = listenerC.getTaskInfo(test_jobC.getID());

      // Test that the TaskListener gets the correct TaskID...
      if (!test_jobA.getID().equalsIgnoreCase(infoA.getJobID()) && !test_jobB.getID().equalsIgnoreCase(infoB.getJobID())
            && !test_jobC.getID().equalsIgnoreCase(infoC.getJobID())) {
         Assert.fail("Invalid job id reported by JobStatusListener.");
      }

      // Test that the JobStatusListener gets a DONE status.
      if (!(infoA.getOldStatus().isRunning() && infoA.getNewStatus().isDone())) {
         Assert.fail("Old (" + infoA.getOldStatus() + ") and new (" + infoA.getNewStatus() + ") job status not valid for test job A.");
      }

      // Test that the JobStatusListener gets a STOPPED status.
      if (!(infoB.getOldStatus().isRunning() && infoB.getNewStatus().isStopped())) {
         Assert.fail("Old (" + infoB.getOldStatus() + ") and new (" + infoB.getNewStatus() + ") job status not valid for test job B.");
      }

      // Test that the JobStatusListener gets an ERRORED status.
      if (!(infoC.getOldStatus().isRunning() && infoC.getNewStatus().isErrored())) {
         Assert.fail("Old (" + infoC.getOldStatus() + ") and new (" + infoC.getNewStatus() + ") job status not valid for test job C.");
      }

      System.out.println("**** Done with testJobStatusListener test.");
      System.out.println();
   }

   public static void runSlaveJobSuccessfully(boolean multi_threaded, boolean multi_task) throws Exception {
      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 7;
      }

      String test_job_prefix = "TestSlaveJob";

      // Create master jobs...
      boolean include_exceptions = false;
      Job[] masters = JobTestUtils.createJobs(5, "Master", multi_threaded, num_threads, include_exceptions);

      // Create slave job...
      long total_est_runtime = 0l;

      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), masters, multi_threaded);
      test_job.setIDPrefix(test_job_prefix + "_");
      test_job.setMaxNumThreads(num_threads);

      for (TestTask test_task : task_vector) {
         total_est_runtime += test_task.getEstimatedProcessingTime();
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      // Slave should be in an UNKNOWN state having just been created...
      Assert.assertEquals("Test slave expected to be in UNKNOWN state.", JobStatus.UNKNOWN, test_job.getState());

      // Initial amount completed should be 0 since nothing has started.
      double init_amount_completed = test_job.getAmountCompleted();
      Assert.assertEquals("Amount completed was greater than expected.", 0, init_amount_completed, 0);

      // Initial estimated runtime should be the same as calculated above
      // 'total_est_runtime'.
      long init_est_runtime = test_job.getEstimatedRuntime();
      Assert.assertEquals("Estimated runtime before starting is not as expected.", total_est_runtime, init_est_runtime);

      double prev_amount_completed = init_amount_completed;
      long prev_est_runtime = init_est_runtime;

      // Start slave...
      test_job.start();

      // Wait a 1/10th of a second before continuing...
      Thread.sleep(100);

      // Slave should be not be queued... should be running in wait state...
      Assert.assertTrue("Test slave expected to be in WAITING state.", test_job.isWaiting());
      // exercise this method
      Assert.assertTrue(test_job.isWaiting());

      // Start master jobs...
      for (Job master : masters) {
         master.start();
         Thread.sleep(Math.round(Math.random() * 100) * 5);
      }

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(500);

         double amount_completed = test_job.getAmountCompleted();
         Assert.assertFalse("Amount completed (" + amount_completed + ") was < previous amount completed (" + prev_amount_completed + ").",
               amount_completed < prev_amount_completed);
         prev_amount_completed = amount_completed;

         long est_runtime = test_job.getEstimatedRuntime();
         Assert.assertFalse("Estimated runtime (" + est_runtime + ") was > previous estimated runtime (" + prev_est_runtime + ").",
               est_runtime > prev_est_runtime);
         prev_est_runtime = est_runtime;

         String status_msg = test_job.getStatusMessage();
         Assert.assertNotNull(NO_SLAVE_STATUS_MESSAGE_AVAILABLE + " state[" + test_job.getState() + "] amount_completed[" + amount_completed + "] est_runtime["
               + est_runtime + "]", status_msg);
         Assert.assertFalse(NO_SLAVE_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());
      }

      Assert.assertTrue("Test slave did not finish successfully; state[" + test_job.getState() + "].", test_job.isDone());
      Assert.assertTrue(test_job.isDone());

      double amount_completed = test_job.getAmountCompleted();
      Assert.assertFalse("Amount completed (" + amount_completed + ") was less than expected.", amount_completed < 1);
      Assert.assertFalse("Amount completed (" + amount_completed + ") was <= initial amount completed (" + init_amount_completed + ").",
            amount_completed <= init_amount_completed);

      long estimated_runtime = test_job.getEstimatedRuntime();
      Assert.assertEquals("Estimated runtime for successfully completed job is not 0.", 0, estimated_runtime);
      Assert.assertFalse("Estimated runtime (" + estimated_runtime + ") was >= initial estimated runtime (" + init_est_runtime + ").",
            estimated_runtime >= init_est_runtime);

      String id = test_job.getID();
      Assert.assertNotNull(FAILED_TO_GET_SLAVE_ID, id);
      Assert.assertFalse(FAILED_TO_GET_SLAVE_ID, id.isEmpty());
      Assert.assertTrue(ASSIGNED_PREFIX_NOT_FOUND, id.contains(test_job_prefix));

      String job_log_data = test_job.getLogData();
      Assert.assertNotNull(NO_SLAVE_LOG_DATA_AVAILABLE, job_log_data);
      Assert.assertFalse(NO_SLAVE_LOG_DATA_AVAILABLE, job_log_data.isEmpty());

      System.out.println("JOB LOG (for review) ------------");
      System.out.println(job_log_data);
      System.out.println();

      for (TestTask test_task : task_vector) {
         String task_log_data = test_job.getLogData(test_task.getID());
         String msg = "No task log data for task '" + test_task.getID() + "' available from the test slave.";
         Assert.assertNotNull(msg, task_log_data);
         Assert.assertFalse(msg, task_log_data.isEmpty());
      }

      for (TestTask test_task : task_vector) {
         Assert.assertTrue(
               "Not all the job's tasks reported that they completed successfully; taskID[" + test_task.getID() + "] taskState[" + test_task.getState() + "]",
               test_task.isDone());
      }

      System.out.println("**** Done with runJobSuccessfully test.");
      System.out.println();
   }

   public static void runSlaveJobWithMasterExceptionA(boolean multi_threaded, boolean multi_task) throws Exception {
      boolean masters_required_to_finish = true; // all the masters must finish for the slave to begin.

      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 7;
      }

      String test_job_prefix = "TestSlaveJob";

      // Create master jobs where one throws an exception...
      boolean include_exceptions = true;
      Job[] masters = JobTestUtils.createJobs(5, "Master", multi_threaded, num_threads, include_exceptions);

      // Create slave job...
      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), masters, multi_threaded);
      test_job.setIDPrefix(test_job_prefix + "_");
      test_job.setMaxNumThreads(num_threads);
      test_job.requireMastersFinish(masters_required_to_finish);

      for (TestTask test_task : task_vector) {
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      long initial_estimated_runtime = test_job.getEstimatedRuntime();

      // Start slave...
      test_job.start();

      Assert.assertTrue("Expected 'all masters must finish' constraint to be true.", test_job.mastersMustFinish());

      // Start master jobs...
      for (Job master : masters) {
         master.start();
         Thread.sleep(Math.round(Math.random() * 100) * 5);
      }

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(200);
      }

      double amount_completed = test_job.getAmountCompleted();
      long estimated_runtime = test_job.getEstimatedRuntime();
      String status_msg = test_job.getStatusMessage();

      Assert.assertTrue("Test slave was not stopped as expected; status[" + test_job.getState() + "].", test_job.isStopped());
      Assert.assertFalse("Amount completed (" + amount_completed + ") was greater than expected.", amount_completed >= 1);
      Assert.assertEquals("Estimated runtime (" + estimated_runtime + ") should be the same as before slave started.", initial_estimated_runtime,
            estimated_runtime);
      Assert.assertNotNull(NO_STATUS_MESSAGE_AVAILABLE, status_msg);
      Assert.assertFalse(NO_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());

      // Because a dependent job was stopped or errored, the slave job never ran 
      // and therefore should not have any log data to display.
      String job_log_data = test_job.getLogData();
      Assert.assertTrue("Test slave had log data: " + job_log_data, job_log_data == null || job_log_data.isEmpty());

      // All the slave's tasks should have been stopped.
      int num_interrupted = 0;
      for (TestTask test_task : task_vector) {
         //System.out.println("**** TASK[" + test_task.getID() + "] STATE[" + test_task.getState() + "]");
         if (test_task.isInterrupted()) { // if (test_task.notStarted()) {
            num_interrupted++;
         }
      }

      Assert.assertEquals("Total number of interrupted tasks was incorrect.", num_tasks, num_interrupted);

      //     System.out.println("**** Done with runSlaveJobWithMasterExceptionA test.");
   }

   public static void runSlaveJobWithMasterExceptionB(boolean multi_threaded, boolean multi_task) throws Exception {
      boolean masters_required_to_finish = false; // NOT all the masters have to finish for the slave to begin.

      int num_threads = 1;
      if (multi_threaded) {
         num_threads = 4;
      }

      int num_tasks = 1;
      if (multi_task) {
         num_tasks = 7;
      }

      String test_job_prefix = "TestSlaveJob";

      // Create master jobs where one throws an exception...
      boolean include_exceptions = true;
      Job[] masters = JobTestUtils.createJobs(5, "Master", multi_threaded, num_threads, include_exceptions);

      // Create slave job...
      long total_est_runtime = 0l;

      List<TestTask> task_vector = createTestTasks(num_tasks, test_job_prefix);
      Job test_job = new Job(task_vector.get(0), masters, multi_threaded);
      test_job.setIDPrefix(test_job_prefix + "_");
      test_job.setMaxNumThreads(num_threads);
      test_job.requireMastersFinish(masters_required_to_finish);

      for (TestTask test_task : task_vector) {
         total_est_runtime += test_task.getEstimatedProcessingTime();
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         test_job.addTask(test_task);
      }

      // Slave should be in an UNKNOWN state having just been created...
      Assert.assertEquals("Test slave expected to be in UNKNOWN state.", JobStatus.UNKNOWN, test_job.getState());

      // Initial amount completed should be 0 since nothing has started.
      double init_amount_completed = test_job.getAmountCompleted();
      Assert.assertEquals("Amount completed was greater than expected.", 0, init_amount_completed, 0);

      // Initial estimated runtime should be the same as calculated above 'total_est_runtime'.
      long init_est_runtime = test_job.getEstimatedRuntime();
      Assert.assertEquals("Estimated runtime before starting is not as expected.", total_est_runtime, init_est_runtime);

      double prev_amount_completed = init_amount_completed;
      long prev_est_runtime = init_est_runtime;

      // Start slave...
      test_job.start();

      // Wait a 1/10th of a second before continuing...
      Thread.sleep(10);

      Assert.assertFalse("Expected 'all masters must finish' constraint to be false.", test_job.mastersMustFinish());

      // Slave should be not be queued... should be running in wait state...
      Assert.assertTrue("Test slave expected to be in WAITING state.", test_job.isWaiting());
      // exercise this method
      Assert.assertTrue(test_job.isWaiting());

      // Start master jobs...
      for (Job master : masters) {
         master.start();
         long sleep_time = Math.round(Math.random() * 100);
         Thread.sleep(sleep_time * 5);
      }

      while (test_job.isRunning() || test_job.isQueued() || test_job.isWaiting()) {
         Thread.sleep(500);

         double amount_completed = test_job.getAmountCompleted();
         Assert.assertFalse("Amount completed (" + amount_completed + ") was < previous amount completed (" + prev_amount_completed + ").",
               amount_completed < prev_amount_completed);
         prev_amount_completed = amount_completed;

         long est_runtime = test_job.getEstimatedRuntime();
         Assert.assertFalse("Estimated runtime (" + est_runtime + ") was > previous estimated runtime (" + prev_est_runtime + ").",
               est_runtime > prev_est_runtime);
         prev_est_runtime = est_runtime;

         String status_msg = test_job.getStatusMessage();
         Assert.assertNotNull(NO_SLAVE_STATUS_MESSAGE_AVAILABLE + " state[" + test_job.getState() + "] amount_completed[" + amount_completed + "] est_runtime["
               + est_runtime + "]", status_msg);
         Assert.assertFalse(NO_SLAVE_STATUS_MESSAGE_AVAILABLE, status_msg.isEmpty());
      }

      Assert.assertTrue("Test slave did not finish successfully.", test_job.isDone());
      // exercise this method
      Assert.assertTrue(test_job.isDone());

      double amount_completed = test_job.getAmountCompleted();
      Assert.assertFalse("Amount completed (" + amount_completed + ") was less than expected.", amount_completed < 1);
      Assert.assertFalse("Amount completed (" + amount_completed + ") was <= initial amount completed (" + init_amount_completed + ").",
            amount_completed <= init_amount_completed);

      long estimated_runtime = test_job.getEstimatedRuntime();
      Assert.assertEquals("Estimated runtime for successfully completed job is not 0.", 0, estimated_runtime);
      Assert.assertFalse("Estimated runtime (" + estimated_runtime + ") was >= initial estimated runtime (" + init_est_runtime + ").",
            estimated_runtime >= init_est_runtime);

      String id = test_job.getID();
      Assert.assertNotNull(FAILED_TO_GET_SLAVE_ID, id);
      Assert.assertFalse(FAILED_TO_GET_SLAVE_ID, id.isEmpty());
      Assert.assertTrue(ASSIGNED_PREFIX_NOT_FOUND, id.contains(test_job_prefix));

      String job_log_data = test_job.getLogData();
      Assert.assertNotNull(NO_SLAVE_LOG_DATA_AVAILABLE, job_log_data);
      Assert.assertFalse(NO_SLAVE_LOG_DATA_AVAILABLE, job_log_data.isEmpty());

      System.out.println("JOB LOG (for review) ------------");
      System.out.println(job_log_data);
      System.out.println();

      for (TestTask test_task : task_vector) {
         String task_log_data = test_job.getLogData(test_task.getID());
         String msg = "No task log data for task '" + test_task.getID() + "' available from the test slave.";
         Assert.assertNotNull(msg, task_log_data);
         Assert.assertFalse(msg, task_log_data.isEmpty());
      }

      for (TestTask test_task : task_vector) {
         Assert.assertTrue("Not all the job's tasks reported that they completed successfully.", test_task.isDone());
      }

      System.out.println("**** Done with runSlaveJobWithMasterExceptionB test.");
      System.out.println();
   }

   static List<TestTask> createTestTasks(int num_tasks, String prefix) {
      return createTestTasks(num_tasks, prefix, 4000, 8500);
   }

   static List<TestTask> createTestTasks(int num_tasks, String prefix, int minRunTime, int maxRunTime) {
      long multiplier = calcMultiplier(maxRunTime);
      List<TestTask> testTasks = new ArrayList<>();
      for (int i = 0; i < num_tasks; i++) {
         long runtime;
         do {
            runtime = (int) Math.round(Math.random() * multiplier);
         } while (runtime < minRunTime || runtime > maxRunTime);

         TestTask task = new TestTask("Task" + i, runtime);
         if (prefix != null) {
            task.setIDPrefix(prefix);
         }
         testTasks.add(task);
      }
      return testTasks;
   }

   static Job createJob(String job_id, boolean multi_threaded, int max_threads, Job[] masters, boolean include_exception) {
      return createJob(job_id, multi_threaded, max_threads, masters, include_exception, null);
   }

   static Job createJob(String job_id, boolean multi_threaded, int max_threads, Job[] masters, boolean include_exception, List<TestTask> task_vector) {
      Job ajob;

      if (task_vector == null) {
         int num_tasks = (int) Math.round(Math.random() * 10);
         while (num_tasks < 5 || num_tasks == 10) {
            num_tasks = (int) Math.round(Math.random() * 10);
         }

         task_vector = JobTestUtils.createTestTasks(num_tasks, job_id + "_");
      }

      int num_tasks = task_vector.size();

      TestTask first_task = task_vector.get(0);
      first_task.Name = job_id + "_" + first_task.Name;
      if (masters == null) {
         ajob = new Job(first_task, multi_threaded);
      }
      else {
         ajob = new Job(first_task, masters, multi_threaded);
      }
      ajob.setIDPrefix(job_id + "_");

      ajob.setMaxNumThreads(max_threads);
      task_vector.get(num_tasks / 2).throwException(include_exception);

      for (TestTask test_task : task_vector) {
         if (test_task.equals(task_vector.get(0))) {
            continue;
         }
         else {
            test_task.Name = job_id + "_" + test_task.Name;
         }

         ajob.addTask(test_task);
      }

      return ajob;
   }

   static Job[] createJobs(int num_jobs, String prefix, boolean multi_threaded, int max_threads, boolean include_exception) {
      int exception_index = (int) Math.round(Math.random() * (num_jobs - 1));
      ArrayList<Job> jobslist = new ArrayList<>();
      for (int i = 0; i < num_jobs; i++) {
         String jobId = prefix + i;
         jobslist.add(createJob(jobId, multi_threaded, max_threads, null, i == exception_index ? include_exception : false));
      }

      Job[] jobs = jobslist.toArray(new Job[jobslist.size()]);
      return (jobs);
   }

   private static long calcMultiplier(long value) {
      String strValue = String.valueOf(value);
      int numZeros = strValue.length() - 1;

      StringBuffer sb = new StringBuffer("" + 1);
      for (int i = 0; i < numZeros; i++) {
         sb.append(0);
      }

      long roundedNumber = Long.parseLong(sb.toString());
      return roundedNumber;
   }

   static final class TestJobInfo {

      private String JobID;
      private JobStatus OldStatus;
      private JobStatus NewStatus;

      public String getJobID() {
         return JobID;
      }

      public JobStatus getOldStatus() {
         return OldStatus;
      }

      public void setOldStatus(JobStatus status) {
         OldStatus = status;
      }

      public JobStatus getNewStatus() {
         return NewStatus;
      }

      public void setNewStatus(JobStatus status) {
         NewStatus = status;
      }
   }

   static final class TestJobStatusListener implements JobStatusListener {

      Map<String, TestJobInfo> Info = new HashMap<>();

      public TestJobInfo getTaskInfo(String job_id) {
         TestJobInfo job_info = new TestJobInfo();
         if (Info.containsKey(job_id)) {
            job_info = Info.get(job_id);
         }
         else {
            job_info.JobID = job_id;
            Info.put(job_id, job_info);
         }
         return job_info;
      }

      @Override
      public void jobStatusChanged(String jobId, JobStatus oldStatus, JobStatus newStatus) {
         TestJobInfo info = getTaskInfo(jobId);
         info.setNewStatus(newStatus);
         info.setOldStatus(oldStatus);
      }
   }
}
