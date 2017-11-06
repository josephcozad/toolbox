package com.jc.command.task;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.jc.util.FileSystem;

public class TaskRunInfo {

   private final AtomicInteger NumActiveTasks = new AtomicInteger(0);
   private final AtomicInteger MaxActiveTaskLimit = new AtomicInteger(-1);
   private final AtomicBoolean Exclusive = new AtomicBoolean(false); // These settings are exclusive to the Task class this object is associated with or applies to all sub-classes.
   private final String ClassName; // For debugging purposes, the class name associated with this object.
   private final Vector<Task> ActiveThreads = new Vector<Task>();

   AtomicBoolean TaskWaiting = new AtomicBoolean(false);

   TaskRunInfo(String classname) {
      ClassName = classname;
   }

   boolean maxActiveTasksReached() {
      return (NumActiveTasks.get() >= MaxActiveTaskLimit.get());
   }

   void setTaskWaiting() {
      TaskWaiting.compareAndSet(false, true);
   }

   String getTaskClassName() {
      return (ClassName);
   }

   void setExclusive(boolean exclusive) {
      Exclusive.set(exclusive);
   }

   boolean isExclusive() {
      return (Exclusive.get());
   }

   void setMaxActiveTaskLimit(int max_num) {
      MaxActiveTaskLimit.set(max_num);
   }

   int getMaxActiveTaskLimit() {
      return (MaxActiveTaskLimit.get());
   }

   void deactivateThread(Task atask) {
      ActiveThreads.remove(atask);
      NumActiveTasks.decrementAndGet(); // decrement task count
   }

   void activateThread(Task atask) {
      NumActiveTasks.incrementAndGet();
      ActiveThreads.add(atask);
      TaskWaiting.compareAndSet(true, false);
   }

   String outputActiveThreads() {
      StringBuffer sb = new StringBuffer("Active Threads at " + System.nanoTime() + " ns." + FileSystem.NEWLINE);
      if (ActiveThreads.size() > 0) {
         for (Task task_obj : ActiveThreads) {
            sb.append("   ID[" + task_obj.getID() + "] STATE[" + task_obj.getState() + "]" + FileSystem.NEWLINE);
         }
      }
      else {
         sb.append("   NONE" + FileSystem.NEWLINE);
      }
      return (sb.toString());
   }

   @Override
   public String toString() {
      return ("Waiting[" + TaskWaiting + "] Active[" + NumActiveTasks + "] MaxTasks[" + MaxActiveTaskLimit + "] Exclusive[" + Exclusive + "] TaskClass["
            + ClassName + "]");
   }
}
