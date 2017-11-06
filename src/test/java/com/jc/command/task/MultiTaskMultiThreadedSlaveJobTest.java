package com.jc.command.task;

import org.junit.Test;

public class MultiTaskMultiThreadedSlaveJobTest {

   private static boolean MULTI_THREADED = true;
   private static boolean MULTI_TASK = true;

   @Test
   public void runSlaveJobSuccessfully() throws Exception {
      JobTestUtils.runSlaveJobSuccessfully(MULTI_THREADED, MULTI_TASK); // All masters must finish -- and they do.
   }

   @Test
   public void runSlaveJobWithMasterExceptionFullFinish() throws Exception {
      JobTestUtils.runSlaveJobWithMasterExceptionA(MULTI_THREADED, MULTI_TASK); // All masters must finish -- and they don't.
   }

   @Test
   public void runSlaveJobWithMasterExceptionPartialFinish() throws Exception {
      JobTestUtils.runSlaveJobWithMasterExceptionB(MULTI_THREADED, MULTI_TASK); // Not all the masters have to finish.
   }
}
