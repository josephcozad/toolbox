package com.jc.command.task;

public class JobTaskInfo {

   private final Task MyTask;
   private double AmountCompleted;
   private double Weight;
   private long EstimatedRuntime;

   public JobTaskInfo(Task atask) {
      MyTask = atask;
      EstimatedRuntime = -1;
   }

   public long getEstimatedRuntime() {
      if (EstimatedRuntime == -1) {
         EstimatedRuntime = MyTask.getEstimatedProcessingTime(); // Default runtime....
      }
      return (EstimatedRuntime);
   }

   void setEstimatedRuntime(long time) {
      EstimatedRuntime = time; // Default runtime....
   }

   public double getAmountCompleted() {
      return (AmountCompleted);
   }

   void setAmountCompleted(double amount) {
      AmountCompleted = amount;
   }

   public Task getTaskObject() {
      return (MyTask);
   }

   double getWeight() {
      return (Weight);
   }

   public void setWeight(double weight) {
      Weight = weight;
   }

   public void calculateWeight(long est_job_runtime) {
      if (est_job_runtime == 0) {
         Weight = 0;
      }
      else if (EstimatedRuntime == 0) {
         Weight = 1; // Assume a weight of 1...
      }
      else {
         Weight = (double) EstimatedRuntime / (double) est_job_runtime;
      }
   }
}
