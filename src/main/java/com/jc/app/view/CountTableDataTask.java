package com.jc.app.view;

import com.jc.command.task.Task;
import com.jc.db.dao.FilterInfo;

public class CountTableDataTask extends Task {

   private static final long serialVersionUID = -7517411396596175404L;

   private final static String TASKPREFIX = "CountDataTask";

   private final AppLazyDataModel<?> dataModel;
   private final FilterInfo filterOn;

   private double amountCompleted;
   private String errorMessage;

   public CountTableDataTask(AppLazyDataModel<?> dataModel, FilterInfo filterOn) {

      this.dataModel = dataModel;
      this.filterOn = filterOn;

      amountCompleted = 0;

      setIDPrefix(TASKPREFIX);
   }

   @Override
   public void doTask() throws Exception {
      updateStatusMessage("Starting task(" + getID() + ")."); // For job logging...

      amountCompleted = 0.5d;

      long totalRows = dataModel.getDataCount(filterOn);
      setTaskResult(new Long(totalRows));

      amountCompleted = 1.0d;
      updateStatusMessage("Completed task."); // For job logging...
   }

   @Override
   protected void updateStatus() {}

   @Override
   protected void updateStatusMessage(String message) {

      // For job logging...
      if (message.contains(INTERRUPTED_TASK_STATUS_MSG) && amountCompleted > 0) {
         String pattern = INTERRUPTED_TASK_STATUS_MSG + getID() + " - ";
         message = "Task stopped: " + message.replace(pattern, "");
      }
      else if (message.contains(ERRORED_TASK_STATUS_MSG)) {
         String pattern = ERRORED_TASK_STATUS_MSG + getID() + " - ";
         errorMessage = message.replace(pattern, "");
         message = "Task errored: " + errorMessage;
      }

      super.updateStatusMessage(message);
   }

   String getErrorMessage() {
      return errorMessage;
   }
}
