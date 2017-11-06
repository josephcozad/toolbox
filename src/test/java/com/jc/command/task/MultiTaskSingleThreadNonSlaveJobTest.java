package com.jc.command.task;

import org.junit.Test;

public class MultiTaskSingleThreadNonSlaveJobTest {

   private static boolean MULTI_THREADED = false;
   private static boolean SLAVE_MODE = false;
   private static boolean MULTI_TASK = true;

   @Test
   public void runJobSuccessfully() throws Exception {
      JobTestUtils.runJobSuccessfully(MULTI_THREADED, SLAVE_MODE, MULTI_TASK);
   }

   @Test
   public void runJobWithException() throws Exception {
      JobTestUtils.runJobWithException(MULTI_THREADED, SLAVE_MODE, MULTI_TASK);
   }

   @Test
   public void runJobWithStop() throws Exception {
      JobTestUtils.runJobWithStop(MULTI_THREADED, SLAVE_MODE, MULTI_TASK);
   }
}
