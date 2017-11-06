package com.jc.command.task;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ThreadMonitorTester {

   @Test
   public void testThreadMonitor() throws Exception {
      int num_tasks = 7;

      List<TestTask> test_tasks = new ArrayList<TestTask>();

      for (int i = 0; i < num_tasks; i++) {
         long runtime;
         do {
            runtime = (int) Math.round(Math.random() * 900000);
         } while (runtime < 180000 || runtime > 240000);

         TestTask task = new TestTask("Task" + i, runtime);
         test_tasks.add(task);
         System.out.println(task.getID() + " est runtime: " + task.getEstimatedProcessingTime());
      }

      TestTask task1 = test_tasks.get(1);
      task1.setTestTask(task1);
      // task1.start();

      TestTask task3 = test_tasks.get(3);
      task3.setTestTask(task1);

      TestTask task4 = test_tasks.get(4);
      task4.setTestTask(task1);

      TestTask task6 = test_tasks.get(6);
      task6.setTestTask(task1);

      // long time = System.currentTimeMillis() + (60000 * 5);
      // System.out.println("Will sleep till: " + CPSStatics.getFormattedDate(time, CPSStatics.DATE_FORMAT_YYYYMMDDHHMMSS));
      // Thread.sleep(60000 * 5); // Wait five minutes...

      for (TestTask task : test_tasks) {
         task.start();
      }

      boolean all_done = false;
      while (!all_done) {
         int numNowRunning = 0;
         for (int i = 0; i < test_tasks.size(); i++) {
            Task test_task = test_tasks.get(i);
            if (test_task.isRunning()) {
               numNowRunning++;
            }
         }

         if (numNowRunning == 0) {
            all_done = true;
         }
      }

      for (Task task : test_tasks) {
         System.out.println(task.getID() + " status: " + task.getState());
      }

      System.out.println("**** Done with runJobSuccessfully test.");

      while (true) {}
   }
}
