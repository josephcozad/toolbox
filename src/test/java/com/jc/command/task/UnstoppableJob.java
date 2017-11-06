package com.jc.command.task;

/*
 *  This class is a special test case where the job can't be stopped by the JobQueue.
 */

public class UnstoppableJob extends Job {

   public UnstoppableJob(Task task, boolean multi_threaded) {
      super(task, multi_threaded);
   }

   @Override
   public void stop() {

   }

   public void forceStop() {
      super.stop();
   }
}
