package com.jc.command.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.jc.command.task.Task.TaskStatus;

public class TaskTester {

   @Test
   public void runTaskSuccessfully() throws Exception {
      TestTaskListener listener = new TestTaskListener();

      List<TestTask> tasks = JobTestUtils.createTestTasks(1, null);
      TestTask test_task = tasks.get(0);
      test_task.addTaskListener(listener);

      if (test_task.getState() != TaskStatus.NOT_RUNNING) {
         Assert.fail("Task was not in a 'not started' state; state was '" + test_task.getState() + "'.");
      }

      test_task.start();

      // Have to wait at least 20ms. before testing if status tracking started...
      Thread.sleep(20);

      if (test_task.statusTrackingStarted()) {
         Assert.fail("When testing 'statusTrackingStarted()', status tracking was on and should have been off.");
      }

      while (!(test_task.isDone() || test_task.isInterrupted() || test_task.isErrored())) {
         Thread.sleep(500);
      }

      if (test_task.statusTrackingStopped()) {
         Assert.fail("When testing 'statusTrackingStopped()', status tracking was on and should have been off.");
      }

      if (test_task.getState() != TaskStatus.DONE) {
         Assert.fail("Test task object did not finish successfully; state was '" + test_task.getState() + "'.");
      }

      TestTaskInfo info = listener.getTaskInfo(test_task.getID());
      double amountCompleted = info.getAmountCompleted();

      if (amountCompleted < 1d) {
         Assert.fail("Amount completed (" + amountCompleted + ") was less than expected.");
      }

      Assert.assertNotNull("No result value was created.", info.getResultValue());
      Assert.assertFalse("No result value was created.", info.getResultValue().isEmpty());

      // If logging... this will wait a bit extra to write everything out to a log file.
      Thread.sleep(1000);
   }

   @Test
   public void runTaskWithException() throws Exception {
      TestTaskListener listener = new TestTaskListener();

      List<TestTask> tasks = JobTestUtils.createTestTasks(1, null);
      TestTask test_task = tasks.get(0);
      test_task.addTaskListener(listener);
      test_task.throwException(true);
      test_task.setStatusTrackingOn(true);
      test_task.start();

      while (!(test_task.isDone() || test_task.isInterrupted() || test_task.isErrored())) {
         Thread.sleep(500);
      }

      if (test_task.getState() != TaskStatus.ERRORED) {
         Assert.fail("Test task object did not error correctly; state was '" + test_task.getState() + "'.");
      }

      if (!test_task.statusTrackingStopped()) {
         Assert.fail("When testing 'statusTrackingStopped()', status tracked was not stopped as expected.");
      }

      TestTaskInfo info = listener.getTaskInfo(test_task.getID());
      double amountCompleted = info.getAmountCompleted();

      if (amountCompleted > 1d) {
         Assert.fail("Amount completed (" + amountCompleted + ") was greater than expected.");
      }

      Assert.assertNull("Result value was created.", info.getResultValue());
      Assert.assertTrue("Errored status did not report Exception message.", info.getStatus().contains(TestTask.EXCEPTION_MESSAGE));
   }

   @Test
   public void runTaskWithForceInterrupt() throws Exception {
      TestTaskListener listener = new TestTaskListener();

      List<TestTask> tasks = JobTestUtils.createTestTasks(1, null);
      TestTask test_task = tasks.get(0);
      test_task.addTaskListener(listener);
      test_task.setStatusTrackingOn(true);

      long estimated_runtime = test_task.getEstimatedProcessingTime();
      test_task.start();

      long sleep_time = 0;
      while (!(test_task.isDone() || test_task.isInterrupted() || test_task.isErrored())) {
         Thread.sleep(500);
         sleep_time += 500;

         if (sleep_time > estimated_runtime * 0.5) {
            test_task.interrupt();
         }
      }

      // This is a "wait" to allow the task to finish clean up... not pretty... but effective.
      Thread.sleep(10);

      if (test_task.getState() != TaskStatus.INTERRUPTED) {
         Assert.fail("Test task object did not interrupt correctly; state was '" + test_task.getState() + "'.");
      }

      if (!test_task.statusTrackingStopped()) {
         Assert.fail("When testing 'statusTrackingStopped()', status tracked was not stopped as expected.");
      }

      TestTaskInfo info = listener.getTaskInfo(test_task.getID());
      double amountCompleted = info.getAmountCompleted();

      if (amountCompleted > 1d) {
         Assert.fail("Amount completed (" + amountCompleted + ") was greater than expected.");
      }

      String status = info.getStatus();
      Assert.assertNull("Result value was created.", info.getResultValue());
      Assert.assertTrue("Stopped task status message was incorrect; MSG:" + status + ".", status.contains(Task.INTERRUPTED_TASK_STATUS_MSG));
   }

   @Test
   public void testTaskListener() throws Exception {
      TestTaskListener listenerA = new TestTaskListener();

      List<TestTask> tasks = JobTestUtils.createTestTasks(3, null);
      TestTask test_taskA = tasks.get(0);
      test_taskA.addTaskListener(listenerA);
      test_taskA.start();

      TestTaskListener listenerB = new TestTaskListener();

      TestTask test_taskB = tasks.get(1);
      test_taskB.addTaskListener(listenerB);
      test_taskB.throwException(true);
      test_taskB.start();

      TestTaskListener listenerC = new TestTaskListener();

      TestTask test_taskC = tasks.get(2);
      test_taskC.addTaskListener(listenerC);

      long estproctimeC = test_taskC.getEstimatedProcessingTime();
      test_taskC.start();

      long sleep_time = 0;
      while (!(test_taskA.isDone() || test_taskA.isInterrupted() || test_taskA.isErrored())
            || !(test_taskB.isDone() || test_taskB.isInterrupted() || test_taskB.isErrored())
            || !(test_taskC.isDone() || test_taskC.isInterrupted() || test_taskC.isErrored())) {
         Thread.sleep(500);
         sleep_time += 500;

         if (sleep_time > estproctimeC * 0.63) {
            test_taskC.interrupt();
         }
      }

      TestTaskInfo infoA = listenerA.getTaskInfo(test_taskA.getID());
      TestTaskInfo infoB = listenerB.getTaskInfo(test_taskB.getID());
      TestTaskInfo infoC = listenerC.getTaskInfo(test_taskC.getID());

      // Test that the TaskListener gets the correct TaskID...
      if (!infoA.getTaskID().equals(test_taskA.getID())) {
         Assert.fail("TaskA id '" + test_taskA.getID() + "' did not match task info '" + infoA.getTaskID() + "'.");
      }

      if (!infoA.getTaskID().equals(test_taskA.getID())) {
         Assert.fail("TaskB id '" + test_taskB.getID() + "' did not match task info '" + infoB.getTaskID() + "'.");
      }

      if (!infoA.getTaskID().equals(test_taskA.getID())) {
         Assert.fail("TaskC id '" + test_taskC.getID() + "' did not match task info '" + infoC.getTaskID() + "'.");
      }

      // Test that the TaskListener gets a status of some sort...
      if ((infoA.getStatus() == null || infoA.getStatus().length() < 1) && (infoB.getStatus() == null || infoB.getStatus().length() < 1)
            && (infoC.getStatus() == null || infoC.getStatus().length() < 1)) {
         Assert.fail("No status reported by TaskListener.");
      }

      // Test that the TaskListener has a result value if the task finishes successfully.
      if (infoA.getResultValue() == null && infoB.getResultValue() != null && infoC.getResultValue() != null) {
         Assert.fail("Incorrect result value reported by TaskListener.");
      }

      // Test that the TaskListener associated with the interrupted task is notified of the interruption.
      Assert.assertTrue("Interrupted state not reported by TaskListener.", infoB.isInterrupted());
      Assert.assertTrue("Interrupted state was not reported by TaskListener as Errored.", infoB.isErrored());

      // Test that the TaskListener associated with the interrupted task is notified of the interruption.
      Assert.assertTrue("Interrupted state not reported by TaskListener [" + infoC.getStatus() + "].", infoC.isInterrupted());
      Assert.assertTrue("Interrupted state was not reported by TaskListener as Stopped.", infoC.isStopped());
   }

   @Test
   public void testOneTaskListenerWithMultipleTasks() throws Exception {
      TestTaskListener listener = new TestTaskListener();

      List<TestTask> tasks = JobTestUtils.createTestTasks(4, null);

      TestTask test_taskA = tasks.get(0);
      test_taskA.addTaskListener(listener);

      TestTask test_taskB = tasks.get(1);
      test_taskA.setNextCommand(test_taskB);

      TestTask test_taskC = tasks.get(2);
      test_taskC.throwException(true);
      test_taskA.setNextCommand(test_taskC); // test_taskC will run after test_taskA

      TestTask test_taskD = tasks.get(3);
      test_taskA.setNextCommand(test_taskD); // test_taskD will run after test_taskC

      test_taskA.start();

      // NOTE: Because test_taskC and test_taskD are chained to test_taskA and
      // because test_taskC throws an exception, test_taskD never runs.
      // Rule: chained tasks are dependent on each other to run.
      // So the while loop does not wait for test_taskD to run as it does for the others.
      while (!(test_taskA.isDone() || test_taskA.isInterrupted() || test_taskA.isErrored())
            || !(test_taskB.isDone() || test_taskB.isInterrupted() || test_taskB.isErrored())
            || !(test_taskC.isDone() || test_taskC.isInterrupted() || test_taskC.isErrored())) {
         Thread.sleep(500);
      }

      TestTaskInfo infoB = listener.getTaskInfo(test_taskB.getID());

      Assert.assertNotNull("Listener didn't report any status for TestTask_B.", infoB.getStatus());

      if (test_taskC.isErrored() && !test_taskD.isNotRunning()) {
         Assert.fail("TestTask_D ran even when TestTask_C before it failed.");
      }
   }

   @Test
   public void testStatusUpdating() throws Exception {
      long interval_time = 1234;

      TestTask test_task = new TestTask("TestTask_0", 10000l);
      test_task.setStatusCheckInterval(interval_time); // check every 1234 ms.
      test_task.setStatusTrackingOn(true);
      test_task.start();

      // Have to wait at least 20ms. before testing if status tracking started...
      Thread.sleep(20);

      Assert.assertTrue("Status tracking failed to start.", test_task.statusTrackingStarted());

      long sleep_time = 0;
      while (!(test_task.isDone() || test_task.isInterrupted() || test_task.isErrored())) {
         Thread.sleep(500);
         sleep_time += 500;

         if (sleep_time > 5000) {
            long update_interval_time = test_task.getTimeSinceLastStatusUpdate();

            String message = "Update interval time (" + update_interval_time + ") was not within the range of " + (interval_time - 20) + " to "
                  + (interval_time + 20) + " ms.";
            Assert.assertEquals(message, interval_time, update_interval_time, 20);
         }
      }
   }

   @Test
   public void testMultipleTasksWithLimitedProcesses() {
      // Create LimitedTestTasks
      int max_processes = 2;
      Task.setMaxActiveTaskLimit(LimitedTestTask.class.getName(), max_processes);

      TestTaskListener limited_listener = new TestTaskListener();
      ArrayList<LimitedTestTask> limited_tasks = new ArrayList<LimitedTestTask>();
      for (int i = 0; i < 10; i++) {
         long runtime = (int) Math.round(Math.random() * 10000);
         while (runtime < 4000 || runtime > 8500) {
            runtime = (int) Math.round(Math.random() * 10000);
         }

         LimitedTestTask test_task = new LimitedTestTask("LimitedTestTask_" + i, runtime);
         test_task.addTaskListener(limited_listener);
         limited_tasks.add(test_task);
      }

      // Create non-limited TestTasks
      List<TestTask> test_tasks = JobTestUtils.createTestTasks(15, null);

      // Randomly start each task from the two task lists.
      boolean done = false;
      int num_limited = limited_tasks.size();
      int num_test = test_tasks.size();
      while (!done) {
         long choice = (int) Math.round(Math.random() * 1);
         if (choice == 0 && num_limited > 0) { // Add limited task...
            Task atask = limited_tasks.get(num_limited - 1);
            atask.start();
            num_limited--;
         }
         else if (choice == 1 && num_test > 0) { // Add regular test task...
            Task atask = test_tasks.get(num_test - 1);
            atask.start();
            num_test--;
         }

         if (num_limited == 0 && num_test == 0) {
            done = true;
         }
      }

      // Wait 2.5 secs to let stuff get started before checking stuff...
      try {
         Thread.sleep(2500);
      }
      catch (InterruptedException ex) {}

      // Make sure that all the TestTasks just created are running...
      int num_running = 0;
      for (TestTask test_task : test_tasks) {
         if (test_task.isRunning()) {
            num_running++;
         }
      }

      // Fail if not all are running... sometimes this will fail because no time was
      // given after Task start up, before we start checking their status...
      if (num_running != test_tasks.size()) {
         Assert.fail(System.nanoTime() + ": Not all " + test_tasks.size() + " TestTasks are running [" + num_running + "].");
      }

      // Now wait till all the LimitedTest Tasks are done, check every so often to make sure
      // that the number of "active" LimitedTest Tasks doesn't exceed the maxium allowed.
      boolean all_done = false;
      while (!all_done) {
         // Wait half a second....
         try {
            Thread.sleep(500);
         }
         catch (Exception ex) {}

         // Check to see how many are running...
         num_running = 0;
         int num_done = 0;
         for (LimitedTestTask test_task : limited_tasks) {
            if (test_task.isRunning()) {
               num_running++;
            }
            else if (!test_task.isWaiting()) {
               num_done++;
            }
         }

         // Fail if the number currently running is more than the number allowed to run.
         if (num_running > max_processes) {
            Assert.fail(System.nanoTime() + ": More than " + max_processes + " LimitedTestTasks are running[" + num_running + "].");
         }

         // End the while loop if everyone has stopped running, note that here
         // "running" means either running or waiting to run.
         if (num_done == limited_tasks.size()) {
            all_done = true;
         }
      }

      // This is for debugging this test only...
      // limited_listener.outputAll();

      // // If logging... this will wait a bit extra to write everything out to a log file.
      // try {
      // Thread.sleep(5000);
      // }
      // catch(InterruptedException ex) {}
   }

   //This could be/was used to run a test a whole lot'a times to make sure it passed every single time.
   @Test
   @Ignore
   public void runStressTest() {
      // Avg RT: ~41 secs. 06/27/2013
      // long time = System.currentTimeMillis();
      for (int x = 0; x < 1250; x++) {
         try {
            testMultipleTasksWithLimitedProcesses();
            System.out.println("***** [" + x + "]: Completed Successfully.");
         }
         catch (Exception ex) {
            System.out.println("***** [" + x + "]: " + ex.getMessage());
         }
      }
      //    System.out.println("RT: " + (System.currentTimeMillis() - time));
   }

   private static final class TestTaskInfo {

      private double AmountCompleted;
      private String TaskID;
      private String ResultValue;
      private String Status;
      private long EstimatedTime;
      private boolean Interrupted;
      private boolean Errored;
      private boolean Stopped;
      private boolean Done;

      public TestTaskInfo() {}

      public String getTaskID() {
         return TaskID;
      }

      void setTaskID(String taskId) {
         TaskID = taskId;
      }

      public String getResultValue() {
         return ResultValue;
      }

      void setResultValue(String resultValue) {
         ResultValue = resultValue;
      }

      public String getStatus() {
         return Status;
      }

      void setStatus(String status) {
         Status = status;
      }

      public boolean isErrored() {
         return Errored;
      }

      void setErrored(boolean errored) {
         Errored = errored;
      }

      public boolean isStopped() {
         return Stopped;
      }

      void setStopped(boolean stopped) {
         Stopped = stopped;
      }

      public boolean isInterrupted() {
         return Interrupted;
      }

      void setInterrupted(boolean interrupted) {
         Interrupted = interrupted;
      }

      public boolean isDone() {
         return Done;
      }

      void setDone(boolean done) {
         Done = done;
      }

      public double getAmountCompleted() {
         return AmountCompleted;
      }

      void setAmountCompleted(double amountCompleted) {
         AmountCompleted = amountCompleted;
      }

      void setEstimatedTime(long estimatedTime) {
         EstimatedTime = estimatedTime;
      }
   }

   private static class TestTaskListener implements TaskListener {

      final Map<String, TestTaskInfo> Info = new HashMap<String, TestTaskInfo>();

      public TestTaskListener() {}

      public TestTaskInfo getTaskInfo(String task_id) {
         TestTaskInfo task_info = new TestTaskInfo();
         if (Info.containsKey(task_id)) {
            task_info = Info.get(task_id);
         }
         else {
            task_info.setTaskID(task_id);
            Info.put(task_id, task_info);
         }
         return task_info;
      }

      public boolean allTasksDone() {
         for (TestTaskInfo info : Info.values()) {
            if (!(info.isErrored() || info.isInterrupted() || info.isStopped() || info.isDone())) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void taskDone(Task task) {
         String task_id = task.getID();
         TestTaskInfo task_info = getTaskInfo(task_id);
         task_info.setDone(true);
         task_info.setResultValue((String) task.getResult());
      }

      @Override
      public void updateAmountCompleted(String task_id, double amount_completed) {
         TestTaskInfo task_info = getTaskInfo(task_id);
         task_info.setAmountCompleted(amount_completed);
      }

      @Override
      public void updateStatusMessage(String task_id, String message) {
         TestTaskInfo task_info = getTaskInfo(task_id);
         task_info.setStatus(message);
      }

      @Override
      public void updateEstimatedRuntime(String task_id, long time) {
         TestTaskInfo task_info = getTaskInfo(task_id);
         task_info.setEstimatedTime(time);
      }

      @Override
      public void taskInterrupted(Task task) {
         String task_id = task.getID();
         TestTaskInfo task_info = getTaskInfo(task_id);
         task_info.setInterrupted(true);
         task_info.setErrored(task.isErrored());
         task_info.setStopped(task.isInterrupted());
      }
   }
}
